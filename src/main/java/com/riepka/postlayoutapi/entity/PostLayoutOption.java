package com.riepka.postlayoutapi.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PostLayoutOption {

  private final List<Double> postLocations;

  private final PostLayoutDescription description;
}
