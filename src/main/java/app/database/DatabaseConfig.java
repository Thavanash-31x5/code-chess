package app.database;

public record DatabaseConfig(String url, String username, String password) {
    public static DatabaseConfig fromEnvironment() {
        return new DatabaseConfig(
                requiredEnvironmentVariable("DB_URL"),
                requiredEnvironmentVariable("DB_USERNAME"),
                requiredEnvironmentVariable("DB_PASSWORD")
        );
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }

        return value;
    }
}