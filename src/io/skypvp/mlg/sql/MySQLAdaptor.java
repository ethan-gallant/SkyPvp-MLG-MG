package io.skypvp.mlg.sql;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;


public class MySQLAdaptor {
	private final MiniConnectionPoolManager conPool;
	private final String host, database, user, password;
	private final int port, connectionsMax, connectionTimeout, connectionMaxIdleTime, connectionMaxRetries;

	public MySQLAdaptor(String host, int port, String database, String user, String password, int connectionsMax,
			int connectionTimeout, int connectionMaxIdleTime, int connectionMaxRetries, String encoding, String characterEncoding, String characterSet)
			throws ClassNotFoundException, SQLException {
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.password = password;
		this.connectionsMax = connectionsMax;
		this.connectionTimeout = connectionTimeout;
		this.connectionMaxIdleTime = connectionMaxIdleTime;
		this.connectionMaxRetries = connectionMaxRetries;
		
		Class.forName("com.mysql.jdbc.Driver");
		MysqlConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
		dataSource.setDatabaseName(this.database);
		dataSource.setServerName(this.host);
		//dataSource.setUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true");
		dataSource.setPort(this.port);
		dataSource.setUser(this.user);
		dataSource.setPassword(this.password);
		dataSource.setAutoReconnect(true);
		dataSource.setAutoReconnectForPools(true);
		dataSource.setAutoReconnectForConnectionPools(true);
		dataSource.setMaxReconnects(this.connectionMaxRetries);
		dataSource.setEncoding(encoding);
		dataSource.setCharacterEncoding(encoding);
		dataSource.setCharacterSetResults(characterSet);
		this.conPool = new MiniConnectionPoolManager(dataSource, this.connectionsMax, this.connectionTimeout,
				this.connectionMaxIdleTime);
	}
	
	public MySQLAdaptor(String host, int port, String database, String user, String password, int connectionsMax,
			int connectionTimeout, int connectionMaxIdleTime, int connectionMaxRetries)
			throws ClassNotFoundException, SQLException {
		this(host, port, database, user, password, connectionsMax, connectionTimeout, connectionMaxIdleTime, connectionMaxRetries, "UTF-8", null, null);
	}
	
	public MySQLAdaptor(String host, int port, String database, String user, String password)
			throws ClassNotFoundException, SQLException {
		this(host, port, database, user, password, 20, 30, 120, 3, "UTF-8", null, null);
	}
	
	public MySQLAdaptor(String host, String database, String user, String password)
			throws ClassNotFoundException, SQLException {
		this(host, 3306, database, user, password, 20, 30, 120, 3, "UTF-8", null, null);
	}

	public ResultSet executeQuery(String query) throws SQLException {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		try {
			connection = getConnection();
			statement = connection.prepareStatement(query);
			result = statement.executeQuery();
		} catch (SQLException e) {
			throw e;
		} finally {
			System.out.println("LOOL BITTE SAGE DAS DEM FLORIAN !");
		}
		return result;
	}

	public Connection getConnection() throws SQLException {
		Connection connection = this.conPool.getConnection();
		int count = 0;
		while (!connection.isValid(5)) {
			System.out.println("[Essentials] Invalid connection. Getting next one.");
			count++;
			connection.close();
			connection = this.conPool.getConnection();
			if (count > this.connectionMaxRetries + 2) {
				System.out.println("[Essentials] Unable to find valid connection. Passing invalid connection. This may cause trouble.");
			}
		}
		return connection;
	}


	public void dispose() {
		try {
			this.conPool.dispose();
		} catch (SQLException e) {
			System.out.println("[Essetnaisl] Unable to dispose conPool.");
		}
	}
}
