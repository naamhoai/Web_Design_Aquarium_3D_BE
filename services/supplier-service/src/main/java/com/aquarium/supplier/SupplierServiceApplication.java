package com.aquarium.supplier;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.aquarium.supplier", "com.aquarium.common"})
@OpenAPIDefinition(
        info = @Info(
                title = "Aquarium Supplier & Warehouse Service API",
                version = "1.0.0",
                description = "Dịch vụ quản lý nhà cung cấp, hồ sơ showroom và hạ tầng kho nguồn đa điểm cho sàn TMĐT Thủy Sinh 3D"
        )
)
public class SupplierServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplierServiceApplication.class, args);
    }
}
