package com.riepka.postlayoutapi.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostLayoutInput {

  @Positive
  private final double postSize;

  @Positive
  private final double panelMaxLength;

  @Positive
  private final double runHorLength;

  @NotNull
  private final List<ObstructionDataInput> obstructions;
}
