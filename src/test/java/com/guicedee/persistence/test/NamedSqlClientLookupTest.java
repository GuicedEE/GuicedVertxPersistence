package com.guicedee.persistence.test;

import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.bind.JtaPersistModule;
import com.guicedee.persistence.implementations.VertxPersistenceModule;
import io.vertx.sqlclient.SqlClient;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NamedSqlClientLookupTest {
    @AfterEach void reset() { VertxPersistenceModule.reset(); }

    @Test void borrowsDirectlyRegisteredModulePoolOnceWithoutClosingIt() {
        var connection=mock(ConnectionBaseInfo.class);var pool=mock(SqlClient.class);
        when(connection.getPersistenceUnitName()).thenReturn("named-fixture");
        when(connection.toPooledDatasource()).thenReturn(pool);
        VertxPersistenceModule.getConnectionModules().put(connection,mock(JtaPersistModule.class));
        assertSame(pool,VertxPersistenceModule.getSqlClientByEntityManager("named-fixture"));
        assertSame(pool,VertxPersistenceModule.getSqlClientByEntityManager("named-fixture"));
        verify(connection,times(1)).toPooledDatasource();
        verifyNoInteractions(pool);
        assertNull(VertxPersistenceModule.getSqlClientByEntityManager("unknown-unit"));
    }

    @Test void missingDatasourceIsNotCachedAcrossInitialization() {
        var connection=mock(ConnectionBaseInfo.class);var pool=mock(SqlClient.class);
        when(connection.getPersistenceUnitName()).thenReturn("late-fixture");
        when(connection.toPooledDatasource()).thenReturn(null,pool);
        VertxPersistenceModule.getConnectionModules().put(connection,mock(JtaPersistModule.class));
        assertNull(VertxPersistenceModule.getSqlClientByEntityManager("late-fixture"));
        assertSame(pool,VertxPersistenceModule.getSqlClientByEntityManager("late-fixture"));
        assertSame(pool,VertxPersistenceModule.getSqlClientByEntityManager("late-fixture"));
        verify(connection,times(2)).toPooledDatasource();verifyNoInteractions(pool);
    }
}
