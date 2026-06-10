package kz.whosnext.hr.employeeservice.controller;

import jakarta.validation.Valid;
import kz.whosnext.hr.employeeservice.model.dto.request.NewsRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.MessageResponse;
import kz.whosnext.hr.employeeservice.model.dto.response.NewsResponse;
import kz.whosnext.hr.employeeservice.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public Page<NewsResponse> list(
            @RequestParam(defaultValue = "true") boolean publishedOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        return newsService.list(publishedOnly, pageable);
    }

    @GetMapping("/{id}")
    public NewsResponse getById(@PathVariable UUID id) {
        return newsService.getById(id);
    }

    @PostMapping
    public ResponseEntity<NewsResponse> create(@Valid @RequestBody NewsRequest req,
                                               @RequestHeader("X-User-Id") UUID authorId,
                                               @RequestHeader("X-User-Email") String email,
                                               @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.create(req, authorId, email, role));
    }

    @PutMapping("/{id}")
    public NewsResponse update(@PathVariable UUID id,
                               @Valid @RequestBody NewsRequest req,
                               @RequestHeader("X-User-Id") UUID userId,
                               @RequestHeader("X-User-Email") String email,
                               @RequestHeader("X-User-Role") String role) {
        return newsService.update(id, req, userId, email, role);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id,
                                                  @RequestHeader("X-User-Id") UUID userId,
                                                  @RequestHeader("X-User-Email") String email,
                                                  @RequestHeader("X-User-Role") String role) {
        newsService.delete(id, userId, email, role);
        return ResponseEntity.ok(new MessageResponse("Новость удалена"));
    }
}

