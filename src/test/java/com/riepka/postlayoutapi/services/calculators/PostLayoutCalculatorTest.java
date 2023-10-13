package com.riepka.postlayoutapi.services.calculators;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.riepka.postlayoutapi.entity.Obstruction;
import com.riepka.postlayoutapi.entity.ObstructionType;
import com.riepka.postlayoutapi.entity.PostLayoutDescription;
import com.riepka.postlayoutapi.entity.PostLayoutOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostLayoutCalculatorTest {

  private static final double POST_SIZE = 4;
  private static final double PANEL_MAX_LENGTH = 96.0;
  private PostLayoutCalculator calculator;

  private void init(double runLength, List<Obstruction> obstructions) {
    calculator = new PostLayoutCalculator(POST_SIZE, PANEL_MAX_LENGTH, runLength, obstructions);
  }

  @Test
  void shouldCalculateCorrectlyDefaultLayout() {
    // given
    final var runLength = 200;
    init(runLength, emptyList());
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(extractLayouts(actual)).containsExactly(List.of(0.0, 100.0, 200.0));
  }

  @Test
  void shouldCalculateCorrectlyDefaultPanels() {
    // given
    final var runLength = 200.1;
    init(runLength, emptyList());
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(extractLayouts(actual)).containsExactly(List.of(0.0, 66.7, 133.4, 200.1));
  }

  @Test
  void shouldCalculateCorrectlyWithPlacePostObstruction() {
    // given
    final var runLength = 250;
    init(runLength, List.of(placePost(50)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(extractLayouts(actual)).containsExactly(List.of(0.0, 50.0, 150.0, 250.0));
  }

  @Test
  void shouldCalculateCorrectlyWithTryToAvoidObstructionsNumberLessThan10Percent() {
    // given
    final var runLength = 270;
    init(runLength, List.of(tryToAvoid(4, 90)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 90.0, 180.0, 270.0), true, 0, 1, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithTwoTryToAvoidObstructions() {
    // given
    final var runLength = 270;
    init(runLength, List.of(tryToAvoid(4, 90), tryToAvoid(4, 178.40)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 94.0, 182.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 91.2, 182.4, 270.0), false, 0, 1, 0),
        option(List.of(0.0, 86.0, 178.0, 270.0), false, 0, 1, 0),
        option(List.of(0.0, 87.2, 174.4, 270.0), false, 0, 1, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithTryToAvoidObstructionsNumberMoreThan10Percent() {
    // given
    final var runLength = 360;
    init(runLength, List.of(tryToAvoid(4, 90), tryToAvoid(4, 180), tryToAvoid(4, 270)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 72.0, 144.0, 216.0, 288.0, 360.0), true, 1, 0, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithTryToAvoidAndMustAvoidObstructionsAndExistedSolutionForFixedPost() {
    // given
    final var runLength = 270;
    init(runLength, List.of(tryToAvoid(4, 90), mustAvoid(4, 178.40)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 94.0, 182.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 91.2, 182.4, 270.0), false, 0, 1, 0),
        option(List.of(0.0, 87.2, 174.4, 270.0), false, 0, 1, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithTwoMustAvoidObstructionsAndExistedSolutionForFixedPost() {
    // given
    final var runLength = 270;
    init(runLength, List.of(mustAvoid(4, 90), mustAvoid(4, 178.40)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 94.0, 182.0, 270.0), false, 0, 0, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithTwoMustAvoidObstructionsAndSolutionsWithPostsShifting() {
    // given
    final var runLength = 270;
    init(runLength, List.of(mustAvoid(4, 90), mustAvoid(4, 180)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 86.0, 176.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 94.0, 184.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 86.0, 184.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 94.0, 176.0, 270.0), false, 0, 0, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithMultipleSections() {
    // given
    final var runLength = 540;
    init(
        runLength,
        List.of(
            placePost( 270),
            tryToAvoid(4, 90),
            tryToAvoid(4, 180),
            mustAvoid(4, 360),
            mustAvoid(4, 450)
        ));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 86.0, 176.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 94.0, 184.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 86.0, 184.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 94.0, 176.0, 270.0), false, 0, 0, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithMustAvoidObstruction() {
    // given
    final var runLength = 270;
    init(runLength, List.of(mustAvoid(4, 90)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0, 337.5, 405.0, 472.5), true, 2, 0, 0),
        option(List.of(0.0, 86.0, 178.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 94.0, 182.0, 270.0), false, 0, 0, 0)
    ));
  }

  private List<List<Double>> extractLayouts(List<PostLayoutOption> options) {
    return options.stream()
        .map(PostLayoutOption::getPostLocations)
        .toList();
  }

  private Obstruction mustAvoid(double size, double location) {
    return new Obstruction(size, location, ObstructionType.MUST_AVOID);
  }

  private Obstruction placePost(double location) {
    return new Obstruction(POST_SIZE, location, ObstructionType.PLACE_POST);
  }

  private Obstruction tryToAvoid(double size, double location) {
    return new Obstruction(size, location, ObstructionType.TRY_TO_AVOID);
  }

  private PostLayoutOption option(List<Double> layout, boolean even, int posts, int tryToAvoid, int mustAvoid) {
    return PostLayoutOption.builder()
        .postLocations(layout)
        .description(PostLayoutDescription.builder()
            .evenLayout(even)
            .additionalPosts(posts)
            .postsFallOnTryToAvoid(tryToAvoid)
            .postsFallOnMustAvoid(mustAvoid)
            .build())
        .build();
  }
}