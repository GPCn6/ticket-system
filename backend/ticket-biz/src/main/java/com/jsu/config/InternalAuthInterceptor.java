package com.jsu.config;

import com.jsu.common.security.InternalAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

@Component
public class InternalAuthInterceptor implements HandlerInterceptor {
    private final String secret;
    public InternalAuthInterceptor(@Value("${internal.auth.secret}") String secret) { this.secret = secret; }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!InternalAuth.isTrusted(request, secret)) { response.setStatus(401); return false; }
        if (requiresAdmin(request) && !"admin".equalsIgnoreCase(request.getHeader(InternalAuth.USER_ROLE_HEADER))) {
            response.setStatus(403); return false;
        }
        return true;
    }
    private boolean requiresAdmin(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if (path.startsWith("/api/admin/") || path.equals("/api/order/admin/list") || path.equals("/api/show/stats")) return true;
        if (path.startsWith("/api/show/") || path.startsWith("/api/ticket/") || path.startsWith("/api/seckill/")) {
            if ("GET".equals(method)) return "/api/seckill/all".equals(path);
            return !"/api/seckill/execute".equals(path);
        }
        return false;
    }
}
