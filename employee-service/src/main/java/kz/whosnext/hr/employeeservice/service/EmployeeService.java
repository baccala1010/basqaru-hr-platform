package kz.whosnext.hr.employeeservice.service;

import kz.whosnext.hr.employeeservice.exception.ResourceNotFoundException;
import kz.whosnext.hr.employeeservice.feign.AuthFeignClient;
import kz.whosnext.hr.employeeservice.mapper.EmployeeMapper;
import kz.whosnext.hr.employeeservice.model.dto.request.CreateEmployeeRequest;
import kz.whosnext.hr.employeeservice.model.dto.request.UpdateEmployeeRequest;
import kz.whosnext.hr.employeeservice.model.dto.response.EmployeeResponse;
import kz.whosnext.hr.employeeservice.model.entity.Department;
import kz.whosnext.hr.employeeservice.model.entity.Employee;
import kz.whosnext.hr.employeeservice.model.entity.WorkSchedule;
import kz.whosnext.hr.employeeservice.model.enums.EmployeeStatus;
import kz.whosnext.hr.employeeservice.model.enums.ActivityAction;
import kz.whosnext.hr.employeeservice.model.enums.ActivitySource;
import kz.whosnext.hr.employeeservice.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final WorkScheduleService workScheduleService;
    private final EmployeeMapper employeeMapper;
    private final AuthFeignClient authFeignClient;
    private final ActivityLogProducer activityLogProducer;

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(String search, UUID departmentId, String status, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return employeeRepository.search(search, pageable).map(emp -> enrichWithPhotoUrl(employeeMapper.toResponse(emp)));
        }
        if (departmentId != null) {
            return employeeRepository.findByDepartmentId(departmentId, pageable).map(emp -> enrichWithPhotoUrl(employeeMapper.toResponse(emp)));
        }
        if (status != null) {
            return employeeRepository.findByStatus(EmployeeStatus.valueOf(status), pageable).map(emp -> enrichWithPhotoUrl(employeeMapper.toResponse(emp)));
        }
        return employeeRepository.findAll(pageable).map(emp -> enrichWithPhotoUrl(employeeMapper.toResponse(emp)));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        return enrichWithPhotoUrl(employeeMapper.toResponse(findById(id)));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getByUserId(UUID userId) {
        Employee emp = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден по userId: " + userId));
        return enrichWithPhotoUrl(employeeMapper.toResponse(emp));
    }

    private EmployeeResponse enrichWithPhotoUrl(EmployeeResponse response) {
        if (response.userId() == null) return response;
        try {
            Map<String, Object> user = authFeignClient.getUserById(response.userId());
            String photoUrl = user != null ? (String) user.get("photoUrl") : null;
            return new EmployeeResponse(
                    response.id(), response.userId(), response.candidateId(), response.applicationId(),
                    response.departmentId(), response.departmentName(),
                    response.workScheduleId(), response.workScheduleName(),
                    response.positionName(), response.contractType(),
                    response.firstName(), response.lastName(), response.middleName(),
                    response.iin(), response.email(), response.phone(), response.address(),
                    response.birthDate(), response.hireDate(), response.fireDate(),
                    response.status(), response.createdAt(), photoUrl
            );
        } catch (Exception e) {
            log.warn("Не удалось получить photoUrl для userId={}: {}", response.userId(), e.getMessage());
            return response;
        }
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req, UUID actorId, String actorEmail, String actorRole) {
        Department department = req.departmentId() != null ? departmentService.findById(req.departmentId()) : null;
        WorkSchedule schedule = req.workScheduleId() != null ? workScheduleService.findById(req.workScheduleId()) : null;

        Employee emp = Employee.builder()
                .userId(req.userId())
                .candidateId(req.candidateId())
                .applicationId(req.applicationId())
                .department(department)
                .workSchedule(schedule)
                .positionName(req.positionName())
                .contractType(req.contractType())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .middleName(req.middleName())
                .iin(req.iin())
                .email(req.email())
                .phone(req.phone())
                .address(req.address())
                .birthDate(req.birthDate())
                .hireDate(req.hireDate())
                .status(EmployeeStatus.PROBATION)
                .build();

        emp = employeeRepository.save(emp);
        log.info("Сотрудник создан: id={}, userId={}", emp.getId(), emp.getUserId());

        activityLogProducer.log(
                ActivityAction.EMPLOYEE_CREATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Employee", emp.getId().toString(),
                "Employee created: " + emp.getFirstName() + " " + emp.getLastName());

        return enrichWithPhotoUrl(employeeMapper.toResponse(emp));
    }

    @Transactional
    public EmployeeResponse update(UUID id, UpdateEmployeeRequest req, UUID actorId, String actorEmail, String actorRole) {
        Employee emp = findById(id);
        if (req.departmentId() != null) emp.setDepartment(departmentService.findById(req.departmentId()));
        if (req.workScheduleId() != null) emp.setWorkSchedule(workScheduleService.findById(req.workScheduleId()));
        if (req.positionName() != null) emp.setPositionName(req.positionName());
        if (req.contractType() != null) emp.setContractType(req.contractType());
        if (req.firstName() != null) emp.setFirstName(req.firstName());
        if (req.lastName() != null) emp.setLastName(req.lastName());
        if (req.middleName() != null) emp.setMiddleName(req.middleName());
        if (req.phone() != null) emp.setPhone(req.phone());
        if (req.address() != null) emp.setAddress(req.address());
        emp = employeeRepository.save(emp);
        log.info("Сотрудник обновлён: id={}", id);

        activityLogProducer.log(
                ActivityAction.EMPLOYEE_UPDATED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Employee", id.toString(),
                "Employee updated");

        return enrichWithPhotoUrl(employeeMapper.toResponse(emp));
    }

    @Transactional
    public EmployeeResponse dismiss(UUID id, UUID actorId, String actorEmail, String actorRole) {
        Employee emp = findById(id);
        emp.setStatus(EmployeeStatus.DISMISSED);
        emp.setFireDate(LocalDate.now());
        emp = employeeRepository.save(emp);
        log.info("Сотрудник уволен: id={}, by={}", id, actorId);

        activityLogProducer.log(
                ActivityAction.EMPLOYEE_DISMISSED, ActivitySource.EMPLOYEE_SERVICE,
                actorId, actorEmail, actorRole,
                "Employee", id.toString(),
                "Employee dismissed");

        return enrichWithPhotoUrl(employeeMapper.toResponse(emp));
    }

    public Employee findById(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден: " + id));
    }
}

