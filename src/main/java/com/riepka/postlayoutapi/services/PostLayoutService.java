package com.riepka.postlayoutapi.services;

import com.riepka.postlayoutapi.entity.LayoutCalculationInput;
import com.riepka.postlayoutapi.entity.PostLayoutOption;
import com.riepka.postlayoutapi.mapper.CalculationDataMapper;
import com.riepka.postlayoutapi.services.calculators.PostLayoutCalculator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostLayoutService {

  private final CalculationDataMapper mapper;

  public List<PostLayoutOption> calcPostLayout(LayoutCalculationInput input) {
    final var calcData = mapper.toCalculationData(input);

    final var calculator = new PostLayoutCalculator(
        calcData.getPostSize(),
        calcData.getPanelMaxLength(),
        calcData.getRunHorLength(),
        calcData.getObstructions()
    );

    return calculator.calculate();
  }
}
