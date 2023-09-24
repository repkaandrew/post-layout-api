package com.riepka.postlayoutapi.services;

import com.riepka.postlayoutapi.entity.LayoutPost;
import com.riepka.postlayoutapi.entity.PostLayoutInput;
import com.riepka.postlayoutapi.entity.PostLayoutOption;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PostLayoutService {

  public List<PostLayoutOption> calcPostLayout(PostLayoutInput input) {
    return List.of(
        PostLayoutOption.builder()
            .posts(List.of(
                new LayoutPost(input.getRunHorLength())
            ))
            .build()
    );
  }
}
