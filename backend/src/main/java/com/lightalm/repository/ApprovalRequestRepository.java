package com.lightalm.repository;

import com.lightalm.domain.ApprovalRequest;
import com.lightalm.domain.ApprovalStatus;
import com.lightalm.domain.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long>, JpaSpecificationExecutor<ApprovalRequest> {

    boolean existsByTargetTypeAndTargetIdAndStatus(TargetType targetType, Long targetId, ApprovalStatus status);
}
