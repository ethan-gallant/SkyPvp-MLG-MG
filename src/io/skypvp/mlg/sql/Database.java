package io.skypvp.mlg.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import io.skypvp.mlg.MLGPlugin;
import io.skypvp.mlg.MLGUtils;

public class Database {

	final MLGPlugin main;
	private final String host;
	private final String port;
	private final String username;
	private final String password;
	private final String database;
	private MySQLAdaptor adaptor;

	public Database(MLGPlugin instance, String host, String port, String username, String password, String database) {
		this.main = instance;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.database = database;
		
		try {
			this.openConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void openConnection() throws SQLException {
		try {
			adaptor = new MySQLAdaptor(host, database, username, password);
		} catch (ClassNotFoundException e) {
			main.getLogger().severe("Could not find JDBC MySQL driver, please install it to use this plugin.");
			main.disable();
		}

		return;
	}

	public void updateName(Connection conn, final UUID id) {
		if (id == null)
			return;


		try {
			Statement statement = conn.createStatement();
			statement.executeUpdate(String.format("UPDATE %s SET NAME='%s' WHERE UUID='%s';", MLGUtils.TABLE_NAME,
					Bukkit.getPlayer(id).getName(), id.toString()));
			statement.close();
			conn.close();
		} catch (SQLException e) {
			main.sendConsoleMessage(ChatColor.DARK_RED + "SEVERE!! Encountered an error while saving player stats...");
			e.printStackTrace();
		}
	}
	
	public List<Map<String, Object>> getAllPlayers() {
		
		return this.query("SELECT * FROM " + MLGUtils.TABLE_NAME);
	}

	public List<Map<String, Object>> query(String query) {
		List<Map<String, Object>> objList = new ArrayList<>();

		try {
			Connection conn = this.adaptor.getConnection();
			PreparedStatement statement = conn.prepareStatement(query);
			ResultSet r = statement.executeQuery();
			while (r.next()) {
				Map<String, Object> objMap = new HashMap<>();
				for (int i = 0; i < r.getMetaData().getColumnCount(); i++) {
					objMap.put(r.getMetaData().getColumnName(i+1), r.getObject(i+1));
				}
				objList.add(objMap);
			}
			
			r.close();
			statement.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return objList;
	}

	public boolean tableExists() {
		DatabaseMetaData md;
		try {
			Connection conn = this.adaptor.getConnection();
			md = conn.getMetaData();
			conn.close();
			ResultSet rs = md.getTables(null, null, MLGUtils.TABLE_NAME, null);
			boolean found = false;
			while (rs.next()) {
				found = rs.getString(3).equalsIgnoreCase(MLGUtils.TABLE_NAME);
				break;
			}
			rs.close();
			return found;
		} catch (SQLException e) {
			main.sendConsoleMessage(ChatColor.DARK_RED + "Encountered an error while checking status of table.");
			e.printStackTrace();
			main.disable();
		}

		return false;
	}

	public void createTable() {
		try {
			Connection conn = this.adaptor.getConnection();
			Statement statement = conn.createStatement();
			statement.executeUpdate(String.format(
					"CREATE TABLE IF NOT EXISTS %s(UUID varchar(36), NAME varchar(16), POINTS int, STREAK int, PRIMARY KEY (UUID));",
					MLGUtils.TABLE_NAME));
			statement.close();
			conn.close();
			main.sendConsoleMessage(ChatColor.DARK_GREEN + "Successfully created database table.");
		} catch (SQLException | NullPointerException e) {
			main.getLogger().severe(
					String.format("Encountered an error while trying to create table. Exception: %s", e.getMessage()));
			main.sendConsoleMessage(ChatColor.DARK_RED
					+ "Ignore the following stack trace if NullPointerException OR table already created. Restart plugin.");
			e.printStackTrace();
			main.disable();
		}
	}
	
	public MySQLAdaptor getAdaptor() {
		return adaptor;
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
