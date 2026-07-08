package edu.eci.uniplay.metrics.infrastructure.web;

import edu.eci.uniplay.metrics.application.port.in.GetBusinessKpisUseCase;
import edu.eci.uniplay.metrics.infrastructure.web.dto.BusinessKpisResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final GetBusinessKpisUseCase getBusinessKpisUseCase;

    public MetricsController(GetBusinessKpisUseCase getBusinessKpisUseCase) {
        this.getBusinessKpisUseCase = getBusinessKpisUseCase;
    }

    @GetMapping("/kpis")
    BusinessKpisResponse currentKpis() {
        return BusinessKpisResponse.from(getBusinessKpisUseCase.currentKpis());
    }
}
