package io.skypvp.mlg;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

public class Database {
	
	final MLGArena main;
	private final String host;
	private final String port;
	private final String username;
	private final String password;
	private final String database;
	
	public Database(MLGArena instance, String host, String port, String username, String password, String database) {
		this.main = instance;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.database = database;
	}
	
	public Connection openConnection(boolean isInitial) throws SQLException {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s",
					host, port, database), username, password);
			
			if(isInitial) {
				new BukkitRunnable() {
					public void run() {
						main.databaseConnected();
					}
				}.runTask(main);
				main.sendConsoleMessage(ChatColor.DARK_GREEN + "Successfully connected to MySQL database!");
			}

			return conn;
		} catch (ClassNotFoundException e) {
			main.getLogger().severe("Could not find JDBC MySQL driver, please install it to use this plugin.");
			main.disable();
		}
		
		return null;
	}
	
	public void updateName(Connection conn, final UUID id) {
		if(id == null) return;
		
		Statement statement;
		
		try {
			statement = conn.createStatement();
			statement.executeUpdate(String.format("UPDATE %s SET NAME='%s' WHERE UUID='%s';", MLGUtils.TABLE_NAME, Bukkit.getPlayer(id).getName(), id.toString()));
		} catch (SQLException e) {
			main.sendConsoleMessage(ChatColor.DARK_RED + "SEVERE!! Encountered an error while saving player stats...");
			e.printStackTrace();
		}
	}
	
	public void updatePlayerStats(final UUID id, final Connection useConn) {
		if(id == null) return;
		final int points = main.getPoints(id);
		final int streak = main.getStreak(id);
		
		Connection conn = null;
		
		if(useConn != null) {
			conn = useConn;
			try {
				if(useConn.isClosed()) {
					try {
						conn = openConnection(false);
					} catch (SQLException e) {
						main.sendConsoleMessage(ChatColor.DARK_RED + "SEVERE!! Encountered an error while saving player stats...");
						e.printStackTrace();
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}else {
			try {
				conn = openConnection(false);
			} catch (SQLException e) {
				main.sendConsoleMessage(ChatColor.DARK_RED + "SEVERE!! Encountered an error while saving player stats...");
				e.printStackTrace();
			}
		}
		
		try {
			Statement statement;
			try {
				statement = conn.createStatement();
				if(Bukkit.getPlayer(id) != null) updateName(conn, id);
				statement.executeUpdate(String.format("UPDATE %s SET POINTS=%d, STREAK=%d WHERE UUID='%s';", MLGUtils.TABLE_NAME, points, streak, id.toString()));
			} catch (SQLException e) {
				main.sendConsoleMessage(ChatColor.DARK_RED + "SEVERE!! Encountered an error while saving player stats...");
				e.printStackTrace();
			}
		} finally {
			if(useConn == conn) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void handlePlayerExit(final UUID id) {
		updatePlayerStats(id, null);
		main.getPoints().remove(id);
		main.getStreaks().remove(id);
		main.getInArena().remove(id);
	}
	
	public MySQLQuery query(String query) {
		ResultSet rs = null;
		Connection conn = null;
		
		try {
			conn = openConnection(false);
			try {
				if(conn != null && !conn.isClosed()) {
					Statement statement = conn.createStatement();
					rs = statement.executeQuery(query);
					if(!rs.next()) rs = null;
				}
			}catch (SQLException e) {
				main.sendConsoleMessage(ChatColor.DARK_RED + "Encountered an error while attempting to execute MySQL query.");
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return new MySQLQuery(conn, rs);
	}
	
	public boolean tableExists(Connection conn) {
		DatabaseMetaData md;
		try {
			md = conn.getMetaData();
			ResultSet rs = md.getTables(null, null, MLGUtils.TABLE_NAME, null);
			boolean found = false;
			while (rs.next()) {
				found = rs.getString(3).equalsIgnoreCase(MLGUtils.TABLE_NAME);
				break;
			}
			return found;
		} catch (SQLException e) {
			main.sendConsoleMessage(ChatColor.DARK_RED + "Encountered an error while checking status of table.");
			e.printStackTrace();
			main.disable();
		}
		
		return false;
	}
	
	public void createTable(Connection conn) {
		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS %s(UUID varchar(36), NAME varchar(16), POINTS int, STREAK int, PRIMARY KEY (UUID));", MLGUtils.TABLE_NAME));
			main.sendConsoleMessage(ChatColor.DARK_GREEN + "Successfully created database table.");
		} catch (SQLException | NullPointerException e) {
			main.getLogger().severe(String.format("Encountered an error while trying to create table. Exception: %s",
				e.getMessage()));
			main.sendConsoleMessage(ChatColor.DARK_RED + "Ignore the following stack trace if NullPointerException OR table already created. Restart plugin.");
			e.printStackTrace();
			main.disable();
		}
	}
	
	public String getHost() {
		return this.host;
	}
	
	public String getPort() {
		return this.port;
	}
	
	public String getUsername() {
		return this.username;
	}
	
	public String getPassword() {
		return this.password;
	}
	
	public String getDatabase() {
		return this.database;
	}

}

class MySQLQuery {
	
	final Connection conn;
	final ResultSet set;
	
	public MySQLQuery(Connection conn, ResultSet set) {
		this.conn = conn;
		this.set = set;
	}
	
	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public Connection getConnection() {
		return this.conn;
	}
	
	public ResultSet getResults() {
		return this.set;
	}
}
