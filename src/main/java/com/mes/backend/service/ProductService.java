// 제품(Product) 마스터 CRUD
package com.mes.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.ProductCreateRequest;
import com.mes.backend.dto.ProductUpdateRequest;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.Product;
import com.mes.backend.repository.BomRepository;
import com.mes.backend.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final BomRepository bomRepo;

    public List<Product> search(String productName, LocalDate createdFrom, LocalDate createdTo) {
        LocalDateTime from = createdFrom != null ? createdFrom.atStartOfDay() : null;
        LocalDateTime toExclusive = createdTo != null ? createdTo.plusDays(1).atStartOfDay() : null;
        List<Product> products = productRepo.search(productName, from, toExclusive);
        products.forEach(this::populateBomId);
        return products;
    }

    public Product getById(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("제품을 찾을 수 없습니다. ID: " + id));
        populateBomId(product);
        return product;
    }

    private void populateBomId(Product product) {
        bomRepo.findByProduct_Id(product.getId()).ifPresent(bom -> product.setBomId(bom.getId()));
    }

    /* 등록 성공 시 연결된 빈 BOM 1행도 같은 트랜잭션에서 생성 */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Product create(ProductCreateRequest request) {
        Product product = productRepo.save(Product.builder()
                .productCode(request.getProductCode())
                .productName(request.getProductName())
                .voltage(request.getVoltage())
                .capacity(request.getCapacity())
                .unit(request.getUnit())
                .build());
        bomRepo.save(Bom.builder().product(product).build());
        return product;
    }

    /* product_code는 잠금 - 여기서 건드리지 않음 */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Product update(Long id, ProductUpdateRequest request) {
        Product product = getById(id);
        product.setProductName(request.getProductName());
        product.setVoltage(request.getVoltage());
        product.setCapacity(request.getCapacity());
        product.setUnit(request.getUnit());
        return productRepo.save(product);
    }

    /* 실제 삭제 대신 is_active=false로 soft delete */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public void delete(Long id) {
        Product product = getById(id);
        product.setActive(false);
        productRepo.save(product);
    }
}
