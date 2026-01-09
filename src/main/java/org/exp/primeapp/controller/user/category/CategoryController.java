package org.exp.primeapp.controller.user.category;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.exp.primeapp.models.dto.responce.user.CategoryRes;
import org.exp.primeapp.models.enums.CategoryStatus;
import org.exp.primeapp.service.face.user.CategoryService;
import org.exp.primeapp.utils.SessionTokenUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.exp.primeapp.utils.Const.*;

@RestController
@RequestMapping(API + V1 + CATEGORY)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final SessionTokenUtil sessionTokenUtil;

    @GetMapping
    public ResponseEntity<?> getCategories(
            HttpServletRequest request,
            HttpServletResponse response) {
        return sessionTokenUtil.handleSessionTokenRequest("category", request, response, () -> {
            List<CategoryRes> categories = categoryService.getResCategories();
            return ResponseEntity.ok(categories);
        });
    }

    @GetMapping("/{spotlightName}")
    public ResponseEntity<?> getCategoriesBySpotlightName(
            @PathVariable String spotlightName,
            HttpServletRequest request,
            HttpServletResponse response) {
        return sessionTokenUtil.handleSessionTokenRequest("category", request, response, () -> {
            List<CategoryRes> categories = categoryService.getResCategoriesBySpotlightName(spotlightName);
            return ResponseEntity.ok(categories);
        });
    }

    /*@GetMapping("/by-spotlight/{spotlightId}")
    public ResponseEntity <List<CategoryRes>> getCategoriesBySpotlightId(@PathVariable Long spotlightId) {
        List<CategoryRes> categories = categoryService.getSpotlightCategories(spotlightId);
        return new ResponseEntity<>(categories, HttpStatus.OK);
    }*/
}
