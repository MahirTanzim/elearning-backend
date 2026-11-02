package com.example.elearningbackend.course;

import com.example.elearningbackend.course.CourseDtos.*;
import com.example.elearningbackend.instructor.InstructorProfile;
import com.example.elearningbackend.instructor.InstructorProfileRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final Set<String> EDITABLE = Set.of(Course.DRAFT, Course.REJECTED);
    private static final int MAX_COURSES = 50;
    private static final int MAX_SECTIONS = 30;
    private static final int MAX_LESSONS = 50;

    private final CourseRepository courses;
    private final SectionRepository sections;
    private final LessonRepository lessons;
    private final CategoryRepository categories;
    private final InstructorProfileRepository profiles;

    // ---------- courses ----------

    @Transactional(readOnly = true)
    public List<CourseSummary> listMine(Long userId) {
        requireInstructor(userId);
        return courses.findByInstructorUserIdOrderByUpdatedAtDesc(userId).stream()
            .map(CourseSummary::from)
            .toList();
    }

    @Transactional
    public CourseDetail create(Long userId, CourseRequest request) {
        InstructorProfile profile = requireInstructor(userId);
        if (courses.countByInstructorId(profile.getId()) >= MAX_COURSES) {
            throw bad("You can have at most " + MAX_COURSES + " courses");
        }

        Course course = new Course();
        course.setInstructor(profile);
        course.setSlug(uniqueSlug(request.title()));
        applyDetails(course, request);
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    @Transactional(readOnly = true)
    public CourseDetail getMine(Long userId, Long courseId) {
        return CourseDetail.from(ownedCourse(userId, courseId));
    }

    @Transactional
    public CourseDetail update(Long userId, Long courseId, CourseRequest request) {
        Course course = ownedCourse(userId, courseId);
        requireEditable(course);
        applyDetails(course, request);
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    @Transactional
    public void delete(Long userId, Long courseId) {
        Course course = ownedCourse(userId, courseId);
        requireEditable(course);
        courses.delete(course);
    }

    // ---------- sections ----------

    @Transactional
    public CourseDetail addSection(Long userId, Long courseId, SectionRequest request) {
        Course course = ownedCourse(userId, courseId);
        requireEditable(course);
        if (course.getSections().size() >= MAX_SECTIONS) {
            throw bad("A course can have at most " + MAX_SECTIONS + " sections");
        }

        Section section = new Section();
        section.setCourse(course);
        section.setTitle(request.title().trim());
        section.setPosition(nextPosition(course.getSections(), Section::getPosition));
        course.getSections().add(section);
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    @Transactional
    public CourseDetail renameSection(Long userId, Long sectionId, SectionRequest request) {
        Section section = ownedSection(userId, sectionId);
        Course course = section.getCourse();
        requireEditable(course);
        section.setTitle(request.title().trim());
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    @Transactional
    public CourseDetail deleteSection(Long userId, Long sectionId) {
        Section section = ownedSection(userId, sectionId);
        Course course = section.getCourse();
        requireEditable(course);

        course.getSections().removeIf(s -> s.getId().equals(sectionId));
        renumber(course.getSections(), Section::getPosition, Section::setPosition);
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    @Transactional
    public CourseDetail reorderSections(Long userId, Long courseId, OrderRequest request) {
        Course course = ownedCourse(userId, courseId);
        requireEditable(course);
        reorder(course.getSections(), request.ids(), Section::getId, Section::setPosition);
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    // ---------- lessons ----------

    @Transactional
    public CourseDetail addLesson(Long userId, Long sectionId, LessonCreateRequest request) {
        Section section = ownedSection(userId, sectionId);
        Course course = section.getCourse();
        requireEditable(course);
        if (section.getLessons().size() >= MAX_LESSONS) {
            throw bad("A section can have at most " + MAX_LESSONS + " lessons");
        }

        Lesson lesson = new Lesson();
        lesson.setSection(section);
        lesson.setTitle(request.title().trim());
        lesson.setType(request.type());
        lesson.setFreePreview(request.freePreview());
        lesson.setDescription(clean(request.description()));
        lesson.setTextContent("TEXT".equals(request.type()) ? clean(request.textContent()) : null);
        lesson.setPosition(nextPosition(section.getLessons(), Lesson::getPosition));
        section.getLessons().add(lesson);
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    @Transactional
    public CourseDetail updateLesson(Long userId, Long lessonId, LessonUpdateRequest request) {
        Lesson lesson = ownedLesson(userId, lessonId);
        Course course = lesson.getSection().getCourse();
        requireEditable(course);

        lesson.setTitle(request.title().trim());
        lesson.setFreePreview(request.freePreview());
        lesson.setDescription(clean(request.description()));
        if ("TEXT".equals(lesson.getType())) {
            lesson.setTextContent(clean(request.textContent()));
        }
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    @Transactional
    public CourseDetail deleteLesson(Long userId, Long lessonId) {
        Lesson lesson = ownedLesson(userId, lessonId);
        Section section = lesson.getSection();
        Course course = section.getCourse();
        requireEditable(course);

        section.getLessons().removeIf(l -> l.getId().equals(lessonId));
        renumber(section.getLessons(), Lesson::getPosition, Lesson::setPosition);
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    @Transactional
    public CourseDetail reorderLessons(Long userId, Long sectionId, OrderRequest request) {
        Section section = ownedSection(userId, sectionId);
        Course course = section.getCourse();
        requireEditable(course);
        reorder(section.getLessons(), request.ids(), Lesson::getId, Lesson::setPosition);
        return CourseDetail.from(courses.saveAndFlush(course));
    }

    // ---------- ownership ----------

    /** Only approved, active instructors may use the builder. Checked against the database, not just the token. */
    private InstructorProfile requireInstructor(Long userId) {
        InstructorProfile profile = profiles.findByUserId(userId)
            .orElseThrow(this::notInstructor);
        if (!InstructorProfile.APPROVED.equals(profile.getApplicationStatus())
                || !"ACTIVE".equals(profile.getUser().getStatus())) {
            throw notInstructor();
        }
        return profile;
    }

    private Course ownedCourse(Long userId, Long courseId) {
        requireInstructor(userId);
        return courses.findByIdAndInstructorUserId(courseId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private Section ownedSection(Long userId, Long sectionId) {
        requireInstructor(userId);
        Section section = sections.findById(sectionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        if (!section.getCourse().getInstructor().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found");
        }
        return section;
    }

    private Lesson ownedLesson(Long userId, Long lessonId) {
        requireInstructor(userId);
        Lesson lesson = lessons.findById(lessonId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));
        if (!lesson.getSection().getCourse().getInstructor().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found");
        }
        return lesson;
    }

    private void requireEditable(Course course) {
        if (!EDITABLE.contains(course.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This course cannot be changed while it is " + course.getStatus());
        }
    }

    // ---------- helpers ----------

    private void applyDetails(Course course, CourseRequest r) {
        Category category = categories.findById(r.categoryId())
            .orElseThrow(() -> bad("Unknown category"));

        course.setCategory(category);
        course.setTitle(r.title().trim());
        course.setSubtitle(clean(r.subtitle()));
        course.setDescription(clean(r.description()));
        course.setLevel(r.level());
        course.setLanguage(r.language().trim());
        course.setPrice(r.price().setScale(2));
        course.setWhatYouLearn(clean(r.whatYouLearn()));
        course.setRequirements(clean(r.requirements()));
    }

    private String uniqueSlug(String title) {
        String base = title.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        if (base.length() > 100) base = base.substring(0, 100).replaceAll("-+$", "");
        if (base.isEmpty()) base = "course";   // e.g. a title written only in Bengali

        String slug = base;
        while (courses.existsBySlug(slug)) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return slug;
    }

    private <T> int nextPosition(List<T> items, ToIntFunction<T> positionOf) {
        return items.stream().mapToInt(positionOf).max().orElse(-1) + 1;
    }

    /** Makes positions 0, 1, 2... again, keeping the current order. */
    private <T> void renumber(List<T> items, ToIntFunction<T> positionOf, BiConsumer<T, Integer> setPosition) {
        List<T> ordered = items.stream().sorted(Comparator.comparingInt(positionOf)).toList();
        for (int i = 0; i < ordered.size(); i++) {
            setPosition.accept(ordered.get(i), i);
        }
    }

    /** The list must contain exactly the current ids, each once, in the wanted order. */
    private <T> void reorder(List<T> items, List<Long> ids, Function<T, Long> idOf, BiConsumer<T, Integer> setPosition) {
        Set<Long> current = items.stream().map(idOf).collect(Collectors.toSet());
        if (ids.size() != items.size() || !current.equals(new HashSet<>(ids))) {
            throw bad("The list must contain every item exactly once");
        }
        Map<Long, T> byId = items.stream().collect(Collectors.toMap(idOf, Function.identity()));
        for (int i = 0; i < ids.size(); i++) {
            setPosition.accept(byId.get(ids.get(i)), i);
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ResponseStatusException notInstructor() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Instructor access required");
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}