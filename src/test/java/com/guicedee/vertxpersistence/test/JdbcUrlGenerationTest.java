package com.guicedee.vertxpersistence.test;

import com.guicedee.vertxpersistence.implementations.postgres.PostgresConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.mysql.MySqlConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.oracle.OracleConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.sqlserver.SqlServerConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.db2.DB2ConnectionBaseInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for verifying JDBC URL generation functionality.
 * This test demonstrates that the getJdbcUrl() method in ConnectionBaseInfo
 * correctly generates JDBC URLs for different database types.
 */
public class JdbcUrlGenerationTest {

    @Test
    public void testPostgresJdbcUrlGeneration() {
        // Create a PostgreSQL connection info
        PostgresConnectionBaseInfo postgresInfo = new PostgresConnectionBaseInfo();
        postgresInfo.setServerName("localhost");
        postgresInfo.setPort("5432");
        postgresInfo.setDatabaseName("testdb");
        postgresInfo.setUsername("test");
        postgresInfo.setPassword("test");

        // Generate the JDBC URL
        String jdbcUrl = postgresInfo.getJdbcUrl();

        // Verify the URL
        assertEquals("jdbc:postgresql://localhost:5432/testdb", jdbcUrl, 
                "PostgreSQL JDBC URL should be generated correctly");
    }

    @Test
    public void testMySqlJdbcUrlGeneration() {
        // Create a MySQL connection info
        MySqlConnectionBaseInfo mysqlInfo = new MySqlConnectionBaseInfo();
        mysqlInfo.setServerName("localhost");
        mysqlInfo.setPort("3306");
        mysqlInfo.setDatabaseName("testdb");
        mysqlInfo.setUsername("test");
        mysqlInfo.setPassword("test");

        // Generate the JDBC URL
        String jdbcUrl = mysqlInfo.getJdbcUrl();

        // Verify the URL
        assertEquals("jdbc:mysql://localhost:3306/testdb", jdbcUrl, 
                "MySQL JDBC URL should be generated correctly");
    }

    @Test
    public void testOracleJdbcUrlGeneration() {
        // Create an Oracle connection info with SID
        OracleConnectionBaseInfo oracleInfo = new OracleConnectionBaseInfo();
        oracleInfo.setServerName("localhost");
        oracleInfo.setPort("1521");
        oracleInfo.setDatabaseName("orcl");
        oracleInfo.setUsername("test");
        oracleInfo.setPassword("test");

        // Generate the JDBC URL
        String jdbcUrl = oracleInfo.getJdbcUrl();

        // Verify the URL (SID format)
        assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", jdbcUrl, 
                "Oracle JDBC URL with SID should be generated correctly");

        // Create an Oracle connection info with Service Name
        OracleConnectionBaseInfo oracleServiceInfo = new OracleConnectionBaseInfo();
        oracleServiceInfo.setServerName("localhost");
        oracleServiceInfo.setPort("1521");
        oracleServiceInfo.setDatabaseName("orcl");
        oracleServiceInfo.setUsername("test");
        oracleServiceInfo.setPassword("test");
        oracleServiceInfo.getCustomProperties().put("useServiceName", "true");

        // Generate the JDBC URL
        String serviceJdbcUrl = oracleServiceInfo.getJdbcUrl();

        // Verify the URL (Service Name format)
        assertEquals("jdbc:oracle:thin:@//localhost:1521/orcl", serviceJdbcUrl, 
                "Oracle JDBC URL with Service Name should be generated correctly");
    }

    @Test
    public void testSqlServerJdbcUrlGeneration() {
        // Create a SQL Server connection info
        SqlServerConnectionBaseInfo sqlServerInfo = new SqlServerConnectionBaseInfo();
        sqlServerInfo.setServerName("localhost");
        sqlServerInfo.setPort("1433");
        sqlServerInfo.setDatabaseName("testdb");
        sqlServerInfo.setUsername("test");
        sqlServerInfo.setPassword("test");

        // Generate the JDBC URL
        String jdbcUrl = sqlServerInfo.getJdbcUrl();

        // Verify the URL
        assertEquals("jdbc:sqlserver://localhost:1433;databaseName=testdb", jdbcUrl, 
                "SQL Server JDBC URL should be generated correctly");

        // Create a SQL Server connection info with instance name
        SqlServerConnectionBaseInfo sqlServerInstanceInfo = new SqlServerConnectionBaseInfo();
        sqlServerInstanceInfo.setServerName("localhost");
        sqlServerInstanceInfo.setPort("1433");
        sqlServerInstanceInfo.setDatabaseName("testdb");
        sqlServerInstanceInfo.setInstanceName("SQLEXPRESS");
        sqlServerInstanceInfo.setUsername("test");
        sqlServerInstanceInfo.setPassword("test");

        // Generate the JDBC URL
        String instanceJdbcUrl = sqlServerInstanceInfo.getJdbcUrl();

        // Verify the URL with instance name
        assertEquals("jdbc:sqlserver://localhost:1433;databaseName=testdb;instanceName=SQLEXPRESS", instanceJdbcUrl, 
                "SQL Server JDBC URL with instance name should be generated correctly");
    }

    @Test
    public void testDB2JdbcUrlGeneration() {
        // Create a DB2 connection info
        DB2ConnectionBaseInfo db2Info = new DB2ConnectionBaseInfo();
        db2Info.setServerName("localhost");
        db2Info.setPort("50000");
        db2Info.setDatabaseName("testdb");
        db2Info.setUsername("test");
        db2Info.setPassword("test");

        // Generate the JDBC URL
        String jdbcUrl = db2Info.getJdbcUrl();

        // Verify the URL
        assertEquals("jdbc:db2://localhost:50000/testdb", jdbcUrl, 
                "DB2 JDBC URL should be generated correctly");
    }

    @Test
    public void testCustomPropertiesInJdbcUrl() {
        // Create a PostgreSQL connection info with custom properties
        PostgresConnectionBaseInfo postgresInfo = new PostgresConnectionBaseInfo();
        postgresInfo.setServerName("localhost");
        postgresInfo.setPort("5432");
        postgresInfo.setDatabaseName("testdb");
        postgresInfo.setUsername("test");
        postgresInfo.setPassword("test");
        postgresInfo.getCustomProperties().put("ssl", "true");
        postgresInfo.getCustomProperties().put("sslmode", "require");
        postgresInfo.getCustomProperties().put("hibernate.show_sql", "true"); // This should be skipped in URL

        // Generate the JDBC URL
        String jdbcUrl = postgresInfo.getJdbcUrl();

        // Verify the URL with custom properties
        assertEquals("jdbc:postgresql://localhost:5432/testdb?ssl=true&sslmode=require", jdbcUrl, 
                "PostgreSQL JDBC URL with custom properties should be generated correctly");
    }

    @Test
    public void testDefaultPortsInJdbcUrl() {
        // Test PostgreSQL with default port
        PostgresConnectionBaseInfo postgresInfo = new PostgresConnectionBaseInfo();
        postgresInfo.setServerName("localhost");
        // Not setting port explicitly
        postgresInfo.setDatabaseName("testdb");

        String postgresJdbcUrl = postgresInfo.getJdbcUrl();
        assertEquals("jdbc:postgresql://localhost:5432/testdb", postgresJdbcUrl, 
                "PostgreSQL JDBC URL should include default port when not explicitly set");

        // Test MySQL with default port
        MySqlConnectionBaseInfo mysqlInfo = new MySqlConnectionBaseInfo();
        mysqlInfo.setServerName("localhost");
        // Not setting port explicitly
        mysqlInfo.setDatabaseName("testdb");

        String mysqlJdbcUrl = mysqlInfo.getJdbcUrl();
        assertEquals("jdbc:mysql://localhost:3306/testdb", mysqlJdbcUrl, 
                "MySQL JDBC URL should include default port when not explicitly set");

        // Test DB2 with default port
        DB2ConnectionBaseInfo db2Info = new DB2ConnectionBaseInfo();
        db2Info.setServerName("localhost");
        // Not setting port explicitly
        db2Info.setDatabaseName("testdb");

        String db2JdbcUrl = db2Info.getJdbcUrl();
        assertEquals("jdbc:db2://localhost:50000/testdb", db2JdbcUrl, 
                "DB2 JDBC URL should include default port when not explicitly set");
    }
}
