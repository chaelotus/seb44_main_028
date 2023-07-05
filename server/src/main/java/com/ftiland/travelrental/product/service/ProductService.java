package com.ftiland.travelrental.product.service;

import com.ftiland.travelrental.category.dto.CategoryDto;
import com.ftiland.travelrental.common.exception.BusinessLogicException;
import com.ftiland.travelrental.common.exception.ExceptionCode;
import com.ftiland.travelrental.member.entity.Member;
import com.ftiland.travelrental.member.repository.MemberRepository;
import com.ftiland.travelrental.product.dto.CreateProduct;
import com.ftiland.travelrental.product.dto.UpdateProduct;
import com.ftiland.travelrental.product.entity.Product;
import com.ftiland.travelrental.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ftiland.travelrental.common.exception.ExceptionCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ProductCategoryService productCategoryService;

    @Transactional
    public CreateProduct.Response createProduct(CreateProduct.Request request, String memberEmail) {
        log.info("[ProductService] createProduct called");
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new BusinessLogicException(MEMBER_NOT_FOUND));

        if (member.getLatitude() == null || member.getLongitude() == null) {
            throw new BusinessLogicException(NOT_FOUNT_LOCATION);
        }

        Product productEntity = Product.builder()
                .productId(UUID.randomUUID().toString())
                .title(request.getTitle())
                .content(request.getContent())
                .overdueFee(request.getOverdueFee())
                .baseFee(request.getBaseFee())
                .feePerDay(request.getFeePerDay())
                .minimumRentalPeriod(request.getMinimumRentalPeriod())
                .totalRateCount(0)
                .totalRateScore(0)
                .viewCount(0)
                .member(member).build();

        // save시에 id를 기준으로 insert쿼리나 update쿼리를 생성해야하기 때문에 select를 먼저 실행한다.
        Product product = productRepository.save(productEntity);

        List<CategoryDto> productCategories =
                productCategoryService.createProductCategories(product, request.getCategoryIds());

        return CreateProduct.Response.from(product, productCategories);
    }

    private void validateOwner(Member member, Product product) {
        if (!member.getMemberId().equals(product.getMember().getMemberId())) {
            throw new BusinessLogicException(ExceptionCode.UNAUTHORIZED);
        }
    }

    @Transactional
    public UpdateProduct.Response updateProduct(UpdateProduct.Request request,
                                                String productId, String memberEmail) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new BusinessLogicException(MEMBER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessLogicException(PRODUCT_NOT_FOUND));

        validateOwner(member, product);

        Optional.ofNullable(request.getBaseFee())
                .ifPresent(baseFee -> product.setBaseFee(baseFee));
        Optional.ofNullable(request.getTitle())
                .ifPresent(title -> product.setTitle(title));
        Optional.ofNullable(request.getContent())
                .ifPresent(content -> product.setContent(content));
        Optional.ofNullable(request.getFeePerDay())
                .ifPresent(feePerDay -> product.setFeePerDay(feePerDay));
        Optional.ofNullable(request.getOverdueFee())
                .ifPresent(overdueFee -> product.setOverdueFee(overdueFee));
        Optional.ofNullable(request.getMinimumRentalPeriod())
                .ifPresent(minimumRentalPeriod -> product.setMinimumRentalPeriod(minimumRentalPeriod));
        Optional.ofNullable(request.getCategoryIds())
                .ifPresent(categoryIds -> {
                    productCategoryService.deleteProductCategoriesByProductId(productId);
                    productCategoryService.createProductCategories(product, categoryIds);
                });

        return UpdateProduct.Response.from(product);
    }

    @Transactional
    public void deleteProduct(String productId, String memberEmail) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new BusinessLogicException(MEMBER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessLogicException(PRODUCT_NOT_FOUND));

        validateOwner(member, product);

        productRepository.delete(product);
    }
}