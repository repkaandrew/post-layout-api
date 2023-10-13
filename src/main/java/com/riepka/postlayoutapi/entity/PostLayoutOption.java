package com.riepka.postlayoutapi.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostLayoutOption {

  private final List<Double> postLocations;
}
