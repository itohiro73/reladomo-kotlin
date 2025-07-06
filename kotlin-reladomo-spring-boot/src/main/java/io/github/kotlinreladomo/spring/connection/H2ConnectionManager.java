package io.github.kotlinreladomo.spring.connection;

import com.gs.fw.common.mithra.bulkloader.BulkLoader;
import com.gs.fw.common.mithra.bulkloader.BulkLoaderException;
import com.gs.fw.common.mithra.connectionmanager.SourcelessConnectionManager;
import com.gs.fw.common.mithra.connectionmanager.XAConnectionManager;
import com.gs.fw.common.mithra.databasetype.DatabaseType;
import com.gs.fw.common.mithra.databasetype.H2DatabaseType;

import java.sql.Connection;
import java.util.Properties;
import java.util.TimeZone;

/**
 * H2-specific connection manager for Reladomo.
 * Uses XAConnectionManager for proper connection pooling and transaction support.
 */
public class H2ConnectionManager implements SourcelessConnectionManager {
    
    private static H2ConnectionManager instance;
    private XAConnectionManager xaConnectionManager;
    
    public static synchronized H2ConnectionManager getInstance() {
        if (instance == null) {
            instance = new H2ConnectionManager();
        }
        return instance;
    }
    
    public static synchronized H2ConnectionManager getInstance(Properties properties) {
        if (instance == null) {
            instance = new H2ConnectionManager();
        }
        return instance;
    }
    
    private H2ConnectionManager() {
        this.xaConnectionManager = new XAConnectionManager();
        this.xaConnectionManager.setDriverClassName("org.h2.Driver");
        this.xaConnectionManager.setJdbcConnectionString("jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
        this.xaConnectionManager.setJdbcUser("sa");
        this.xaConnectionManager.setJdbcPassword("");
        this.xaConnectionManager.setPoolName("H2 In-Memory Pool");
        this.xaConnectionManager.setInitialSize(1);
        this.xaConnectionManager.setPoolSize(10);
        this.xaConnectionManager.setUseStatementPooling(true);
        this.xaConnectionManager.initialisePool();
    }
    
    @Override
    public Connection getConnection() {
        return xaConnectionManager.getConnection();
    }
    
    @Override
    public DatabaseType getDatabaseType() {
        return H2DatabaseType.getInstance();
    }
    
    @Override
    public TimeZone getDatabaseTimeZone() {
        return TimeZone.getTimeZone("UTC");
    }
    
    @Override
    public String getDatabaseIdentifier() {
        return "H2_MEMORY";
    }
    
    @Override
    public BulkLoader createBulkLoader() throws BulkLoaderException {
        return null;
    }
}