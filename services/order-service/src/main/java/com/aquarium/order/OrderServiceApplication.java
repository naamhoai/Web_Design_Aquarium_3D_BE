package com.aquarium.order;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.aquarium.order", "com.aquarium.common"})
@OpenAPIDefinition(
        info = @Info(
                title = "Aquarium Smart Cart & Multi-Vendor Order Service API",
                version = "1.0.0",
                description = "Dịch vụ giỏ hàng 3D, thanh toán, tự động bóc tách đơn hàng con theo nhà cung cấp (Sub-Orders Splitting) và theo dõi tiến độ giao hàng"
        )
)
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
