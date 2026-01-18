package com.guicedee.vertxpersistence;


import com.guicedee.client.services.IDefaultService;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;


import java.util.Map;
import java.util.Properties;

/**
 * Contributes entity-manager properties derived from a persistence unit.
 *
 * @param <J> self type used by the service loader
 */
public interface IPropertiesEntityManagerReader<J extends IPropertiesEntityManagerReader<J>> extends IDefaultService<J>
{
	/**
	 * Processes and returns properties for the entity manager factory.
	 */
	Map<String, String> processProperties(org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor persistenceUnit,
										  Properties incomingProperties);
	
	/**
	 * Indicates whether this reader applies to the given persistence unit.
	 *
	 * @param persistenceUnit the persistence unit descriptor
	 * @return true when this reader should be used
	 */
	boolean applicable(PersistenceUnitDescriptor persistenceUnit);
}
