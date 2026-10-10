package app.registration;

import app.database.DatabaseConfig;
import app.database.JdbcConnectionFactory;
import app.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RegistrationService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final String INSERT_USER_SQL = """
            INSERT INTO users (name, phone_number, email, password_hash)
            VALUES (?, ?, ?, ?)
            """;

    private final DatabaseConfig databaseConfig;

    public RegistrationService(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public long register(RegistrationRequest request)
            throws SQLException, DuplicateEmailException {
        String name = requireValue(request.name(), "Name");
        String phoneNumber = requireValue(request.phoneNumber(), "Phone number");
        String email = requireEmail(request.email());
        String password = requireValue(request.password(), "Password");
        String passwordHash = PasswordHasher.hash(password);

        try (
                Connection connection = JdbcConnectionFactory.openConnection(databaseConfig);
                PreparedStatement statement = connection.prepareStatement(
                        INSERT_USER_SQL,
                        PreparedStatement.RETURN_GENERATED_KEYS
                )
        ) {
            statement.setString(1, name);
            statement.setString(2, phoneNumber);
            statement.setString(3, email);
            statement.setString(4, passwordHash);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("User ID was not generated");
                }

                return generatedKeys.getLong(1);
            }
        } catch (SQLIntegrityConstraintViolationException exception) {
            throw new DuplicateEmailException(email, exception);
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