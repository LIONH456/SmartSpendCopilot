package com.smartspend.copilot.service;

import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {


    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username){
        // 1. 拿着名字，用你自己的 UserRepository 去查 MySQL
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. 查出来后，不能直接返回你自己的 User 实体，因为 Spring Security 不认识你的类！
        // 3. 所以你要用 Spring 自带的工具箱（User.builder()）把它打包转换成 Spring 认识的“标准居民身份证”（UserDetails）
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("USER")   // 顺便给这个人贴上一个“普通用户”的权限标签
                .build();
    }
}
