package edu.eci.uniplay.metrics.application.port.in;

import edu.eci.uniplay.metrics.application.dto.BusinessKpisResult;

public interface GetBusinessKpisUseCase {

    BusinessKpisResult currentKpis();
}
