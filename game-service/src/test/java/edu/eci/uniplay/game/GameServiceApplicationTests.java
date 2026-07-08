package edu.eci.uniplay.game;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class GameServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainStartsApplication() {
        assertThatCode(() -> GameServiceApplication.main(new String[] {
                "--spring.main.web-application-type=none"
        })).doesNotThrowAnyException();
    }
}
