package com.aquarium.aquarium3d;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.aquarium.aquarium3d", "com.aquarium.common"})
@OpenAPIDefinition(
        info = @Info(
                title = "Aquarium 3D Service API",
                version = "1.0.0",
                description = "Lõi tính toán mô phỏng 3D: Cấu trúc bóc tách 6 tầng linh kiện (BOM), Quy chuẩn sinh học cá cảnh và lưu trữ phối cảnh 3D"
        )
)
public class Aquarium3dServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Aquarium3dServiceApplication.class, args);
    }
}
