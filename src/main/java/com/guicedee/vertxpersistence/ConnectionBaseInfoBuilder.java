package com.guicedee.vertxpersistence;



import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.Properties;

/**
 * Populates {@link ConnectionBaseInfo} from standard JPA/Jakarta persistence properties.
 */
public class ConnectionBaseInfoBuilder
		implements IPropertiesConnectionInfoReader<ConnectionBaseInfoBuilder>
{
	/**
	 * Maps JDBC properties (url, user, password, driver) into the connection info.
	 *
	 * @param unit the persistence unit being configured
	 * @param filteredProperties properties scoped to the persistence unit
	 * @param cbi target connection info instance
	 * @return the populated connection info
	 */
	@Override
	public ConnectionBaseInfo populateConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties, ConnectionBaseInfo cbi)
	{
		for (String prop : filteredProperties.stringPropertyNames())
		{
			switch (prop)
			{
				case "javax.persistence.jdbc.url":
				case "jakarta.persistence.jdbc.url":
				{
					cbi.setUrl(filteredProperties.getProperty(prop));
					break;
				}
				case "javax.persistence.jdbc.user":
				case "jakarta.persistence.jdbc.user":
				{
					cbi.setUsername(filteredProperties.getProperty(prop));
					break;
				}
				case "javax.persistence.jdbc.password":
				case "jakarta.persistence.jdbc.password":
				{
					cbi.setPassword(filteredProperties.getProperty(prop));
					break;
				}
				case "javax.persistence.jdbc.driver":
				case "jakarta.persistence.jdbc.driver":
				{
					cbi.setDriverClass(filteredProperties.getProperty(prop));
					break;
				}
				default:
				{
					break;
				}
			}
		}
		return cbi;
	}

	@Override
	public Integer sortOrder()
	{
		return 50;
	}
}
