package com.aquarium.aquarium3d.service.impl;

import com.aquarium.aquarium3d.dto.BomLayerDetailResponse;
import com.aquarium.aquarium3d.dto.ExplodedBomResponse;
import com.aquarium.aquarium3d.service.BomService;
import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BomServiceImpl implements BomService {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public ExplodedBomResponse getExplodedBom(UUID parentProductId) {
        String parentSql = "SELECT id, name, sku, base_price FROM products WHERE id = :productId";
        Query parentQuery = entityManager.createNativeQuery(parentSql);
        parentQuery.setParameter("productId", parentProductId);

        List<?> parentResults = parentQuery.getResultList();
        if (parentResults.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm combo bể cá");
        }

        Object[] parentRow = (Object[]) parentResults.get(0);
        UUID comboId = (UUID) parentRow[0];
        String comboName = (String) parentRow[1];
        String comboSku = (String) parentRow[2];
        BigDecimal basePrice = (BigDecimal) parentRow[3];

        List<BomLayerDetailResponse> layers = queryBomLayers(comboId);

        return ExplodedBomResponse.builder()
                .comboProductId(comboId)
                .comboName(comboName)
                .comboSku(comboSku)
                .totalPrice(basePrice)
                .layers(layers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExplodedBomResponse getExplodedBomBySku(String sku) {
        String parentSql = "SELECT id, name, sku, base_price FROM products WHERE sku = :sku";
        Query parentQuery = entityManager.createNativeQuery(parentSql);
        parentQuery.setParameter("sku", sku);

        List<?> parentResults = parentQuery.getResultList();
        if (parentResults.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm combo bể cá với mã SKU: " + sku);
        }

        Object[] parentRow = (Object[]) parentResults.get(0);
        UUID comboId = (UUID) parentRow[0];
        String comboName = (String) parentRow[1];
        String comboSku = (String) parentRow[2];
        BigDecimal basePrice = (BigDecimal) parentRow[3];

        List<BomLayerDetailResponse> layers = queryBomLayers(comboId);

        return ExplodedBomResponse.builder()
                .comboProductId(comboId)
                .comboName(comboName)
                .comboSku(comboSku)
                .totalPrice(basePrice)
                .layers(layers)
                .build();
    }

    private List<BomLayerDetailResponse> queryBomLayers(UUID parentProductId) {
        String sql = """
            SELECT 
                b.layer_index,
                b.layer_name,
                pv.id AS variant_id,
                pv.name AS variant_name,
                pv.sku,
                pv.price,
                b.is_required,
                b.can_swap,
                a.file_url AS asset_url,
                CAST(a.bounding_box AS text) AS bounding_box
            FROM product_boms b
            JOIN product_variants pv ON b.component_variant_id = pv.id
            LEFT JOIN assets_3d a ON a.product_variant_id = pv.id
            WHERE b.parent_product_id = :parentId
            ORDER BY b.layer_index ASC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("parentId", parentProductId);

        List<?> results = query.getResultList();
        List<BomLayerDetailResponse> layers = new ArrayList<>();

        for (Object item : results) {
            Object[] row = (Object[]) item;
            layers.add(BomLayerDetailResponse.builder()
                    .layerIndex(((Number) row[0]).intValue())
                    .layerName((String) row[1])
                    .componentVariantId((UUID) row[2])
                    .componentName((String) row[3])
                    .sku((String) row[4])
                    .price((BigDecimal) row[5])
                    .isRequired((Boolean) row[6])
                    .canSwap((Boolean) row[7])
                    .asset3dUrl((String) row[8])
                    .boundingBox((String) row[9])
                    .build());
        }

        return layers;
    }
}
