package com.fencegenius.postlayoutapi.services;

import com.fencegenius.postlayoutapi.services.calculators.PostLayoutCalculator;
import com.fencegenius.postlayoutapi.entity.LayoutCalculationInput;
import com.fencegenius.postlayoutapi.entity.PostLayoutOption;
import com.fencegenius.postlayoutapi.mapper.CalculationDataMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostLayoutService {

  public List<PostLayoutOption> calcPostLayout(LayoutCalculationInput input) {
    final var calcData = CalculationDataMapper.INSTANCE.toCalculationData(input);

    final var calculator = new PostLayoutCalculator(
        calcData.getPostSize(),
        calcData.getPanelMaxLength(),
        calcData.getRunHorLength(),
        calcData.getObstructions()
    );

    return calculator.calculate();
  }
}
