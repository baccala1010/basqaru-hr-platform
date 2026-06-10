package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.mapper.NewsMapper;
import kz.whosnext.hr.employeeservice.model.dto.request.NewsRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.NewsResponse;
import kz.whosnext.hr.employeeservice.model.entity.News;
import kz.whosnext.hr.employeeservice.repository.NewsRepository;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public Page<NewsResponse> list(boolean publishedOnly, Pageable pageable) {
        if (publishedOnly) return newsRepository.findByPublishedTrue(pageable).map(newsMapper::toResponse);
        return newsRepository.findAll(pageable).map(newsMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NewsResponse getById(UUID id) {
        return newsMapper.toResponse(findById(id));
    }

    @Transactional
    public NewsResponse create(NewsRequest req, UUID authorId, String actorEmail, String actorRole) {
        News news = News.builder()
                .title(req.title())
                .content(req.content())
                .imageUrl(req.imageUrl())
                .authorId(authorId)
                .published(req.published())
                .publishedAt(req.published() ? LocalDateTime.now() : null)
                .build();
        news = newsRepository.save(news);
        log.info("Новость создана: id={}", news.getId());

        activityLogProducer.log(
                ActivityAction.NEWS_CREATED, ActivitySource.EMPLOYEE_SERVICE,
                authorId, actorEmail, actorRole,
                "News", news.getId().toString(),
                "News created: " + news.getTitle());

        return newsMapper.toResponse(news);
    }

    @Transactional
    public NewsResponse update(UUID id, NewsRequest req, UUID actorId, String actorEmail, String actorRole) {
        News news = findById(id);
        news.setTitle(req.title());
        news.setContent(req.content());
        news.setImageUrl(req.imageUrl());
        if (req.published() && !news.getPublished()) {
            news.setPublishedAt(LocalDateTime.now());
        }
        news.setPublished(req.published());
        news = newsRepository.save(news);

        activityLogProducer.log(
                ActivityAction.NEWS_UPDATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "News", id.toString(),
                "News updated: " + news.getTitle());

        return newsMapper.toResponse(news);
    }

    @Transactional
    public void delete(UUID id, UUID actorId, String actorEmail, String actorRole) {
        News news = findById(id);

        activityLogProducer.log(
                ActivityAction.NEWS_DELETED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "News", id.toString(),
                "News deleted: " + news.getTitle());

        newsRepository.delete(news);
        log.info("Новость удалена: id={}", id);
    }

    public News findById(UUID id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Новость не найдена: " + id));
    }
}

