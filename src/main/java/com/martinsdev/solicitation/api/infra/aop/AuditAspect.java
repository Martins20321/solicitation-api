package com.martinsdev.solicitation.api.infra.aop;

import com.martinsdev.solicitation.api.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    @Around("@annotation(audit)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {

        // dados necessários
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String action = audit.action();

        // objeto autenticado
        String userEmail = "anonymous";
        String userRole = "anonymous";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            userEmail = user.getEmail();
            userRole = user.getRole().name();
        }

        try {
            Object result = joinPoint.proceed(); // chamando o metodo real
            long duration = System.currentTimeMillis() - start;
            log.info("[AUDIT] action={} user={} role={} duration={}ms status=SUCCESS", action, userEmail,userRole, duration);

            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[AUDIT] action={} user={} role={} duration={}ms status=ERROR[{}]", action, userEmail, userRole, duration, e.getMessage());

            throw e;
        }
    }
}
