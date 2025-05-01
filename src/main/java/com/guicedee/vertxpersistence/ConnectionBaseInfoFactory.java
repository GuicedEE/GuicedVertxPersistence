package com.guicedee.vertxpersistence;

import com.guicedee.vertxpersistence.implementations.db2.DB2ConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.mysql.MySqlConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.oracle.OracleConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.postgres.PostgresConnectionBaseInfo;
import com.guicedee.vertxpersistence.implementations.sqlserver.SqlServerConnectionBaseInfo;

/**
 * A factory class for creating database-specific ConnectionBaseInfo implementations.
 * This class provides methods to create the appropriate ConnectionBaseInfo implementation
 * based on the database type.
 */
public class ConnectionBaseInfoFactory {

    /**
     * Creates a ConnectionBaseInfo implementation for the specified database type.
     *
     * @param databaseType The type of database (e.g., "postgresql", "mysql", "oracle", "sqlserver", "db2")
     * @return A ConnectionBaseInfo implementation for the specified database type, or a generic VertxConnectionBaseInfo if the type is not recognized
     */
    public static ConnectionBaseInfo createConnectionBaseInfo(String databaseType) {
        return createConnectionBaseInfo(databaseType, false);
    }

    /**
     * Creates a ConnectionBaseInfo implementation for the specified database type with the specified XA mode.
     *
     * @param databaseType The type of database (e.g., "postgresql", "mysql", "oracle", "sqlserver", "db2")
     * @param xa Whether this is an XA connection
     * @return A ConnectionBaseInfo implementation for the specified database type, or a generic VertxConnectionBaseInfo if the type is not recognized
     */
    public static ConnectionBaseInfo createConnectionBaseInfo(String databaseType, boolean xa) {
        if (databaseType == null) {
            return new VertxConnectionBaseInfo(xa);
        }

        String type = databaseType.toLowerCase();

        switch (type) {
            case "postgresql":
            case "postgres":
                return new PostgresConnectionBaseInfo(xa);
            case "mysql":
            case "mariadb":
                return new MySqlConnectionBaseInfo(xa);
            case "oracle":
                return new OracleConnectionBaseInfo(xa);
            case "sqlserver":
            case "mssql":
                return new SqlServerConnectionBaseInfo(xa);
            case "db2":
                return new DB2ConnectionBaseInfo(xa);
            default:
                return new VertxConnectionBaseInfo(xa);
        }
    }

    /**
     * Creates a ConnectionBaseInfo implementation based on the JDBC URL.
     * This method attempts to determine the database type from the JDBC URL.
     *
     * @param jdbcUrl The JDBC URL (e.g., "jdbc:postgresql://localhost:5432/mydatabase")
     * @return A ConnectionBaseInfo implementation for the database type specified in the URL, or a generic VertxConnectionBaseInfo if the type cannot be determined
     */
    public static ConnectionBaseInfo createConnectionBaseInfoFromJdbcUrl(String jdbcUrl) {
        return createConnectionBaseInfoFromJdbcUrl(jdbcUrl, false);
    }

    /**
     * Creates a ConnectionBaseInfo implementation based on the JDBC URL with the specified XA mode.
     * This method attempts to determine the database type from the JDBC URL.
     *
     * @param jdbcUrl The JDBC URL (e.g., "jdbc:postgresql://localhost:5432/mydatabase")
     * @param xa Whether this is an XA connection
     * @return A ConnectionBaseInfo implementation for the database type specified in the URL, or a generic VertxConnectionBaseInfo if the type cannot be determined
     */
    public static ConnectionBaseInfo createConnectionBaseInfoFromJdbcUrl(String jdbcUrl, boolean xa) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            return new VertxConnectionBaseInfo(xa);
        }

        // Remove jdbc: prefix if present
        String url = jdbcUrl.startsWith("jdbc:") ? jdbcUrl.substring(5) : jdbcUrl;

        // Extract database type
        int colonIndex = url.indexOf(':');
        if (colonIndex > 0) {
            String dbType = url.substring(0, colonIndex);
            return createConnectionBaseInfo(dbType, xa);
        }

        // If we can't determine the database type, return a generic VertxConnectionBaseInfo
        return new VertxConnectionBaseInfo(xa);
    }
}