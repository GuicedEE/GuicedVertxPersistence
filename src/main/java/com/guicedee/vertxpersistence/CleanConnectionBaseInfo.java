package com.guicedee.vertxpersistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * A cleaner version of ConnectionBaseInfo that only includes the essential properties
 * needed for Vertx SQL client configuration.
 * <p>
 * This class extends ConnectionBaseInfo for backward compatibility but provides
 * a cleaner interface for new code. It only exposes the properties that are actually
 * used by the Vertx SQL client.
 */
@Getter
@Setter
@Accessors(chain = true)
public abstract class CleanConnectionBaseInfo extends ConnectionBaseInfo {

    /**
     * Creates a new CleanConnectionBaseInfo instance.
     */
    public CleanConnectionBaseInfo() {
        super();
    }

    /**
     * Gets the persistence unit name.
     * @return The persistence unit name
     */
    @Override
    public String getPersistenceUnitName() {
        return super.getPersistenceUnitName();
    }

    /**
     * Sets the persistence unit name.
     * @param persistenceUnitName The persistence unit name
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setPersistenceUnitName(String persistenceUnitName) {
        return super.setPersistenceUnitName(persistenceUnitName);
    }

    /**
     * Gets the URL.
     * @return The URL
     */
    @Override
    public String getUrl() {
        return super.getUrl();
    }

    /**
     * Sets the URL.
     * @param url The URL
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setUrl(String url) {
        return super.setUrl(url);
    }

    /**
     * Gets the server name.
     * @return The server name
     */
    @Override
    public String getServerName() {
        return super.getServerName();
    }

    /**
     * Sets the server name.
     * @param serverName The server name
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setServerName(String serverName) {
        return super.setServerName(serverName);
    }

    /**
     * Gets the port.
     * @return The port
     */
    @Override
    public String getPort() {
        return super.getPort();
    }

    /**
     * Sets the port.
     * @param port The port
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setPort(String port) {
        return super.setPort(port);
    }

    /**
     * Gets the driver.
     * @return The driver
     */
    @Override
    public String getDriver() {
        return super.getDriver();
    }

    /**
     * Sets the driver.
     * @param driver The driver
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setDriver(String driver) {
        return super.setDriver(driver);
    }

    /**
     * Gets the driver class.
     * @return The driver class
     */
    @Override
    public String getDriverClass() {
        return super.getDriverClass();
    }

    /**
     * Sets the driver class.
     * @param driverClass The driver class
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setDriverClass(String driverClass) {
        return super.setDriverClass(driverClass);
    }

    /**
     * Gets the class name.
     * @return The class name
     */
    @Override
    public String getClassName() {
        return super.getClassName();
    }

    /**
     * Sets the class name.
     * @param className The class name
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setClassName(String className) {
        return super.setClassName(className);
    }

    /**
     * Gets the username.
     * @return The username
     */
    @Override
    public String getUsername() {
        return super.getUsername();
    }

    /**
     * Sets the username.
     * @param username The username
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setUsername(String username) {
        return super.setUsername(username);
    }

    /**
     * Gets the password.
     * @return The password
     */
    @Override
    @JsonIgnore
    public String getPassword() {
        return super.getPassword();
    }

    /**
     * Sets the password.
     * @param password The password
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setPassword(String password) {
        return super.setPassword(password);
    }

    /**
     * Gets the transaction isolation.
     * @return The transaction isolation
     */
    @Override
    public String getTransactionIsolation() {
        return super.getTransactionIsolation();
    }

    /**
     * Sets the transaction isolation.
     * @param transactionIsolation The transaction isolation
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setTransactionIsolation(String transactionIsolation) {
        return super.setTransactionIsolation(transactionIsolation);
    }

    /**
     * Gets the database name.
     * @return The database name
     */
    @Override
    public String getDatabaseName() {
        return super.getDatabaseName();
    }

    /**
     * Sets the database name.
     * @param databaseName The database name
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setDatabaseName(String databaseName) {
        return super.setDatabaseName(databaseName);
    }

    /**
     * Gets the JNDI name.
     * @return The JNDI name
     */
    @Override
    public String getJndiName() {
        return super.getJndiName();
    }

    /**
     * Sets the JNDI name.
     * @param jndiName The JNDI name
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setJndiName(String jndiName) {
        return super.setJndiName(jndiName);
    }

    /**
     * Gets the minimum pool size.
     * @return The minimum pool size
     */
    @Override
    public Integer getMinPoolSize() {
        return super.getMinPoolSize();
    }

    /**
     * Sets the minimum pool size.
     * @param minPoolSize The minimum pool size
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setMinPoolSize(Integer minPoolSize) {
        return super.setMinPoolSize(minPoolSize);
    }

    /**
     * Gets the maximum pool size.
     * @return The maximum pool size
     */
    @Override
    public Integer getMaxPoolSize() {
        return super.getMaxPoolSize();
    }

    /**
     * Sets the maximum pool size.
     * @param maxPoolSize The maximum pool size
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setMaxPoolSize(Integer maxPoolSize) {
        return super.setMaxPoolSize(maxPoolSize);
    }

    /**
     * Gets the maximum idle time.
     * @return The maximum idle time
     */
    @Override
    public Integer getMaxIdleTime() {
        return super.getMaxIdleTime();
    }

    /**
     * Sets the maximum idle time.
     * @param maxIdleTime The maximum idle time
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setMaxIdleTime(Integer maxIdleTime) {
        return super.setMaxIdleTime(maxIdleTime);
    }

    /**
     * Gets the maximum life time.
     * @return The maximum life time
     */
    @Override
    public Integer getMaxLifeTime() {
        return super.getMaxLifeTime();
    }

    /**
     * Sets the maximum life time.
     * @param maxLifeTime The maximum life time
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setMaxLifeTime(Integer maxLifeTime) {
        return super.setMaxLifeTime(maxLifeTime);
    }

    /**
     * Gets the acquire increment.
     * @return The acquire increment
     */
    @Override
    public Integer getAcquireIncrement() {
        return super.getAcquireIncrement();
    }

    /**
     * Sets the acquire increment.
     * @param acquireIncrement The acquire increment
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setAcquireIncrement(Integer acquireIncrement) {
        return super.setAcquireIncrement(acquireIncrement);
    }

    /**
     * Gets the acquisition timeout.
     * @return The acquisition timeout
     */
    @Override
    public Integer getAcquisitionTimeout() {
        return super.getAcquisitionTimeout();
    }

    /**
     * Sets the acquisition timeout.
     * @param acquisitionTimeout The acquisition timeout
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setAcquisitionTimeout(Integer acquisitionTimeout) {
        return super.setAcquisitionTimeout(acquisitionTimeout);
    }

    /**
     * Gets the test query.
     * @return The test query
     */
    @Override
    public String getTestQuery() {
        return super.getTestQuery();
    }

    /**
     * Sets the test query.
     * @param testQuery The test query
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setTestQuery(String testQuery) {
        return super.setTestQuery(testQuery);
    }

    /**
     * Gets the custom properties.
     * @return The custom properties
     */
    @Override
    public Map<String, String> getCustomProperties() {
        return super.getCustomProperties();
    }

    /**
     * Gets whether this is the default connection.
     * @return Whether this is the default connection
     */
    @Override
    public boolean isDefaultConnection() {
        return super.isDefaultConnection();
    }

    /**
     * Sets whether this is the default connection.
     * @param defaultConnection Whether this is the default connection
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setDefaultConnection(boolean defaultConnection) {
        return super.setDefaultConnection(defaultConnection);
    }

    /**
     * Gets whether this is an XA connection.
     * @return Whether this is an XA connection
     */
    @Override
    public boolean isXa() {
        return super.isXa();
    }

    /**
     * Sets whether this is an XA connection.
     * @param xa Whether this is an XA connection
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setXa(boolean xa) {
        return super.setXa(xa);
    }

    /**
     * Gets the server instance name property.
     * @return The server instance name property
     */
    @Override
    public String getServerInstanceNameProperty() {
        return super.getServerInstanceNameProperty();
    }

    /**
     * Sets the server instance name property.
     * @param serverInstanceNameProperty The server instance name property
     * @return This instance for method chaining
     */
    @Override
    public ConnectionBaseInfo setServerInstanceNameProperty(String serverInstanceNameProperty) {
        return super.setServerInstanceNameProperty(serverInstanceNameProperty);
    }
}