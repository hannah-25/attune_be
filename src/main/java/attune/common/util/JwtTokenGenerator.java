package attune.common.util;


import attune.common.config.JwtConfig;
import attune.user.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Component
@RequiredArgsConstructor
public class JwtTokenGenerator {

    private final JwtConfig jwtConfig;

    // 서명키 생성
    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    // Access Token 생성
    public String generateAccessToken(User user){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());
        System.out.println("토큰 만료 시간은 "+expiration);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getUserType().toString())
                .claim("status", user.getUserStatus().toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }


    public String generateRefreshToken(User user){
        Date now = new Date();
        Date expiration = new Date(now.getTime()+jwtConfig.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

}
