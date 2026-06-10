package kz.whosnext.hr.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Value("${AUTH_SERVICE_HOST:localhost}")
    private String authHost;
    @Value("${AUTH_SERVICE_PORT:8081}")
    private int authPort;

    @Value("${CANDIDATE_SERVICE_HOST:localhost}")
    private String candidateHost;
    @Value("${CANDIDATE_SERVICE_PORT:8082}")
    private int candidatePort;

    @Value("${DOCUMENT_SERVICE_HOST:localhost}")
    private String documentHost;
    @Value("${DOCUMENT_SERVICE_PORT:8083}")
    private int documentPort;

    @Value("${EMPLOYEE_SERVICE_HOST:localhost}")
    private String employeeHost;
    @Value("${EMPLOYEE_SERVICE_PORT:8084}")
    private int employeePort;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/api/v1/users/**", "/api/v1/activity-logs/**")
                        .uri("http://" + authHost + ":" + authPort))
                .route("candidate-service", r -> r
                        .path("/api/v1/vacancies/**", "/api/v1/applications/**",
                                "/api/v1/candidates/**", "/api/v1/dictionaries/**",
                                "/api/v1/candidate-notifications/**")
                        .uri("http://" + candidateHost + ":" + candidatePort))
                .route("document-service", r -> r
                        .path("/api/v1/documents/**", "/api/v1/contracts/**",
                                "/api/v1/certificates/**", "/api/v1/templates/**")
                        .uri("http://" + documentHost + ":" + documentPort))
                .route("employee-service", r -> r
                        .path("/api/v1/people/**", "/api/v1/employees/**",
                                "/api/v1/leaves/**", "/api/v1/attendance/**",
                                "/api/v1/news/**", "/api/v1/analytics/**",
                                "/api/v1/notifications/**", "/api/v1/departments/**",
                                "/api/v1/positions/**", "/api/v1/document-requests/**",
                                "/api/v1/schedules/**", "/api/v1/time-entries/**",
                                "/api/v1/payroll/**")
                        .uri("http://" + employeeHost + ":" + employeePort))
                .route("employee-service-ws", r -> r
                        .path("/ws/**")
                        .uri("ws://" + employeeHost + ":" + employeePort))
                .build();
    }
}

