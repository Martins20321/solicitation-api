package com.martinsdev.solicitation.api.model;

import com.martinsdev.solicitation.api.model.enums.AuditStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_audit_logs")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String action;
    @Column(name = "user_email", nullable = false, length = 80)
    private String userEmail;
    @Column(name = "user_role", nullable = false, length = 50)
    private String userRole;
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditStatus status;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
