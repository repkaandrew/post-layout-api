package com.riepka.postlayoutapi.entity;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalculationData {

  private final double postSize;
  private final double panelMaxLength;
  private final double runHorLength;
  private final List<Obstruction> obstructions;
}
