package com.smartspend.copilot.config;

import tools.jackson.databind.ObjectMapper;
import com.smartspend.copilot.dto.response.ApiErrorResponse;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.service.CustomUserDetailService;
import com.smartspend.copilot.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
// extends OncePerRequestFilter: Guarantees that this filter code executes exactly once
// for every single API request that hits your server.
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailService userDetailService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,     // Contains all data coming from Flutter (headers, URLs, payloads).
            HttpServletResponse response,   // Used if you need to send back data directly from the filter.
            FilterChain filterChain     // The long line of security checks. Calling filterChain.doFilter passes the request to the next check down the line.
    ) throws ServletException, IOException {
        // 1。检查请求头是否携带Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")){
            // Since they didn't provide a JWT, we just say: "Move along to the next security checkpoint."
            // (If they are trying to reach a public page like /login, they will pass.
            // If they are trying to see a secured bill page, Spring will block them later.)
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. 裁剪并解密Token
            // Extracts the raw token characters. Because "Bearer " is exactly 7 characters long
            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);

            // 3. 核对身份与查验真伪
            // SecurityContextHolder...getAuthentication() == null: Checks if Spring Security's memory is
            // currently empty for this request. This prevents the server from doing redundant
            // validation math if the user was already approved earlier in this request chain.
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                UserDetails userDetails = userDetailService.loadUserByUsername(username);

                if(jwtService.isTokenValid(token,userDetails)) {
                    // 4. 盖章认证并彻底放行（发Boarding Pass）
                    // Creates a standard internal identity card (UsernamePasswordAuthenticationToken) that
                    // Spring Security understands.
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // The password parameter (set to null because they are already authenticated via JWT; we don't need their raw password anymore.)
                            userDetails.getAuthorities());  // The "USER" role label

                    // Attaches extra network metadata to the identity card (like the user's IP address and
                    // browser/device info) just in case you want to log it for auditing purposes.
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (AppException e) {
            sendErrorResponse(response, e.getErrorCode(), request.getRequestURI());
        } catch (Exception e) {
            sendErrorResponse(response, ErrorCode.INVALID_TOKEN, request.getRequestURI());
        }
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode, String path) throws IOException {
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .error(errorCode.getStatus().getReasonPhrase())
                .message(errorCode.getMessage())
                .path(path)
                .build();

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
