package com.smartspend.copilot.service;

import com.smartspend.copilot.dto.request.LoginRequest;
import com.smartspend.copilot.dto.request.RegisterRequest;
import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;

    public void register(RegisterRequest request){
        if(userRepository.existsByUsername(request.getUsername())){
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if(userRepository.existsByEmail(request.getEmail())){
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
    }

    public String login(LoginRequest request){
        User user = userRepository.findByUsername(request.getLogin())
                .or(()->userRepository.findByEmail(request.getLogin()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if(!matches){
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return jwtService.generateToken(user.getUsername());
    }
}
