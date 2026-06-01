package com.smartspend.copilot.service;

import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurrentUserService {
    UserRepository userRepository;

    public User getCurrentUser(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() ->new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
