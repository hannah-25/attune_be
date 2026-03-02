package attune.common.filter;

import attune.common.security.CustomUserDetails;
import attune.common.util.JwtTokenValidator;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";


    // 모든 HTTP 요청이 이 메서드를 거쳐감
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // JWT 토큰 추출
        String token = resolveToken(request);

        if(!StringUtils.hasText(token)){
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 유효성 및 만료 검증
        if(!jwtTokenValidator.validateToken(token) || jwtTokenValidator.isTokenExpired(token) ){

        // 만료된 토큰의 경우 401 응답
            if(jwtTokenValidator.isTokenExpired(token)){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Access token expired\"}");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }


        // 인증 객체 생성
        try{
            UUID userId = jwtTokenValidator.getUserIdFromToken(token);
            UserType userType = UserType.valueOf(jwtTokenValidator.getUserTypeFromToken(token));
            UserStatus userStatus = UserStatus.valueOf(jwtTokenValidator.getUserStatusFromToken(token));

            CustomUserDetails userDetails = CustomUserDetails.fromJwt(userId, userType, userStatus);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);


        } catch (Exception e){
            // 에러 로그 남기고 SecurityContext 초기화
            log.error("JWT 토큰 처리 중 오류 발생 : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    // HttpServletRequest 에서 JWT 토큰 추출
    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)){
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;

    }


}