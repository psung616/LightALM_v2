package com.lightalm.license.domain;

import com.lightalm.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * ADR-011 §3.1. 상태 변경은 {@link #supersede()}/{@link #revoke()} 도메인 동사 메서드로만 한다(세터 금지).
 */
@Entity
@Table(name = "licenses")
@Getter
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_key", nullable = false, unique = true, length = 100)
    private String licenseKey;

    @Column(name = "organization_name", nullable = false, length = 150)
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_type", nullable = false, length = 20)
    private LicenseType licenseType;

    @Column(name = "seat_limit", nullable = false)
    private Integer seatLimit;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LicenseStatus status;

    @Column(name = "raw_file_name", nullable = false, length = 255)
    private String rawFileName;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    protected License() {
        // JPA
    }

    @Builder
    private License(String licenseKey, String organizationName, LicenseType licenseType, Integer seatLimit,
                     LocalDateTime issuedAt, LocalDateTime expiresAt, String rawFileName, String rawPayload,
                     Boolean signatureValid, User uploadedBy) {
        this.licenseKey = licenseKey;
        this.organizationName = organizationName;
        this.licenseType = licenseType;
        this.seatLimit = seatLimit;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.status = LicenseStatus.ACTIVE;
        this.rawFileName = rawFileName;
        this.rawPayload = rawPayload;
        this.signatureValid = signatureValid;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
    }

    public void supersede() {
        this.status = LicenseStatus.SUPERSEDED;
    }

    public void revoke() {
        this.status = LicenseStatus.REVOKED;
    }

    public boolean isActive() {
        return status == LicenseStatus.ACTIVE;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}
