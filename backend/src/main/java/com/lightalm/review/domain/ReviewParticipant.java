package com.lightalm.review.domain;

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
 * ADR-008(Phase 16) / 03-data-model.md §3.19. 리뷰 사이클 참여자 한 명의 결정(기록용).
 *
 * <p><b>범용 워크플로우 엔진이 아니다</b>: {@code decision}은 기록·표시 용도이며, 이 값을 근거로
 * 대상(요구사항/이슈)의 status를 자동으로 바꾸는 로직은 서비스 레이어에도 두지 않는다.</p>
 */
@Entity
@Table(name = "review_participants")
@Getter
public class ReviewParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_cycle_id", nullable = false)
    private ReviewCycle reviewCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    protected ReviewParticipant() {
        // JPA
    }

    @Builder
    private ReviewParticipant(ReviewCycle reviewCycle, User user, ReviewDecision decision, String comment) {
        this.reviewCycle = reviewCycle;
        this.user = user;
        this.decision = decision != null ? decision : ReviewDecision.PENDING;
        this.comment = comment;
    }

    /** 본인의 결정을 기록한다. 대상의 status는 건드리지 않는다(ReviewCycle 클래스 주석 참고). */
    public void recordDecision(ReviewDecision decision, String comment) {
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = LocalDateTime.now();
    }
}
