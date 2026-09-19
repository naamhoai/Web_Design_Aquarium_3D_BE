package com.aquarium.inventory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.aquarium.inventory", "com.aquarium.common"})
@OpenAPIDefinition(
        info = @Info(
                title = "Aquarium Multi-Warehouse & Livestock Inventory Service API",
                version = "1.0.0",
                description = "Dịch vụ quản lý tồn kho đa showroom, cơ chế giữ/xuất kho, kiểm dịch cá cảnh (Quarantine) và tỷ lệ hao hụt sinh học"
        )
)
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
