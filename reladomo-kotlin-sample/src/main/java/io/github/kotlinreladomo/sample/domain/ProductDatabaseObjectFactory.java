package io.github.kotlinreladomo.sample.domain;

import com.gs.fw.common.mithra.MithraSequenceObjectFactory;
import com.gs.fw.common.mithra.MithraSequence;

public class ProductDatabaseObjectFactory implements MithraSequenceObjectFactory {
    
    public ProductDatabaseObjectFactory() {
    }

    @Override
    public MithraSequence getMithraSequenceObject(String sequenceName, Object sourceAttribute, int initialValue) {
        // For demo purposes, we use a simple in-memory sequence
        // In production, this would typically be backed by a database sequence
        return new InMemorySequence(sequenceName, initialValue);
    }
    
    private static class InMemorySequence implements MithraSequence {
        private String sequenceName;
        private long currentValue;
        
        public InMemorySequence(String sequenceName, int initialValue) {
            this.sequenceName = sequenceName;
            this.currentValue = initialValue;
        }
        
        @Override
        public synchronized long getNextId() {
            return currentValue++;
        }
        
        @Override
        public synchronized void setNextId(long nextValue) {
            this.currentValue = nextValue;
        }
        
        @Override
        public void setSequenceName(String sequenceName) {
            this.sequenceName = sequenceName;
        }
    }
}