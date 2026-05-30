package com.smartspend.copilot.service;

import com.smartspend.copilot.dto.request.RegisterRequest;
import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
//    UserRepository userRepository;
//    PasswordEncoder passwordEncoder;
//
//    public void register(RegisterRequest request){
//        if(userRepository.existsByUsername(request.getUsername())){
//            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
//        }
//
//        if(userRepository.existsByEmail(request.getEmail())){
//            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
//        }
//
//        User user = User.builder()
//                .username(request.getUsername())
//                .email(request.getEmail())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .build();
//
//        userRepository.save(user);
//    }
}
