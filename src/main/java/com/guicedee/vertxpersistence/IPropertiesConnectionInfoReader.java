package com.guicedee.vertxpersistence;

import com.guicedee.client.services.IDefaultService;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;


import java.util.Properties;

/**
 * Populates a {@link ConnectionBaseInfo} from persistence unit properties.
 * Implementations typically map provider-specific keys to connection settings.
 *
 * @param <J> self type used by the service loader
 */
@FunctionalInterface
public interface IPropertiesConnectionInfoReader<J extends IPropertiesConnectionInfoReader<J>>
	extends IDefaultService<J>
{
	/**
	 * Populates the given connection info using filtered persistence properties.
	 *
	 * @param unit the persistence unit descriptor being configured
	 * @param filteredProperties properties applicable to this unit
	 * @param cbi the connection info to populate
	 * @return the populated connection info (usually the same instance)
	 */
	ConnectionBaseInfo populateConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties, ConnectionBaseInfo cbi);
}
