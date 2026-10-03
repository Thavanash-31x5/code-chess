package app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JdbcConnectionFactory{
    private JdbcConnectionFactory(){

    }

    public static Connection openCOnnection(DatabaseConfig config) throws SQLException {
        return DriverManager.getConnection(
            config.url(), 
            config.username(), 
            config.password());
    }
}