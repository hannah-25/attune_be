package attune.user.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OAuthProvider {
    GOOGLE, KAKAO, APPLE;

    @JsonCreator
    public static OAuthProvider from(String value) {
        return valueOf(value.toUpperCase());
    }
}
