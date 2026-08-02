package com.lightalm.repository;

import com.lightalm.domain.Comment;
import com.lightalm.domain.TargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(TargetType targetType, Long targetId);
}
