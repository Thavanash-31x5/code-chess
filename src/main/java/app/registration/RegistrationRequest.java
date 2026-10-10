package app.registration;

public record RegistrationRequest(
        String name,
        String phoneNumber,
        String email,
        String password
) {
}