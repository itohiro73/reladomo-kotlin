package io.github.reladomokotlin.springboot.sequence

import com.gs.fw.common.mithra.MithraSequence
import com.gs.fw.common.mithra.MithraSequenceObjectFactory
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * Generic sequence object factory for Reladomo's SimulatedSequence strategy.
 * This factory creates and manages sequence records in the MITHRA_SEQUENCE table.
 * 
 * This implementation can be used for any entity that uses SimulatedSequence,
 * avoiding the need to create individual factory classes for each entity.
 * 
 * Note: This implementation uses direct JDBC to avoid circular dependencies,
 * as MithraSequence itself would need to be a Reladomo object.
 */
class GenericSequenceObjectFactory : MithraSequenceObjectFactory {
    
    companion object {
        private val logger = LoggerFactory.getLogger(GenericSequenceObjectFactory::class.java)
        
        @Volatile
        private var dataSource: DataSource? = null
        
        /**
         * Static instance method required by Reladomo when referenced in XML configuration.
         * This allows the factory to be referenced as:
         * sequenceObjectFactoryName="io.github.reladomokotlin.springboot.sequence.GenericSequenceObjectFactory"
         */
        @JvmStatic
        fun getInstance(): GenericSequenceObjectFactory {
            return GenericSequenceObjectFactory()
        }
        
        /**
         * Set the DataSource to be used for sequence operations.
         * This should be called during Spring Boot initialization.
         */
        @JvmStatic
        fun setDataSource(ds: DataSource) {
            dataSource = ds
        }
    }
    
    /**
     * Implementation of MithraSequenceObjectFactory interface.
     * The interface expects: getMithraSequenceObject(String sequenceName, Object sourceAttribute, int initialValue)
     */
    override fun getMithraSequenceObject(
        sequenceName: String,
        sourceAttribute: Any?,
        initialValue: Int
    ): MithraSequence {
        logger.debug("Getting sequence object for: $sequenceName with initial value: $initialValue")
        
        val ds = dataSource ?: throw IllegalStateException("DataSource not initialized in GenericSequenceObjectFactory")
        
        return JdbcMithraSequence(ds, sequenceName, initialValue.toLong())
    }
    
    /**
     * JDBC-based implementation of MithraSequence interface.
     * Uses direct SQL to manage sequences without requiring Reladomo objects.
     */
    private class JdbcMithraSequence(
        private val dataSource: DataSource,
        private val sequenceName: String,
        private val initialValue: Long
    ) : MithraSequence {
        
        private var nextValue: Long? = null
        
        init {
            // Initialize the sequence if it doesn't exist
            initializeSequence()
        }
        
        private fun initializeSequence() {
            dataSource.connection.use { conn ->
                // Check if sequence exists
                val checkSql = "SELECT NEXT_ID FROM MITHRA_SEQUENCE WHERE SEQUENCE_NAME = ?"
                conn.prepareStatement(checkSql).use { ps ->
                    ps.setString(1, sequenceName)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            nextValue = rs.getLong("NEXT_ID")
                        } else {
                            // Create new sequence
                            val insertSql = "INSERT INTO MITHRA_SEQUENCE (SEQUENCE_NAME, NEXT_ID) VALUES (?, ?)"
                            conn.prepareStatement(insertSql).use { insertPs ->
                                insertPs.setString(1, sequenceName)
                                insertPs.setLong(2, initialValue)
                                insertPs.executeUpdate()
                                nextValue = initialValue
                            }
                        }
                    }
                }
            }
        }
        
        override fun setSequenceName(name: String) {
            // This is set in constructor, no-op here
        }
        
        override fun getNextId(): Long {
            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    // Get and increment in a single transaction
                    val selectSql = "SELECT NEXT_ID FROM MITHRA_SEQUENCE WHERE SEQUENCE_NAME = ? FOR UPDATE"
                    val currentValue: Long
                    
                    conn.prepareStatement(selectSql).use { ps ->
                        ps.setString(1, sequenceName)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) {
                                throw IllegalStateException("Sequence $sequenceName not found")
                            }
                            currentValue = rs.getLong("NEXT_ID")
                        }
                    }
                    
                    // Update to next value
                    val updateSql = "UPDATE MITHRA_SEQUENCE SET NEXT_ID = ? WHERE SEQUENCE_NAME = ?"
                    conn.prepareStatement(updateSql).use { ps ->
                        ps.setLong(1, currentValue + 1)
                        ps.setString(2, sequenceName)
                        ps.executeUpdate()
                    }
                    
                    conn.commit()
                    return currentValue
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            }
        }
        
        override fun setNextId(nextId: Long) {
            dataSource.connection.use { conn ->
                val updateSql = "UPDATE MITHRA_SEQUENCE SET NEXT_ID = ? WHERE SEQUENCE_NAME = ?"
                conn.prepareStatement(updateSql).use { ps ->
                    ps.setLong(1, nextId)
                    ps.setString(2, sequenceName)
                    ps.executeUpdate()
                }
                nextValue = nextId
            }
        }
    }
}