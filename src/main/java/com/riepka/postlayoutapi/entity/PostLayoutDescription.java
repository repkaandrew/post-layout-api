package com.riepka.postlayoutapi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PostLayoutDescription {

  private final int additionalPosts;
  private final boolean evenLayout;
  private final int postsFallOnTryToAvoid;
  private final int postsFallOnMustAvoid;
}
