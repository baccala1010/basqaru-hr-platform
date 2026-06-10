package kz.whosnext.hr.candidate.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@FeignClient(name = "document-service", url = "${app.feign.document-service-url}")
public interface DocumentFeignClient {

    @PostMapping(value = "/api/v1/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> uploadFile(@RequestPart("file") MultipartFile file, @RequestPart("type") String type);
}

