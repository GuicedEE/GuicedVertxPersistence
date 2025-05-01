package com.guicedee.vertxpersistence;

import com.guicedee.guicedinjection.interfaces.IDefaultService;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import java.util.Properties;

/**
 * A functional interface to populate a connection base info based on properties received
 */
@FunctionalInterface
public interface IPropertiesConnectionInfoReader<J extends IPropertiesConnectionInfoReader<J>>
	extends IDefaultService<J>
{
	/**
	 * Method populateConnectionBaseInfo ...
	 *
	 * @param unit
	 * 		of type PersistenceUnit
	 * @param filteredProperties
	 * 		of type Properties
	 * @param cbi
	 * 		of type ConnectionBaseInfo
	 *
	 * @return ConnectionBaseInfo
	 */
	ConnectionBaseInfo populateConnectionBaseInfo(ParsedPersistenceXmlDescriptor unit, Properties filteredProperties, ConnectionBaseInfo cbi);
}
