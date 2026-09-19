package com.aquarium.catalog;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.aquarium.catalog", "com.aquarium.common"})
@OpenAPIDefinition(
        info = @Info(
                title = "Aquarium Catalog Service API",
                version = "1.0.0",
                description = "Dịch vụ quản lý danh mục sản phẩm, biến thể linh kiện bể cá, phân trang và tìm kiếm cho sàn TMĐT 3D"
        )
)
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
