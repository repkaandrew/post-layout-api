package com.fencegenius.postlayoutapi.resources;

import com.fencegenius.postlayoutapi.entity.LayoutCalculationInput;
import com.fencegenius.postlayoutapi.entity.PostLayoutOption;
import com.fencegenius.postlayoutapi.services.PostLayoutService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/post-layout")
@RequiredArgsConstructor
public class PostLayoutResource {

  private final PostLayoutService service;

  @PostMapping
  public List<PostLayoutOption> calculateLayout(@RequestBody @Valid LayoutCalculationInput input) {
    return service.calcPostLayout(input);
  }
}
