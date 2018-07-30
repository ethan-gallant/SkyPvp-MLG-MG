package io.skypvp.mlg.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlQuery {

    final Connection conn;
    final ResultSet set;

    public MySqlQuery(Connection conn, ResultSet set) {
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