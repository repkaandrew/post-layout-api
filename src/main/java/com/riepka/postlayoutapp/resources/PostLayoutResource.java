package com.riepka.postlayoutapp.resources;

import com.riepka.postlayoutapp.entity.PostLayoutInput;
import com.riepka.postlayoutapp.entity.PostLayoutOption;
import com.riepka.postlayoutapp.services.PostLayoutService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("post-layout")
@RequiredArgsConstructor
public class PostLayoutResource {

  private final PostLayoutService service;

  @PostMapping
  private List<PostLayoutOption> calculateLayout(@RequestBody @Valid PostLayoutInput input) {
    return service.calcPostLayout(input);
  }
}
