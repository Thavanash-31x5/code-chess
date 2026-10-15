package app.login;

public record LoginRequest(
        String email,
        String password
) {
}