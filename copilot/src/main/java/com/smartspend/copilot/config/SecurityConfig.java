package com.smartspend.copilot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private static final String[] PUBLIC_ENDPOINTS = {  // 登录注册不需要带 Token 的 EndPoint
            "/api/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {


        http
                // 1. 禁用 CSRF 防御（因为无状态的 JWT 天生免疫 CSRF 攻击）
                // CSRF 攻击是建立在浏览器自动携带 Cookie 的特性上的。而你现在做的是给 Flutter 手机端用的 REST API，
                // 手机端不会用 Cookie，而是用请求头里的 JWT Token。留着它只会阻挡你的手机端发送 POST 请求。
                .csrf(csrf -> csrf.disable())
                // 2. 告诉系统：我是无状态的，别给我创建 Session
                // 告诉服务器不需要记住任何人，每次请求过来，我们只认他们带过来的 JWT Token 通行证。
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 3. 配置路由放行规则
                .authorizeHttpRequests(authorize ->
                        authorize.requestMatchers(
                                PUBLIC_ENDPOINTS
                        ).permitAll() // 这几个不用登录（不需要jwt）
                                .anyRequest().authenticated()) // 其他所有记账接口必须带 Token
                .addFilterBefore(jwtAuthenticationFilter, // 任何request都要通过这个filter
                        UsernamePasswordAuthenticationFilter.class);
                return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

