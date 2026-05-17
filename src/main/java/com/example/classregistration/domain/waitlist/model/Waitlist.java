package com.example.classregistration.domain.waitlist.model;

import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "waitlists",
        uniqueConstraints = @UniqueConstraint(columnNames = {"klassmate_id", "klass_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "klassmate_id", nullable = false)
    private Klassmate klassmate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "klass_id", nullable = false)
    private Klass klass;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Waitlist create(Klassmate klassmate, Klass klass) {
        Waitlist waitlist = new Waitlist();
        waitlist.klassmate = klassmate;
        waitlist.klass = klass;
        return waitlist;
    }
}
