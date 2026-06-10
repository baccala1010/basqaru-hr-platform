package kz.whosnext.hr.employeeservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "auth-service", url = "${app.feign.auth-service-url}")
public interface AuthFeignClient {

    @PatchMapping("/api/v1/users/{userId}/role")
    void updateUserRole(@PathVariable UUID userId, @RequestParam String role);

    @GetMapping("/api/v1/users/{id}")
    Map<String, Object> getUserById(@PathVariable UUID id);
}
