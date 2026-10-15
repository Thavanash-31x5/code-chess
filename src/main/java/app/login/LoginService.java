package app.login;

import app.database.DatabaseConfig;
import app.database.JdbcConnectionFactory;
import app.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class LoginService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final String FIND_PASSWORD_HASH_SQL = """
            SELECT password_hash
            FROM users
            WHERE email = ?
            """;

    private final DatabaseConfig databaseConfig;

    public LoginService(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public boolean authenticate(LoginRequest request) throws SQLException {
        String email = requireEmail(request.email());
        String password = requireValue(request.password(), "Password");

        try (
                Connection connection =
                        JdbcConnectionFactory.openConnection(databaseConfig);
                PreparedStatement statement =
                        connection.prepareStatement(FIND_PASSWORD_HASH_SQL)
        ) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }

                String storedHash = resultSet.getString("password_hash");
                return PasswordHasher.verify(password, storedHash);
            }
        }
    }

    private static String requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }

    private static String requireEmail(String value) {
        String email = requireValue(value, "Email").toLowerCase(Locale.ROOT);

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email format is invalid");
        }

        return email;
    }
}