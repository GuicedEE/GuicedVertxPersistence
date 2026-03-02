package com.guicedee.persistence.test;

import com.guicedee.persistence.CleanVertxConnectionBaseInfo;
import com.guicedee.persistence.ConnectionBaseInfo;
import com.guicedee.persistence.ConnectionBaseInfoBuilder;
import com.guicedee.persistence.DatabaseModule;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.Properties;

public class TestModule1 extends DatabaseModule<TestModule1>
{
	
	@Override
	protected String getPersistenceUnitName()
	{
		return "guiceinjectionh2test";
	}
	
	@Override
	protected ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnitDescriptor unit, Properties filteredProperties)
	{
		ConnectionBaseInfoBuilder connectionBuilder = new ConnectionBaseInfoBuilder();
		return connectionBuilder.populateConnectionBaseInfo(unit,filteredProperties,new CleanVertxConnectionBaseInfo());
	}

	@Override
	protected String getJndiMapping()
	{
		return "jdbc/testmodule";
	}
}
