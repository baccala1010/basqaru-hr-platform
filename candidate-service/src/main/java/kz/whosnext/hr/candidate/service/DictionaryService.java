package kz.whosnext.hr.candidate.service;

import kz.whosnext.hr.candidate.exception.BadRequestException;
import kz.whosnext.hr.candidate.exception.ResourceNotFoundException;
import kz.whosnext.hr.candidate.model.dto.request.DictionaryRequest;
import kz.whosnext.hr.candidate.model.dto.response.DictionaryResponse;
import kz.whosnext.hr.candidate.model.entity.ContractType;
import kz.whosnext.hr.candidate.model.entity.Position;
import kz.whosnext.hr.candidate.model.enums.ActivityAction;
import kz.whosnext.hr.candidate.model.enums.ActivitySource;
import kz.whosnext.hr.candidate.repository.ContractTypeRepository;
import kz.whosnext.hr.candidate.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final PositionRepository positionRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final ActivityLogProducer activityLogProducer;

    public List<DictionaryResponse> getAllPositions() {
        return positionRepository.findAll().stream().map(p -> new DictionaryResponse(p.getId(), p.getName())).toList();
    }

    @Transactional
    public DictionaryResponse createPosition(DictionaryRequest req, UUID actorId, String actorEmail, String actorRole) {
        if (positionRepository.existsByName(req.name())) throw new BadRequestException("Должность уже существует");
        Position p = positionRepository.save(Position.builder().name(req.name()).build());

        activityLogProducer.log(
                ActivityAction.POSITION_CREATED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "Position", p.getId().toString(),
                "Position created: " + p.getName());

        return new DictionaryResponse(p.getId(), p.getName());
    }

    @Transactional
    public DictionaryResponse updatePosition(UUID id, DictionaryRequest req, UUID actorId, String actorEmail, String actorRole) {
        Position p = positionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Должность не найдена"));
        String oldName = p.getName();
        p.setName(req.name());
        Position saved = positionRepository.save(p);

        activityLogProducer.log(
                ActivityAction.POSITION_UPDATED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "Position", saved.getId().toString(),
                "Position updated: " + oldName + " -> " + saved.getName());

        return new DictionaryResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public void deletePosition(UUID id, UUID actorId, String actorEmail, String actorRole) {
        Position p = positionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Должность не найдена"));

        activityLogProducer.log(
                ActivityAction.POSITION_DELETED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "Position", id.toString(),
                "Position deleted: " + p.getName());

        positionRepository.delete(p);
    }

    public List<DictionaryResponse> getAllContractTypes() {
        return contractTypeRepository.findAll().stream().map(c -> new DictionaryResponse(c.getId(), c.getName())).toList();
    }

    @Transactional
    public DictionaryResponse createContractType(DictionaryRequest req, UUID actorId, String actorEmail, String actorRole) {
        if (contractTypeRepository.existsByName(req.name())) throw new BadRequestException("Тип договора уже существует");
        ContractType c = contractTypeRepository.save(ContractType.builder().name(req.name()).build());

        activityLogProducer.log(
                ActivityAction.CONTRACT_TYPE_CREATED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "ContractType", c.getId().toString(),
                "Contract type created: " + c.getName());

        return new DictionaryResponse(c.getId(), c.getName());
    }

    @Transactional
    public DictionaryResponse updateContractType(UUID id, DictionaryRequest req, UUID actorId, String actorEmail, String actorRole) {
        ContractType c = contractTypeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Тип договора не найден"));
        String oldName = c.getName();
        c.setName(req.name());
        ContractType saved = contractTypeRepository.save(c);

        activityLogProducer.log(
                ActivityAction.CONTRACT_TYPE_UPDATED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "ContractType", saved.getId().toString(),
                "Contract type updated: " + oldName + " -> " + saved.getName());

        return new DictionaryResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public void deleteContractType(UUID id, UUID actorId, String actorEmail, String actorRole) {
        ContractType c = contractTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Тип договора не найден"));

        activityLogProducer.log(
                ActivityAction.CONTRACT_TYPE_DELETED, ActivitySource.CANDIDATE_SERVICE,
                actorId, actorEmail, actorRole,
                "ContractType", id.toString(),
                "Contract type deleted: " + c.getName());

        contractTypeRepository.delete(c);
    }
}

