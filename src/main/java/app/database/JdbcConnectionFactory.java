package app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class JdbcConnectionFactory {
    private JdbcConnectionFactory() {
    }

    public static Connection openConnection(DatabaseConfig config) throws SQLException {
        return DriverManager.getConnection(
                config.url(),
                config.username(),
                config.password()
        );
    }
}