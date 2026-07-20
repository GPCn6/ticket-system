package com.jsu.config;

import com.jsu.common.security.InternalAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalAuthInterceptor implements HandlerInterceptor {
    private final String secret;
    public InternalAuthInterceptor(@Value("${internal.auth.secret}") String secret) { this.secret = secret; }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!InternalAuth.isTrusted(request, secret)) { response.setStatus(401); return false; }
        return true;
    }
}
