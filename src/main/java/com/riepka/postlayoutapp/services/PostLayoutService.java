package com.riepka.postlayoutapp.services;

import com.riepka.postlayoutapp.entity.LayoutPost;
import com.riepka.postlayoutapp.entity.PostLayoutInput;
import com.riepka.postlayoutapp.entity.PostLayoutOption;
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
