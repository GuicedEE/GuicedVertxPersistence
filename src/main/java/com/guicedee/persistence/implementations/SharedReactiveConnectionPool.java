package com.guicedee.persistence.implementations;

import io.vertx.sqlclient.Pool;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.reactive.pool.impl.ExternalSqlClientPool;
import org.hibernate.reactive.pool.impl.SqlClientPool;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Borrows the exact managed pool; PersistenceShutdown retains responsibility for closing it. */
public final class SharedReactiveConnectionPool extends SqlClientPool implements Configurable, ServiceRegistryAwareService {
    private Pool pool;
    private SqlStatementLogger logger;
    private final SqlExceptionHelper exceptions=new SqlExceptionHelper(false);

    @Override public void configure(Map<String,Object> settings) {
        if(!(settings.get("hibernate.vertx.pool") instanceof Pool configured))
            throw new IllegalArgumentException("A shared Vertx SQL pool is required");
        pool=configured;
    }
    @Override public void injectServices(ServiceRegistryImplementor registry) {
        logger=registry.getService(SqlStatementLogger.class);
    }
    @Override protected Pool getPool() {return pool;}
    @Override protected SqlStatementLogger getSqlStatementLogger() {return logger;}
    @Override protected SqlExceptionHelper getSqlExceptionHelper() {return exceptions;}
    @Override public CompletionStage<Void> getCloseFuture() {return CompletableFuture.completedFuture(null);}
    @Override public CompletionStage<ResultSet> selectJdbcOutsideTransaction(String sql,Object[] parameters) {
        return new ExternalSqlClientPool(pool,logger,exceptions).selectJdbcOutsideTransaction(sql,parameters);
    }
}
