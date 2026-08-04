package com.lightalm.repository;

import com.lightalm.domain.AuditLog;
import com.lightalm.domain.AuditTargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByProjectIdAndTargetTypeAndTargetIdOrderByCreatedAtDesc(Long projectId, AuditTargetType targetType, Long targetId);
}
