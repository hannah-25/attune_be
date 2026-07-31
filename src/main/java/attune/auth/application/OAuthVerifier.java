package attune.auth.application;

import attune.auth.application.dto.OAuthUserInfo;
import attune.user.domain.model.OAuthProvider;

public interface OAuthVerifier {
    OAuthProvider provider();
    OAuthUserInfo verify(String token);
}
