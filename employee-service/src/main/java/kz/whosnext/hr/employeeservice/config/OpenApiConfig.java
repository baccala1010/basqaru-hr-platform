package kz.whosnext.hr.employeeservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Service API")
                        .description("Карточка сотрудника, отделы, графики, табель, отпуска, посещаемость, новости, аналитика")
                        .version("1.0.0"));
    }
}

