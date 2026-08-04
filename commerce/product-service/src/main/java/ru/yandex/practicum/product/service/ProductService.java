package ru.yandex.practicum.product.service;

import ru.yandex.practicum.product.dto.ProductRequest;
import ru.yandex.practicum.product.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllActiveProducts();

    ProductResponse getProductById(Long id);

    List<ProductResponse> getProductsByCategory(Long categoryId);

    List<ProductResponse> searchProducts(String query);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);
}
