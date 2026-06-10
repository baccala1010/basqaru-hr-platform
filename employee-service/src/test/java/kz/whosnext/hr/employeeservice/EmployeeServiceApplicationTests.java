package kz.whosnext.hr.employeeservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = EmployeeServiceApplication.class)
@ActiveProfiles("test")
class EmployeeServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
