package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.exception.BadRequestException;
import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.mapper.PositionMapper;
import kz.whosnext.hr.employeeservice.model.dto.request.CreatePositionRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdatePositionRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.PositionResponse;
import kz.whosnext.hr.employeeservice.model.entity.Position;
import kz.whosnext.hr.employeeservice.repository.PositionRepository;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public List<PositionResponse> listAll() {
        return positionRepository.findAll().stream()
                .map(positionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PositionResponse getById(UUID id) {
        return positionMapper.toResponse(findById(id));
    }

    @Transactional
    public PositionResponse create(CreatePositionRequest req, UUID actorId, String actorEmail, String actorRole) {
        if (positionRepository.existsByName(req.name()))
            throw new BadRequestException("Должность уже существует: " + req.name());
        Position position = Position.builder()
                .name(req.name())
                .description(req.description())
                .build();
        position = positionRepository.save(position);
        log.info("Должность создана: id={}, name={}", position.getId(), position.getName());

        activityLogProducer.log(
                ActivityAction.EMP_POSITION_CREATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Position", position.getId().toString(),
                "Position created: " + position.getName());

        return positionMapper.toResponse(position);
    }

    @Transactional
    public PositionResponse update(UUID id, UpdatePositionRequest req, UUID actorId, String actorEmail, String actorRole) {
        Position position = findById(id);
        if (req.name() != null) position.setName(req.name());
        if (req.description() != null) position.setDescription(req.description());
        position = positionRepository.save(position);
        log.info("Должность обновлена: id={}", id);

        activityLogProducer.log(
                ActivityAction.EMP_POSITION_UPDATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Position", id.toString(),
                "Position updated: " + position.getName());

        return positionMapper.toResponse(position);
    }

    @Transactional
    public void delete(UUID id, UUID actorId, String actorEmail, String actorRole) {
        Position position = findById(id);

        activityLogProducer.log(
                ActivityAction.EMP_POSITION_DELETED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Position", id.toString(),
                "Position deleted: " + position.getName());

        positionRepository.delete(position);
        log.info("Должность удалена: id={}", id);
    }

    private Position findById(UUID id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Должность не найдена: " + id));
    }
}

