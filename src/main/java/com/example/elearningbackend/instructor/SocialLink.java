package com.example.elearningbackend.instructor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instructor_social_links")
@Getter
@Setter
@NoArgsConstructor
public class SocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id")
    private InstructorProfile instructor;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String url;
}