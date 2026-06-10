package kz.whosnext.hr.authservice;

import kz.whosnext.hr.auth.AuthServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AuthServiceApplication.class)
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
