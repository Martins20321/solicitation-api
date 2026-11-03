package com.martinsdev.solicitation.api.service;

import com.martinsdev.solicitation.api.model.AuditLog;
import com.martinsdev.solicitation.api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception exception) {
            log.error("Failed to save AuditLog [action={}, user={}]", auditLog.getAction(), auditLog.getUserEmail(), exception);
        }
    }
}
