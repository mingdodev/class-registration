package com.example.classregistration.domain.klass.model;

import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.global.exception.BusinessException;
import com.example.classregistration.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "klasses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Klass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Column(nullable = false, length = 20)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KlassStatus status;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    private int remainingCapacity;

    private LocalDate startDate;
    private LocalDate endDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static Klass create(Creator creator, String title, String description, int price,
                                int maxCapacity, LocalDate startDate, LocalDate endDate) {
        if (title.length() > 20) throw new BusinessException(ErrorCode.KLASS_TITLE_TOO_LONG);
        if (maxCapacity < 1) throw new BusinessException(ErrorCode.KLASS_CAPACITY_INVALID);
        Klass klass = new Klass();
        klass.creator = creator;
        klass.title = title;
        klass.description = description;
        klass.price = price;
        klass.status = KlassStatus.DRAFT;
        klass.maxCapacity = maxCapacity;
        klass.remainingCapacity = maxCapacity;
        klass.startDate = startDate;
        klass.endDate = endDate;
        return klass;
    }

    public boolean isOwnedBy(Long creatorId) {
        return this.creator.getId().equals(creatorId);
    }

    public void open() {
        if (status != KlassStatus.DRAFT) throw new BusinessException(ErrorCode.KLASS_NOT_DRAFT);
        this.status = KlassStatus.OPEN;
    }

    public void update(String title, String description, Integer price, Integer maxCapacity,
                       LocalDate startDate, LocalDate endDate) {
        if (title != null) {
            if (title.length() > 20) throw new BusinessException(ErrorCode.KLASS_TITLE_TOO_LONG);
            this.title = title;
        }
        if (description != null) this.description = description;
        if (price != null) {
            if (status != KlassStatus.DRAFT) throw new BusinessException(ErrorCode.KLASS_PRICE_UPDATE_NOT_ALLOWED);
            this.price = price;
        }
        if (maxCapacity != null) {
            if (maxCapacity < 1) throw new BusinessException(ErrorCode.KLASS_CAPACITY_INVALID);
            if (status != KlassStatus.DRAFT && maxCapacity < this.maxCapacity) {
                throw new BusinessException(ErrorCode.KLASS_CAPACITY_DECREASE_NOT_ALLOWED);
            }
            // 실제 DB 반영은 KlassService에서 원자적 쿼리로 처리
        }
        if (startDate != null || endDate != null) {
            if (status != KlassStatus.DRAFT) throw new BusinessException(ErrorCode.KLASS_PERIOD_UPDATE_NOT_ALLOWED);
            if (startDate != null) this.startDate = startDate;
            if (endDate != null) this.endDate = endDate;
        }
    }

    public void validateDeletable() {
        if (status != KlassStatus.DRAFT) throw new BusinessException(ErrorCode.KLASS_NOT_DELETABLE);
    }

    public void validateEnrollable() {
        if (status != KlassStatus.OPEN) throw new BusinessException(ErrorCode.KLASS_NOT_OPEN);
        if (endDate != null && LocalDate.now().isAfter(endDate)) throw new BusinessException(ErrorCode.KLASS_PERIOD_ENDED);
    }

    public void validateWaitlistJoinable() {
        if (status != KlassStatus.CLOSED || (endDate != null && LocalDate.now().isAfter(endDate))) {
            throw new BusinessException(ErrorCode.WAITLIST_NOT_AVAILABLE);
        }
    }

    public boolean isPeriodEnded() {
        return endDate != null && LocalDate.now().isAfter(endDate);
    }

    public void close() {
        this.status = KlassStatus.CLOSED;
    }

    public void reopen() {
        this.status = KlassStatus.OPEN;
    }
}
