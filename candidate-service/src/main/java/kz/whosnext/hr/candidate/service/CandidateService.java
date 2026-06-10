package kz.whosnext.hr.candidate.service;

import kz.whosnext.hr.candidate.exception.ResourceNotFoundException;
import kz.whosnext.hr.candidate.mapper.CandidateMapper;
import kz.whosnext.hr.candidate.model.dto.request.UpdateCandidateRequest;
import kz.whosnext.hr.candidate.model.dto.response.CandidateResponse;
import kz.whosnext.hr.candidate.model.entity.Candidate;
import kz.whosnext.hr.candidate.model.enums.ActivityAction;
import kz.whosnext.hr.candidate.model.enums.ActivitySource;
import kz.whosnext.hr.candidate.repository.ApplicationRepository;
import kz.whosnext.hr.candidate.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateMapper candidateMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public CandidateResponse getMyProfile(UUID userId) {
        return candidateMapper.toResponse(findByUserId(userId));
    }

    @Transactional
    public CandidateResponse updateProfile(UUID userId, UpdateCandidateRequest req) {
        Candidate c = findByUserId(userId);
        if (req.firstName() != null) c.setFirstName(req.firstName());
        if (req.lastName() != null) c.setLastName(req.lastName());
        if (req.birthDate() != null) c.setBirthDate(req.birthDate());
        if (req.iin() != null) c.setIin(req.iin());
        if (req.phone() != null) c.setPhone(req.phone());
        if (req.address() != null) c.setAddress(req.address());
        if (req.activityType() != null) c.setActivityType(req.activityType());
        Candidate saved = candidateRepository.save(c);

        activityLogProducer.log(
                ActivityAction.CANDIDATE_PROFILE_UPDATED, ActivitySource.CANDIDATE_SERVICE,
                userId, saved.getEmail(), "CANDIDATE",
                "Candidate", saved.getId().toString(),
                "Candidate profile updated");

        return candidateMapper.toResponse(saved);
    }

    @Transactional
    public void deleteByUserId(UUID userId, UUID actorId, String actorEmail) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль кандидата не найден"));
        applicationRepository.deleteAllByCandidate_Id(candidate.getId());
        candidateRepository.delete(candidate);

        activityLogProducer.log(
                ActivityAction.CANDIDATE_PROFILE_UPDATED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, "ADMIN",
                "Candidate", candidate.getId().toString(),
                "Candidate and all applications deleted by admin " + actorId);
    }

    public Candidate findByUserId(UUID userId) {
        return candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль кандидата не найден"));
    }
}

