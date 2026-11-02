package com.example.elearningbackend.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private int position;

    @Column(name = "free_preview", nullable = false)
    private boolean freePreview = false;

    @Column(length = 2000)
    private String description;

    @Column(name = "text_content", length = 20000)
    private String textContent;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}