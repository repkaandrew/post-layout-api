package com.riepka.postlayoutapp.entity;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostLayoutOption {

  private final List<LayoutPost> posts;
}
