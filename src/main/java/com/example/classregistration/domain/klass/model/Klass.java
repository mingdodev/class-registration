package com.example.classregistration.domain.klass.model;

import com.example.classregistration.domain.creator.model.Creator;
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
}
