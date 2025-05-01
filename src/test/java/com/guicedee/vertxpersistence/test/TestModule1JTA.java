package com.guicedee.vertxpersistence.test;

import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.DatabaseModule;
import com.guicedee.vertxpersistence.VertxConnectionBaseInfo;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import java.util.Properties;

public class TestModule1JTA extends DatabaseModule<TestModule1JTA>
{
	
	@Override
	protected String getPersistenceUnitName()
	{
		return "guiceinjectionh2testJTA";
	}
	
	@Override
	protected ConnectionBaseInfo getConnectionBaseInfo(ParsedPersistenceXmlDescriptor unit, Properties filteredProperties)
	{
		return new VertxConnectionBaseInfo().setDefaultConnection(false);
	}
	
	@Override
	protected String getJndiMapping()
	{
		return "jdbc/testmoduleJTA";
	}
}
