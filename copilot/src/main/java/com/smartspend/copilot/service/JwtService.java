package com.smartspend.copilot.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtService {

    // this is how jwt payload looks like:
    //    {
    //        "sub": "lionh456",
    //            "iat": 1748500000,
    //            "exp": 1748586400
    //    }

    @Value("${jwt.secret}")
    String jwtSecret;

    @Value("${jwt.expiration}")
    long jwtExpiration;

    // 把你在配置文件里写的那串普通的“文字密码”（jwtSecret），转换成符合 Java HMAC 算法安全标准、
    // 可以真正用于加密的物理钥匙对象（SecretKey）
    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 什么时候用：用户在 Flutter 手机端第一次输入密码登录，
    // 后端校验密码正确后的瞬间，立刻调用这个方法传入用户名，生成一张通行证发回给手机保存
    public String generateToken(String username){
        return Jwts.builder()
                .subject(username)      // 1. 塞入用户名（主体字段 "sub"）
                .issuedAt(new Date())   // 2. 塞入当前签发时间（"iat"）
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))   // 3. 当前系统时间 + 24小时，算出并塞入过期死期（"exp"）
                .signWith(getSigningKey()) // 4. 用刚才的数字钢印对上面所有数据盖章加密
                .compact();     // 5. 压缩打包，变成一串发给手机端的超长 Base64 字符串
    }

    // 查看通行证的主人（提取用户名）
    public String extractUsername(String token){
        return extractClaims(token).getSubject();
    }

    // 验证通行证真伪与效期（验证 Token）
    public boolean isTokenValid(String token, UserDetails userDetails){
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // 辅助功能：检查令牌是否寿终正寝
    private boolean isTokenExpired(String token){
        return extractClaims(token).getExpiration().before(new Date());
    }


    // 上面的“提取用户名”和“检查是否过期”想要数据，都必须首先调用它来拆包。
    // 安全精髓：verifyWith 保证了安全性。只要黑客不知道你在 .env 里写的那个密钥，他就绝对不可能伪造出能通过这一步的 Token。
    private Claims extractClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())    // 1. 拿着你唯一的钢印去比对防伪。如果黑客篡改过Token，这一步直接抛异常崩掉！
                .build()
                .parseSignedClaims(token)       // 2. 验伪通过，放心地撕开信封
                .getPayload();        // 3. 拿出里面包含 sub, iat, exp 等字段的 JSON 数据块（Claims）
    }

}
