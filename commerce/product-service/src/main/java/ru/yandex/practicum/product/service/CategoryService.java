package ru.yandex.practicum.product.service;

import ru.yandex.practicum.product.dto.CategoryRequest;
import ru.yandex.practicum.product.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);
}
