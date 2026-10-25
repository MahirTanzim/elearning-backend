package com.example.elearningbackend.instructor;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instructor_qualifications")
@Getter
@Setter
@NoArgsConstructor
public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id")
    private InstructorProfile instructor;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String institution;

    @Column(name = "field_of_study")
    private String fieldOfStudy;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private boolean verified = false;
}