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
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .active(true)
                .category(category)
                .build();
    }

    public void updateEntity(Product product, ProductRequest request, Category category) {
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
        if (category != null) {
            product.setCategory(category);
        }
    }
}
