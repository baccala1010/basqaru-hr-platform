package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.mapper.DepartmentMapper;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateDepartmentRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdateDepartmentRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.DepartmentResponse;
import kz.whosnext.hr.employeeservice.model.entity.Department;
import kz.whosnext.hr.employeeservice.repository.DepartmentRepository;
import kz.whosnext.hr.employeeservice.repository.EmployeeRepository;
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
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAll().stream()
                .map(d -> {
                    long count = employeeRepository.countByDepartmentId(d.getId());
                    return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(),
                            d.getParentId(), d.getManagerId(), count, d.getCreatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(UUID id) {
        Department d = findById(id);
        long count = employeeRepository.countByDepartmentId(id);
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription(),
                d.getParentId(), d.getManagerId(), count, d.getCreatedAt());
    }

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest req, UUID actorId, String actorEmail, String actorRole) {
        Department dept = Department.builder()
                .name(req.name())
                .description(req.description())
                .parentId(req.parentId())
                .managerId(req.managerId())
                .build();
        dept = departmentRepository.save(dept);
        log.info("Отдел создан: id={}, name={}", dept.getId(), dept.getName());

        activityLogProducer.log(
                ActivityAction.DEPARTMENT_CREATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Department", dept.getId().toString(),
                "Department created: " + dept.getName());

        return departmentMapper.toResponse(dept);
    }

    @Transactional
    public DepartmentResponse update(UUID id, UpdateDepartmentRequest req, UUID actorId, String actorEmail, String actorRole) {
        Department dept = findById(id);
        if (req.name() != null) dept.setName(req.name());
        if (req.description() != null) dept.setDescription(req.description());
        if (req.parentId() != null) dept.setParentId(req.parentId());
        if (req.managerId() != null) dept.setManagerId(req.managerId());
        dept = departmentRepository.save(dept);
        long count = employeeRepository.countByDepartmentId(id);

        activityLogProducer.log(
                ActivityAction.DEPARTMENT_UPDATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Department", id.toString(),
                "Department updated: " + dept.getName());

        return new DepartmentResponse(dept.getId(), dept.getName(), dept.getDescription(),
                dept.getParentId(), dept.getManagerId(), count, dept.getCreatedAt());
    }

    @Transactional
    public void delete(UUID id, UUID actorId, String actorEmail, String actorRole) {
        Department dept = findById(id);

        activityLogProducer.log(
                ActivityAction.DEPARTMENT_DELETED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Department", id.toString(),
                "Department deleted: " + dept.getName());

        departmentRepository.delete(dept);
        log.info("Отдел удалён: id={}", id);
    }

    public Department findById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Отдел не найден: " + id));
    }
}

