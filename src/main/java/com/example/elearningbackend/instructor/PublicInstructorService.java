package com.example.elearningbackend.instructor;

import com.example.elearningbackend.instructor.PublicInstructorDtos.*;
import com.example.elearningbackend.user.AvatarUrls;
import com.example.elearningbackend.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PublicInstructorService {

    private final InstructorProfileRepository profiles;

    @Transactional(readOnly = true)
    public List<InstructorCard> list() {
        return profiles
            .findTop50ByApplicationStatusAndUserStatusOrderByReviewedAtDesc(InstructorProfile.APPROVED, "ACTIVE")
            .stream()
            .map(p -> {
                User u = p.getUser();
                return new InstructorCard(p.getId(), u.getFullName(), AvatarUrls.of(u),
                    p.getHeadline(), p.getExpertise());
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public InstructorPage get(Long id) {
        InstructorProfile p = profiles
            .findByIdAndApplicationStatusAndUserStatus(id, InstructorProfile.APPROVED, "ACTIVE")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found"));

        User u = p.getUser();
        return new InstructorPage(
            p.getId(),
            u.getFullName(),
            AvatarUrls.of(u),
            p.getHeadline(),
            p.getBio(),
            p.getExpertise(),
            p.getQualifications().stream()
                .filter(Qualification::isVerified)              // unverified claims are never shown
                .map(q -> new PublicQualification(q.getType(), q.getTitle(), q.getInstitution(),
                    q.getFieldOfStudy(), q.getStartYear(), q.getEndYear()))
                .toList(),
            p.getSocialLinks().stream()
                .map(l -> new PublicSocialLink(l.getPlatform(), l.getUrl()))
                .toList()
        );
    }
}