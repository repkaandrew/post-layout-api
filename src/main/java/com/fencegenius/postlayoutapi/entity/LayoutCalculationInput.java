package com.fencegenius.postlayoutapi.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LayoutCalculationInput {

  @Positive
  private final double postSize;

  @Positive
  private final double panelMaxLength;

  @Positive
  private final double runHorLength;

  @Valid
  @NotNull
  private final List<ObstructionInput> obstructions;
}
