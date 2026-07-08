package edu.eci.uniplay.voice.infrastructure.config;

import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.uniplay.voice.application.port.in.ChangeMuteStateUseCase;
import edu.eci.uniplay.voice.application.port.in.ChangeSpeakingStateUseCase;
import edu.eci.uniplay.voice.application.port.in.GenerateVoiceTokenUseCase;
import edu.eci.uniplay.voice.application.port.out.VoiceEventPublisher;
import edu.eci.uniplay.voice.application.port.out.VoiceTokenIssuer;
import edu.eci.uniplay.voice.application.service.ChangeMuteStateService;
import edu.eci.uniplay.voice.application.service.ChangeSpeakingStateService;
import edu.eci.uniplay.voice.application.service.GenerateVoiceTokenService;
import edu.eci.uniplay.voice.infrastructure.livekit.LiveKitVoiceTokenIssuer;
import edu.eci.uniplay.voice.infrastructure.redis.RedisVoiceEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class VoiceServiceConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    VoiceTokenIssuer voiceTokenIssuer(VoiceProperties voiceProperties) {
        return new LiveKitVoiceTokenIssuer(voiceProperties);
    }

    @Bean
    GenerateVoiceTokenUseCase generateVoiceTokenUseCase(
            VoiceTokenIssuer voiceTokenIssuer,
            VoiceProperties voiceProperties,
            Clock clock
    ) {
        return new GenerateVoiceTokenService(voiceTokenIssuer, voiceProperties, clock);
    }

    @Bean
    VoiceEventPublisher voiceEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisVoiceEventPublisher(redisTemplate, objectMapper);
    }

    @Bean
    ChangeMuteStateUseCase changeMuteStateUseCase(VoiceEventPublisher voiceEventPublisher, Clock clock) {
        return new ChangeMuteStateService(voiceEventPublisher, clock);
    }

    @Bean
    ChangeSpeakingStateUseCase changeSpeakingStateUseCase(VoiceEventPublisher voiceEventPublisher, Clock clock) {
        return new ChangeSpeakingStateService(voiceEventPublisher, clock);
    }
}
