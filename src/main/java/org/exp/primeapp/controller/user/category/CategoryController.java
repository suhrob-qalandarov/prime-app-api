package org.exp.primeapp.controller.user.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.exp.primeapp.models.dto.responce.user.CategoryRes;
import org.exp.primeapp.service.face.user.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.exp.primeapp.utils.Const.*;

@Slf4j
@RestController
@RequestMapping(API + V1 + CATEGORY)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryRes>> getCategories() {
        List<CategoryRes> categories = categoryService.getResCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{spotlightName}")
    public ResponseEntity<List<CategoryRes>> getCategoriesBySpotlightName(
            @PathVariable String spotlightName) {
        List<CategoryRes> categories = categoryService.getResCategoriesBySpotlightName(spotlightName);
        return ResponseEntity.ok(categories);
    }
}
