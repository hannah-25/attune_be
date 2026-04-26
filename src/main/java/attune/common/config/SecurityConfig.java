package attune.common.config;


import attune.common.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origin-patterns:}")
    private java.util.List<String> corsAllowedOriginPatterns;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 스프링 시큐리티의 인증 매니저 빈 -> 내부적으로 UserDetailsService 사용함!
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API에서는 일반적으로 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 필터 활성화
                .cors(cors -> {})

                // 클릭재킹 방지
                .headers(headers -> headers
                        .addHeaderWriter(new XFrameOptionsHeaderWriter(
                                XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
                )

                // JWT 토큰 기반 인증에서는 세션을 사용하지 않음!
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // OPTIONS 요청(Preflight 요청)을 허용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ 헬스/인포는 무조건 허용
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/health/**").permitAll()

                        // 인증 관련 엔드포인트
                        .requestMatchers("/auth/**", "/oauth2/**", "/login/oauth2/**", "/api/account/signup", "/api/auth/login", "/api/auth/reissue").permitAll()

                        // 비밀번호 재설정 (비로그인 허용)
                        .requestMatchers("/api/account/password/reset/**").permitAll()

                        // 개발/문서화 도구
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        //Health Check
                        .anyRequest().authenticated()

                )
                // JWT 인증 필터를 UsernamePassword 필터 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    // 실제 CORS 정책 정의
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발/배포 환경에서 사용할 수 있는 기본 도메인 패턴들
        java.util.List<String> defaultPatterns = java.util.List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://attune-me.com"
        );

        // application-{profile}.yml의 cors.allowed-origin-patterns 값 사용
        java.util.List<String> allowedPatterns = (corsAllowedOriginPatterns != null && !corsAllowedOriginPatterns.isEmpty())
                ? corsAllowedOriginPatterns
                : defaultPatterns;

        configuration.setAllowedOriginPatterns(allowedPatterns);



        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 모든 헤더 허용 (클라이언트 -> 브라우저)
        //Authorization, Content-Type, X-Custom-Header 등
        //Preflight 검사 영향: Access-Control-Request-Headers
        configuration.addAllowedHeader("*");


        // 브라우저 보안 정책: CORS 요청시 안전한 헤더만 자동으로 노출
        //나머지 헤더는 기본적으로 "숨겨짐"
        // 허용 헤더 명시(백엔드 -> 브라우저)
        configuration.setExposedHeaders(java.util.List.of(
                "Location", // 리다이렉트 위치
                "Content-Disposition", // 파일 다운로드
                "X-Export-Message", // 커스텀: 내보내기 메시지
                "X-Export-Status" // 커스텀: 내보내기 상태
        ));


        configuration.setAllowCredentials(true);  // 쿠키/인증 헤더 포함 가능


        // Preflight 결과 캐싱 시간: 1시간
        //브라우저가 Preflight 결과를 캐시해 동일 요청시 생략
        configuration.setMaxAge(java.time.Duration.ofHours(1));


        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
