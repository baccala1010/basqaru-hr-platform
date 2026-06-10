package kz.whosnext.hr.candidateservice;

import kz.whosnext.hr.candidate.CandidateServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CandidateServiceApplication.class)
@ActiveProfiles("test")
class CandidateServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
