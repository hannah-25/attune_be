package attune.common.util;


import attune.common.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenValidator {

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }


    // 토큰 유효성 검사
    public boolean validateToken(String token){
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e){
            log.warn("Invalid JWT token : {}", e.getMessage());
            return false;
        }
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(String token){
        try{
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e){
            log.warn("Failed to parse token for expiration check: {}", e.getMessage());
            return false;
        }
    }


    // 토큰 파싱 메서드
    public Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰에서 사용자 ID 추출
    public UUID getUserIdFromToken(String token){
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String accessToken){
        Claims claims = parseToken(accessToken);
        String email = claims.get("email", String.class);
        if(email == null){
            throw new JwtException("Required 'email' claim is missing from the token");
        }
        return email;
    }

    public String getUserTypeFromToken(String accessToken){
        Claims claims = parseToken(accessToken);
        String userType = claims.get("userType", String.class);
        if(userType == null){
            throw new JwtException("Required 'userType' claim is missing from the token");
        }
        return userType;
    }


}
