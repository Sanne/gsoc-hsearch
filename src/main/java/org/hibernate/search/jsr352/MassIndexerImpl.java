package org.hibernate.search.jsr352;

import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.hibernate.search.jsr352.internal.IndexingContext;

public class MassIndexerImpl implements MassIndexer {

    private boolean optimizeAfterPurge = false;
    private boolean optimizeAtEnd = false;
    private boolean purgeAtStart = false;
    private int arrayCapacity = 1000;
    private int fetchSize = 200000;
    private int maxResults = 1000000;
    private int partitionCapacity = 250;
    private int partitions = 4;
    private int threads = 2;
    private Set<Class<?>> rootEntities;
    
    private final String JOB_NAME = "mass-index";
    
    MassIndexerImpl() {
        
    }
    
    @Override
    public long start() {
        
        registrerRootEntities(rootEntities);
        
        Properties jobParams = new Properties();
        jobParams.setProperty("fetchSize", String.valueOf(fetchSize));
        jobParams.setProperty("arrayCapacity", String.valueOf(arrayCapacity));
        jobParams.setProperty("maxResults", String.valueOf(maxResults));
        jobParams.setProperty("partitionCapacity", String.valueOf(partitionCapacity));
        jobParams.setProperty("partitions", String.valueOf(partitions));
        jobParams.setProperty("threads", String.valueOf(threads));
        jobParams.setProperty("purgeAtStart", String.valueOf(purgeAtStart));
        jobParams.setProperty("optimizeAfterPurge", String.valueOf(optimizeAfterPurge));
        jobParams.setProperty("optimizeAtEnd", String.valueOf(optimizeAtEnd));
        jobParams.setProperty("rootEntities", String.valueOf(rootEntities));
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Long executionId = jobOperator.start(JOB_NAME, jobParams);
        return executionId;
    }

    @Override
    public void stop(long executionId) {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        jobOperator.stop(executionId);
    }

    @Override
    public MassIndexer arrayCapacity(int arrayCapacity) {
        if (arrayCapacity < 1) {
            throw new IllegalArgumentException("arrayCapacity must be at least 1");
        }
        this.arrayCapacity = arrayCapacity;
        return this;
    }

    @Override
    public MassIndexer fetchSize(int fetchSize) {
        if (fetchSize < 1) {
            throw new IllegalArgumentException("fetchSize must be at least 1");
        }
        this.fetchSize = fetchSize;
        return this;
    }

    @Override
    public MassIndexer maxResults(int maxResults) {
        if (maxResults < 1) {
            throw new IllegalArgumentException("maxResults must be at least 1");
        }
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge) {
        this.optimizeAfterPurge = optimizeAfterPurge;
        return this;
    }

    @Override
    public MassIndexer optimizeAtEnd(boolean optimizeAtEnd) {
        this.optimizeAtEnd = optimizeAtEnd;
        return this;
    }

    @Override
    public MassIndexer partitionCapacity(int partitionCapacity) {
        if (partitionCapacity < 1) {
            throw new IllegalArgumentException("partitionCapacity must be at least 1");
        }
        this.partitionCapacity = partitionCapacity;
        return this;
    }

    @Override
    public MassIndexer partitions(int partitions) {
        if (partitions < 1) {
            throw new IllegalArgumentException("partitions must be at least 1");
        }
        this.partitions = partitions;
        return this;
    }

    @Override
    public MassIndexer purgeAtStart(boolean purgeAtStart) {
        this.purgeAtStart = purgeAtStart;
        return this;
    }

    @Override
    public MassIndexer threads(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1.");
        }
        this.threads = threads;
        return this;
    }

    @Override
    public MassIndexer rootEntities(Set<Class<?>> rootEntities) {
        if (rootEntities == null) {
            throw new NullPointerException("rootEntities cannot be NULL.");
        } else if (rootEntities.isEmpty()) {
            throw new NullPointerException("rootEntities must have at least 1 element.");
        }
        this.rootEntities = rootEntities;
        return this;
    }

    @Override
    public boolean isOptimizeAfterPurge() {
        return optimizeAfterPurge;
    }

    public boolean isOptimizeAtEnd() {
        return optimizeAtEnd;
    }

    public boolean isPurgeAtStart() {
        return purgeAtStart;
    }

    public int getArrayCapacity() {
        return arrayCapacity;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public int getPartitionCapacity() {
        return partitionCapacity;
    }

    public int getPartitions() {
        return partitions;
    }

    public int getThreads() {
        return threads;
    }

    public String getJOB_NAME() {
        return JOB_NAME;
    }

    public Set<Class<?>> getRootEntities() {
        return rootEntities;
    }
    
    public void registrerRootEntities(Set<Class<?>> rootEntities) {
        if (rootEntities == null) {
            throw new NullPointerException("rootEntities cannot be NULL.");
        } else if (rootEntities.isEmpty()) {
            throw new NullPointerException("rootEntities must have at least 1 element.");
        }
        int s = rootEntities.size();
        
        BeanManager bm = CDI.current().getBeanManager();
        Bean<IndexingContext> bean = (Bean<IndexingContext>) bm
                .resolve(bm.getBeans(IndexingContext.class));
        IndexingContext indexingContext = bm
                .getContext(bean.getScope())
                .get(bean, bm.createCreationalContext(bean));
        Class<?>[] r = rootEntities.toArray(new Class<?>[s]);
        indexingContext.setRootEntities(r);
    }
}