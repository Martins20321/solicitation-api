package com.martinsdev.solicitation.api.infra.aop;

import com.martinsdev.solicitation.api.model.AuditLog;
import com.martinsdev.solicitation.api.model.User;
import com.martinsdev.solicitation.api.model.enums.AuditStatus;
import com.martinsdev.solicitation.api.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(audit)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {

        // dados necessários
        long start = System.currentTimeMillis();
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
            Long duration = System.currentTimeMillis() - start;
            log.info("[AUDIT] action={} user={} role={} duration={}ms status=SUCCESS", action, userEmail, userRole, duration);

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .userEmail(userEmail)
                    .userRole(userRole)
                    .durationMs(duration)
                    .status(AuditStatus.SUCCESS)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogService.save(auditLog);
            return result;
        } catch (Throwable e) {
            Long duration = System.currentTimeMillis() - start;
            log.error("[AUDIT] action={} user={} role={} duration={}ms status=ERROR[{}]", action, userEmail, userRole, duration, e.getMessage());

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .userEmail(userEmail)
                    .userRole(userRole)
                    .durationMs(duration)
                    .status(AuditStatus.ERROR)
                    .errorMessage(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogService.save(auditLog);
            throw e;
        }
    }
}
