package com.lightalm.formlayout.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * ADR-012 §B.2. 폼 레이아웃 안의 그룹(예: "기본 정보", "일정"). {@link FormLayout}의
 * 애그리게잇 안에서만 생성/삭제되며(전체 치환), 독립적으로 Repository를 두지 않는다.
 */
@Entity
@Table(name = "form_layout_sections")
@Getter
public class FormLayoutSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_layout_id", nullable = false)
    private FormLayout formLayout;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private final List<FormLayoutField> fields = new ArrayList<>();

    protected FormLayoutSection() {
        // JPA
    }

    @Builder
    private FormLayoutSection(String title, Integer displayOrder) {
        this.title = title;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    void assignFormLayout(FormLayout formLayout) {
        this.formLayout = formLayout;
    }

    public void addField(FormLayoutField field) {
        field.assignSection(this);
        this.fields.add(field);
    }
}
