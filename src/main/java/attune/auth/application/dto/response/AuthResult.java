package attune.auth.application.dto.response;

public record AuthResult(LoginResponse loginResponse, String refreshToken) {
}
