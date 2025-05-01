package com.guicedee.vertxpersistence.bind;


import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import io.vertx.sqlclient.SqlClient;

//todo jdbc vertx connection base info
public class JPAConnectionBaseInfo
		extends ConnectionBaseInfo
{
	/**
	 * You can fetch it directly from the entity manager factory
	 *
	 * @return Null as this is a placeholder implementation
	 */
	@Override
	public SqlClient toPooledDatasource()
	{
		return null;
	}
}
