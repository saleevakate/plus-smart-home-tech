package ru.yandex.practicum.product.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.product.dto.CategoryResponse;
import ru.yandex.practicum.product.dto.ProductRequest;
import ru.yandex.practicum.product.dto.ProductResponse;
import ru.yandex.practicum.product.model.Category;
import ru.yandex.practicum.product.model.Product;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        CategoryResponse categoryResponse = new CategoryResponse(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription()
        );

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getActive(),
                categoryResponse
        );
    }

    public Product toEntity(ProductRequest request, Category category) {
        if (request == null) {
            return null;
        }

        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .active(true)
                .category(category)
                .build();
    }

    public void updateEntity(Product product, ProductRequest request, Category category) {
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (category != null) {
            product.setCategory(category);
        }
    }
}
