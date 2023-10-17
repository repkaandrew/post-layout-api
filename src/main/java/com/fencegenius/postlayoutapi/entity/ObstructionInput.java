package com.fencegenius.postlayoutapi.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ObstructionInput {

  @Positive
  private final double size;
  @Positive
  private final double location;
  @NotNull
  private final ObstructionType type;
}
