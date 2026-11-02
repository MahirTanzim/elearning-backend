package com.example.elearningbackend.course;

import com.example.elearningbackend.course.CourseDtos.CategoryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categories;

    @GetMapping("/api/public/categories")
    public List<CategoryResponse> list() {
        return categories.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
    }
}