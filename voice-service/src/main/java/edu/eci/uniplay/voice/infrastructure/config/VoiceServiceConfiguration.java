package edu.eci.uniplay.voice.infrastructure.config;

import java.time.Clock;

import edu.eci.uniplay.voice.application.port.in.GenerateVoiceTokenUseCase;
import edu.eci.uniplay.voice.application.port.out.VoiceTokenIssuer;
import edu.eci.uniplay.voice.application.service.GenerateVoiceTokenService;
import edu.eci.uniplay.voice.infrastructure.livekit.LiveKitVoiceTokenIssuer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
