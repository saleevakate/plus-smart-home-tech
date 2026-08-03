package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.ProductRequest;
import ru.yandex.practicum.product.dto.ProductResponse;
import ru.yandex.practicum.product.exception.CategoryNotFoundException;
import ru.yandex.practicum.product.exception.ProductNotFoundException;
import ru.yandex.practicum.product.mapper.ProductMapper;
import ru.yandex.practicum.product.model.Category;
import ru.yandex.practicum.product.model.Product;
import ru.yandex.practicum.product.repository.CategoryRepository;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Товар не найден"));
        return productMapper.toResponse(product);
    }

    @Override
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Категория не найдена"));

        Product product = productMapper.toEntity(request, category);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Товар не найден"));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));
        }

        productMapper.updateEntity(product, request, category);
        Product updated = productRepository.save(product);
        return productMapper.toResponse(updated);
    }
}
