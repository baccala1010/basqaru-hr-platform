package kz.whosnext.hr.candidate.service;

import jakarta.persistence.criteria.Predicate;
import kz.whosnext.hr.candidate.exception.AccessDeniedException;
import kz.whosnext.hr.candidate.exception.ResourceNotFoundException;
import kz.whosnext.hr.candidate.mapper.VacancyMapper;
import kz.whosnext.hr.candidate.model.dto.request.VacancyRequest;
import kz.whosnext.hr.candidate.model.dto.response.VacancyResponse;
import kz.whosnext.hr.candidate.model.entity.Vacancy;
import kz.whosnext.hr.candidate.model.enums.*;
import kz.whosnext.hr.candidate.repository.ContractTypeRepository;
import kz.whosnext.hr.candidate.repository.PositionRepository;
import kz.whosnext.hr.candidate.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyService {

    private final VacancyRepository vacancyRepository;
    private final PositionRepository positionRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final VacancyMapper vacancyMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public Page<VacancyResponse> search(String keyword, LocationType location, EmploymentType employmentType,
                                         ExperienceLevel experienceLevel, VacancyCategory category,
                                         BigDecimal salaryMin, BigDecimal salaryMax,
                                         Integer daysAgo, Boolean isActive, boolean includeInactive, Pageable pageable) {

        Specification<Vacancy> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isActive != null) {
                predicates.add(cb.equal(root.get("active"), isActive));
            } else if (!includeInactive) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("skills")), pattern),
                        cb.like(cb.lower(root.get("company")), pattern)
                ));
            }
            if (location != null) predicates.add(cb.equal(root.get("location"), location));
            if (employmentType != null) predicates.add(cb.equal(root.get("employmentType"), employmentType));
            if (experienceLevel != null) predicates.add(cb.equal(root.get("experienceLevel"), experienceLevel));
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            if (salaryMin != null) predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), salaryMin));
            if (salaryMax != null) predicates.add(cb.lessThanOrEqualTo(root.get("salaryMin"), salaryMax));
            if (daysAgo != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), LocalDateTime.now().minusDays(daysAgo)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return vacancyRepository.findAll(spec, pageable).map(vacancyMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VacancyResponse getById(UUID id) {
        return vacancyMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public VacancyResponse getByIdForCandidate(UUID id) {
        Vacancy vacancy = findOrThrow(id);
        if (!Boolean.TRUE.equals(vacancy.getActive())) {
            throw new AccessDeniedException("Вакансия не активна");
        }
        return vacancyMapper.toResponse(vacancy);
    }

    @Transactional
    public VacancyResponse create(VacancyRequest req, UUID createdBy) {
        Vacancy v = Vacancy.builder()
                .title(req.title()).company(req.company()).companyLogoUrl(req.companyLogoUrl())
                .location(req.location()).employmentType(req.employmentType())
                .experienceLevel(req.experienceLevel()).category(req.category())
                .description(req.description()).shortDescription(req.shortDescription())
                .skills(req.skills()).salaryMin(req.salaryMin()).salaryMax(req.salaryMax())
                .requiresCriminalRecord(req.requiresCriminalRecord() != null && req.requiresCriminalRecord())
                .requiresMedicalCert(req.requiresMedicalCert() != null && req.requiresMedicalCert())
                .createdBy(createdBy).build();
        if (req.positionId() != null) v.setPosition(positionRepository.findById(req.positionId()).orElse(null));
        if (req.contractTypeId() != null) v.setContractType(contractTypeRepository.findById(req.contractTypeId()).orElse(null));
        Vacancy saved = vacancyRepository.save(v);

        activityLogProducer.log(
                ActivityAction.VACANCY_CREATED, ActivitySource.CANDIDATE_SERVICE,
                createdBy, "", "",
                "Vacancy", saved.getId().toString(),
                "Vacancy created: " + saved.getTitle());

        return vacancyMapper.toResponse(saved);
    }

    @Transactional
    public VacancyResponse update(UUID id, VacancyRequest req, UUID actorId, String actorEmail, String actorRole) {
        Vacancy v = findOrThrow(id);
        v.setTitle(req.title()); v.setCompany(req.company()); v.setCompanyLogoUrl(req.companyLogoUrl());
        v.setLocation(req.location()); v.setEmploymentType(req.employmentType());
        v.setExperienceLevel(req.experienceLevel()); v.setCategory(req.category());
        v.setDescription(req.description()); v.setShortDescription(req.shortDescription());
        v.setSkills(req.skills()); v.setSalaryMin(req.salaryMin()); v.setSalaryMax(req.salaryMax());
        if (req.positionId() != null) v.setPosition(positionRepository.findById(req.positionId()).orElse(null));
        if (req.contractTypeId() != null) v.setContractType(contractTypeRepository.findById(req.contractTypeId()).orElse(null));
        v.setRequiresCriminalRecord(req.requiresCriminalRecord() != null && req.requiresCriminalRecord());
        v.setRequiresMedicalCert(req.requiresMedicalCert() != null && req.requiresMedicalCert());
        Vacancy saved = vacancyRepository.save(v);

        activityLogProducer.log(
                ActivityAction.VACANCY_UPDATED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "Vacancy", saved.getId().toString(),
                "Vacancy updated: " + saved.getTitle());

        return vacancyMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Vacancy v = findOrThrow(id);
        v.setActive(false);
        vacancyRepository.save(v);
    }

    @Transactional
    public VacancyResponse deactivate(UUID id, UUID actorId, String actorEmail, String actorRole) {
        Vacancy vacancy = findOrThrow(id);
        if (!Boolean.TRUE.equals(vacancy.getActive())) {
            throw new kz.whosnext.hr.candidate.exception.BadRequestException("Вакансия уже деактивирована");
        }
        vacancy.setActive(false);
        Vacancy saved = vacancyRepository.save(vacancy);

        activityLogProducer.log(
                ActivityAction.VACANCY_DEACTIVATED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "Vacancy", saved.getId().toString(),
                "Vacancy deactivated: " + saved.getTitle());

        return vacancyMapper.toResponse(saved);
    }

    @Transactional
    public VacancyResponse activate(UUID id, UUID actorId, String actorEmail, String actorRole) {
        Vacancy vacancy = findOrThrow(id);
        vacancy.setActive(true);
        Vacancy saved = vacancyRepository.save(vacancy);

        activityLogProducer.log(
                ActivityAction.VACANCY_ACTIVATED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "Vacancy", saved.getId().toString(),
                "Vacancy activated: " + saved.getTitle());

        return vacancyMapper.toResponse(saved);
    }

    private Vacancy findOrThrow(UUID id) {
        return vacancyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Вакансия не найдена"));
    }
}

