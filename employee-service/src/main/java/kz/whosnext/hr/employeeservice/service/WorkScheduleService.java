package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.mapper.WorkScheduleMapper;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateWorkScheduleRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.WorkScheduleResponse;
import kz.whosnext.hr.employeeservice.model.entity.WorkSchedule;
import kz.whosnext.hr.employeeservice.model.enums.WorkScheduleType;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import kz.whosnext.hr.employeeservice.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final WorkScheduleMapper workScheduleMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public List<WorkScheduleResponse> listAll() {
        return workScheduleRepository.findAll().stream()
                .map(workScheduleMapper::toResponse)
                .toList();
    }

    @Transactional
    public WorkScheduleResponse create(CreateWorkScheduleRequest req, UUID actorId, String actorEmail, String actorRole) {
        WorkSchedule schedule = WorkSchedule.builder()
                .name(req.name())
                .type(WorkScheduleType.valueOf(req.type()))
                .startTime(req.startTime())
                .endTime(req.endTime())
                .workingDays(req.workingDays())
                .build();
        schedule = workScheduleRepository.save(schedule);
        log.info("График работы создан: id={}", schedule.getId());

        activityLogProducer.log(
                ActivityAction.WORK_SCHEDULE_CREATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "WorkSchedule", schedule.getId().toString(),
                "Work schedule created: " + schedule.getName());

        return workScheduleMapper.toResponse(schedule);
    }

    @Transactional
    public WorkScheduleResponse update(UUID id, CreateWorkScheduleRequest req, UUID actorId, String actorEmail, String actorRole) {
        WorkSchedule schedule = findById(id);
        schedule.setName(req.name());
        schedule.setType(WorkScheduleType.valueOf(req.type()));
        schedule.setStartTime(req.startTime());
        schedule.setEndTime(req.endTime());
        schedule.setWorkingDays(req.workingDays());
        schedule = workScheduleRepository.save(schedule);
        log.info("График работы обновлён: id={}", schedule.getId());

        activityLogProducer.log(
                ActivityAction.WORK_SCHEDULE_UPDATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "WorkSchedule", id.toString(),
                "Work schedule updated: " + schedule.getName());

        return workScheduleMapper.toResponse(schedule);
    }

    @Transactional
    public void delete(UUID id, UUID actorId, String actorEmail, String actorRole) {
        WorkSchedule schedule = findById(id);

        activityLogProducer.log(
                ActivityAction.WORK_SCHEDULE_DELETED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "WorkSchedule", id.toString(),
                "Work schedule deleted: " + schedule.getName());

        workScheduleRepository.delete(schedule);
    }

    public WorkSchedule findById(UUID id) {
        return workScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("График не найден: " + id));
    }
}

