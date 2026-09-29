package com.aquarium.identity;

import com.aquarium.identity.config.AuthCookieProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AuthCookieProperties.class)
@ComponentScan(basePackages = {"com.aquarium.identity", "com.aquarium.common"})
@OpenAPIDefinition(
        info = @Info(
                title = "Aquarium Identity Service API",
                version = "1.0.0",
                description = "Dịch vụ xác thực, quản lý người dùng, phân quyền và sổ địa chỉ cho nền tảng Bể Cá Thủy Sinh 3D"
        )
)
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
