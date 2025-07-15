package com.guicedee.vertxpersistence;

/**
 * Persistence provider service. Use this to manage the overall startup and stop of the persistence
 * module(s).
 *
 * <p>TODO(user): Integrate with Service API when appropriate.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public interface PersistService {

  /**
   * Starts the underlying persistence engine and makes guice-persist ready for use. For instance,
   * with JPA, it creates an EntityManagerFactory and may open connection pools. This method must be
   * called by your code prior to using any guice-persist or JPA artifacts. If already started,
   * calling this method does nothing, if already stopped, it also does nothing.
   */
  void start();

  /**
   * Stops the underlying persistence engine. For instance, with JPA, it closes the {@code
   * EntityManagerFactory}. If already stopped, calling this method does nothing. If not yet
   * started, it also does nothing.
   */
  void stop();
}
