package kz.whosnext.hr.employeeservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "candidate-service", url = "${app.feign.candidate-service-url}")
public interface CandidateFeignClient {

    @GetMapping("/api/v1/applications/{id}/details")
    Map<String, Object> getApplicationDetails(@PathVariable("id") UUID applicationId);
}
