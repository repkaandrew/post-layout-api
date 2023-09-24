package com.riepka.postlayoutapp.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ObstructionDataInput {

  private final double size;
  private final double horLocation;
  private final ObstructionType type;
}
