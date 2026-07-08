package edu.eci.uniplay.game.infrastructure.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class GamePropertiesTest {

    @Test
    void bindsGameProperties() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource();
        source.put("uniplay.game.points-per-correct-answer", "150");
        source.put("uniplay.game.session-ttl", "PT1H");

        BindResult<GameProperties> result = new Binder(source)
                .bind("uniplay.game", GameProperties.class);

        assertThat(result.get().pointsPerCorrectAnswer()).isEqualTo(150);
        assertThat(result.get().sessionTtl()).isEqualTo(Duration.ofHours(1));
    }
}
