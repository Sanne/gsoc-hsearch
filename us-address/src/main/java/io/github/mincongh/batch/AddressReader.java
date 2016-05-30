package io.github.mincongh.batch;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.batch.api.chunk.ItemReader;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.impl.HibernateSessionLoadingInitializer;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.spi.InstanceInitializer;

import io.github.mincongh.entity.Address;

/**
 * The item reader, which reads entities from Hibernate session and more
 * TODO: add more explicit explanations
 * 
 * @author Mincong HUANG
 */
@Named
@Singleton
public class AddressReader implements ItemReader {

    // Entity manager is used for fetching entities from persistence context.
    @PersistenceContext(unitName = "us-address")
    private EntityManager em;
    
    // Cache for add-lucene-work
    // TODO: LinkedList because its insertion is O(1) ?
    // and no need to resize and copy the current list comparing to array list
    List<AddLuceneWork> addWorks = new LinkedList<>();
    Iterator<AddLuceneWork> iterator;
    
    // Current session
    private Session session;
    
    private final int MAX_RESULTS = 1000;
    
    // Interface which gives access to runtime configuration
    // Intended to be used by Search components
    private ExtendedSearchIntegrator searchIntegrator;
    
    // Entity index binding specifies the relation and options from an indexed
    // entity to its index(es).
    private EntityIndexBinding entityIndexBinding;
    
    // The document builder for indexed entity (Address)
    private DocumentBuilderIndexedEntity docBuilder;
    
    // TODO: what is a session initializer ?
    private InstanceInitializer sessionInitializer;
    
    // TODO: add description
    private ConversionContext conversionContext;
    
    @Override
    public Serializable checkpointInfo() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

    /**
     * When open() is called, the address entities will be read from
     * persistence context. Then they will be stored temporarily in this class
     * as a list. Then different classes, required for the Lucene work, will be 
     * initialized here. The checkpoint is not used in this demo.
     * TODO: maybe we should do it in a better way
     * 
     * @param checkpoint the checkpoint given by the batch runtime. It is used
     *         when the job execution is restarted.
     * @throws Exception when any exception is thrown during the open process
     */
    @Override
    @SuppressWarnings("unchecked")
    public void open(Serializable checkpoint) throws Exception {

        // Load entities from database via entity manager
        List<Address> addresses = em
                .createQuery("SELECT a FROM Address a WHERE address_id <= 1000")
                .setMaxResults(MAX_RESULTS)
                .getResultList();
        
        // Get Hibernate session from JPA entity manager
        session = em.unwrap(Session.class);
        
        // TODO: construct SessionImplementor before getting searchIntegrator
        // but how should I use it ? This class belongs to org.hibernate.engine.
        // ...
        // get extendedSearchIntegrator via ContextHelper using hibernate
        // session
        searchIntegrator = ContextHelper.getSearchintegrator(session);
        
        // In the previous implementation (currently used in HSERACH), the class
        // IdentifierConsumerDocumentProducer is used to get the index bindings
        // but this time, reader interacts directly with searchIntegrator to
        // these bindings. Once bindings assigned, we need to find out the
        // matched binding for the target class:
        //
        //     io.github.mincongh.entity.Address
        // 
        entityIndexBinding = searchIntegrator
                .getIndexBindings()
                .get(Address.class);
        
        // TODO: There should be optimizations here, but they're omitted at the
        // first time for simply the process
        // ...
        // Get the document builder
        docBuilder = entityIndexBinding.getDocumentBuilder();
        
        // initialize conversion context
        conversionContext = new ContextualExceptionBridgeHelper();
        
        // initialize session initializer
        sessionInitializer = new HibernateSessionLoadingInitializer(
                (SessionImplementor) session
        );
        
        // Builds the Lucene {@code Document} for a given entity.
        
        // Tenant-aware
        // Hibernate Search supports multi-tenancy on top of Hibernate ORM, 
        // it stores the tenant identifier in the document and automatically 
        // filters the query results. The FullTextSession will be bound to 
        // the specific tenant ("client-A" in the example) and the mass indexer 
        // will only index the entities associated to that tenant identifier.
        // TODO: can it be null for a single-tenancy db ?
        String tenantId = null;
        
        for (Address address: addresses) {
            Serializable id = session.getIdentifier(address);
            TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
            conversionContext.pushProperty(docBuilder.getIdKeywordName());
            String idInString = null;
            try {
                idInString = conversionContext
                        .setClass(Address.class)
                        .twoWayConversionContext(idBridge)
                        .objectToString(id);
            } finally {
                conversionContext.popProperty();
            }
            AddLuceneWork addWork = docBuilder.createAddWork(
                    tenantId,
                    Address.class,
                    address,
                    id,
                    idInString,
                    sessionInitializer,
                    conversionContext
            );
            addWorks.add(addWork);
        }
        iterator = addWorks.iterator();
    }

    /**
     * Read items from the item list until the last one is reached.
     */
    @Override
    public Object readItem() throws Exception {
        return iterator.hasNext() ? iterator.next() : null;
    }
    
    /**
     * Getter for EntityIndexBinding
     */
    // TODO: should have better way to do this
    public EntityIndexBinding getEntityIndexBinding() {
        return entityIndexBinding;
    }
}