/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.test.entity.Company;
import org.hibernate.search.jsr352.test.entity.CompanyManager;
import org.hibernate.search.jsr352.test.entity.Person;
import org.hibernate.search.jsr352.test.entity.PersonManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This integration test (IT) aims to test the performance of the existing mass-indexer and the new mass-indexer under
 * JSR352.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class PerformanceIT {

	private static final Logger LOGGER = Logger.getLogger( PerformanceIT.class );
	private static final int JOB_FETCH_SIZE = 100 * 1000;
	private static final int JOB_MAX_THREADS = 10;
	private static final int JOB_ROWS_PER_PARTITION = 20 * 1000;
	private static final int JOB_ITEM_COUNT = 500;
	private static final int DB_COMP_ROWS = 10 * 1000;
	private static final int DB_PERS_ROWS = 10 * 1000;

	@PersistenceContext(unitName = "h2")
	private EntityManager em;

	@Inject
	private CompanyManager companyManager;

	@Inject
	private PersonManager personManager;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap.create( WebArchive.class, PerformanceIT.class.getSimpleName() + ".war" )
				.addAsResource( "META-INF/persistence.xml" )
				.addAsResource( "META-INF/batch-jobs/make-deployment-as-batch-app.xml" ) // WFLY-7000
				.addAsWebInfResource( "jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( Company.class.getPackage() );
		return war;
	}

	@Before
	public void insertData() {
		final String[][] str = new String[][]{
				{ "Google", "Sundar", "Pichai" },
				{ "Red Hat", "James", "M. Whitehurst" },
				{ "Microsoft", "Satya", "Nadella" },
				{ "Facebook", "Mark", "Zuckerberg" },
				{ "Amazon", "Jeff", "Bezos" }
		};

		List<Company> companies = new ArrayList<>( DB_COMP_ROWS );
		for ( int i = 0; i < DB_COMP_ROWS - 1; i++ ) {
			String companyName = str[i % 5][0];
			companies.add( new Company( companyName ) );
		}
		companies.add( new Company( "hibernate" ) );
		companyManager.persist( companies );

		List<Person> people = new ArrayList<>( DB_PERS_ROWS );
		for ( int i = 0; i < DB_PERS_ROWS - 1; i++ ) {
			String firstName = str[i % 5][1];
			String lastName = str[i % 5][2];
			people.add( new Person( i, firstName, lastName ) );
		}
		people.add( new Person( DB_PERS_ROWS, "Mincong", "Huang" ) );
		personManager.persist( people );
	}

	@Test
	public void testDiffrentMassIndexer() throws InterruptedException, IOException {

		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );

		long start0 = System.currentTimeMillis();
		testOldMassIndexer( ftem );
		long end0 = System.currentTimeMillis();

		ftem.purgeAll( Person.class );
		ftem.purgeAll( Company.class );
		ftem.flushToIndexes();

		long start1 = System.currentTimeMillis();
		testNewMassIndexer();
		long end1 = System.currentTimeMillis();
		double delta0 = ( end0 - start0 ) / 1000f;
		double delta1 = ( end1 - start1 ) / 1000f;
		LOGGER.infof( "%n%n"
				+ "\told massindexer = %.2fs%n"
				+ "\tnew massindexer = %.2fs%n",
				delta0, delta1 );
	}

	public void testOldMassIndexer(FullTextEntityManager ftem) throws InterruptedException {

		List<Company> hibernate = companyManager.findCompanyByName( "hibernate" );
		List<Person> mincong = personManager.findPerson( "mincong" );
		assertEquals( 0, hibernate.size() );
		assertEquals( 0, mincong.size() );

		// Start the job
		ftem.createIndexer()
				.batchSizeToLoadObjects( 500 )
				.threadsToLoadObjects( JOB_MAX_THREADS )
				.cacheMode( CacheMode.IGNORE )
				.startAndWait();

		// Assert
		hibernate = companyManager.findCompanyByName( "hibernate" );
		mincong = personManager.findPerson( "mincong" );
		assertEquals( 1, hibernate.size() );
		assertEquals( 1, mincong.size() );
	}

	public void testNewMassIndexer() throws InterruptedException, IOException {

		List<Company> hibernate = companyManager.findCompanyByName( "hibernate" );
		List<Person> mincong = personManager.findPerson( "mincong" );
		assertEquals( 0, hibernate.size() );
		assertEquals( 0, mincong.size() );

		// Start the job
		long executionId = BatchIndexingJob.forEntities( Company.class, Person.class )
				.fetchSize( JOB_FETCH_SIZE )
				.maxThreads( JOB_MAX_THREADS )
				.rowsPerPartition( JOB_ROWS_PER_PARTITION )
				.checkpointFreq( JOB_ITEM_COUNT )
				.start();
		JobExecution jobExecution = BatchRuntime.getJobOperator().getJobExecution( executionId );
		jobExecution = keepTestAlive( jobExecution );
		assertEquals( BatchStatus.COMPLETED, jobExecution.getBatchStatus() );

		// Assert
		hibernate = companyManager.findCompanyByName( "hibernate" );
		mincong = personManager.findPerson( "mincong" );
		assertEquals( 1, hibernate.size() );
		assertEquals( 1, mincong.size() );
	}

	private JobExecution keepTestAlive(JobExecution jobExecution)
			throws InterruptedException {

		int tries = 0;
		while ( ( jobExecution.getBatchStatus().equals( BatchStatus.STARTING )
				|| jobExecution.getBatchStatus().equals( BatchStatus.STARTED ) )
				&& tries < 300 ) {

			long executionId = jobExecution.getExecutionId();
			LOGGER.infof( "Job execution (id=%d) is still working (status=%s).",
					executionId,
					jobExecution.getBatchStatus(),
					1000 );
			Thread.sleep( 1000 );
			jobExecution = BatchRuntime.getJobOperator().getJobExecution( executionId );
			tries++;
		}
		return jobExecution;
	}
}
