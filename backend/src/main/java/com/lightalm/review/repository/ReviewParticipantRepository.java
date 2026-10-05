package com.lightalm.review.repository;

import com.lightalm.review.domain.ReviewParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewParticipantRepository extends JpaRepository<ReviewParticipant, Long> {

    List<ReviewParticipant> findByReviewCycleIdOrderById(Long reviewCycleId);

    Optional<ReviewParticipant> findByReviewCycleIdAndUserId(Long reviewCycleId, Long userId);

    boolean existsByReviewCycleIdAndUserId(Long reviewCycleId, Long userId);
}
