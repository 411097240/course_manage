package com.cm.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import java.util.Collections;

/**
 * 自定义JWT认证Token，存储当前登录用户信息
 */
public class JwtUserToken extends AbstractAuthenticationToken {
    private final Long userId;
    private final String username;
    private final Integer role;

    public JwtUserToken(Long userId, String username, Integer role) {
        super(Collections.emptyList());
        this.userId = userId;
        this.username = username;
        this.role = role;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Integer getRole() {
        return role;
    }
}
