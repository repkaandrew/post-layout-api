package com.riepka.postlayoutapi.services.calculators;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.riepka.postlayoutapi.entity.Obstruction;
import com.riepka.postlayoutapi.entity.ObstructionType;
import com.riepka.postlayoutapi.entity.PostLayoutDescription;
import com.riepka.postlayoutapi.entity.PostLayoutOption;
import java.util.List;
import org.assertj.core.util.DoubleComparator;
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
    assertRecursivelyEquals(
        extractLayouts(actual),
        List.of(
            List.of(0.0, 100.0, 200.0),
            List.of(0.0, 66.6667, 133.3333, 200.0)
        )
    );
  }

  @Test
  void shouldCalculateCorrectlyDefaultPanels() {
    // given
    final var runLength = 200.1;
    init(runLength, emptyList());
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(extractLayouts(actual))
        .isEqualTo(
            List.of(
                List.of(0.0, 66.7, 133.4, 200.1),
                List.of(0.0, 50.025, 100.05, 150.075, 200.1)
            )
        );
  }

  @Test
  void shouldCalculateCorrectlyWithPlacePostObstruction() {
    // given
    final var runLength = 250;
    init(runLength, List.of(placePost(50)));
    // when
    final var actual = calculator.calculate();
    // then
    assertRecursivelyEquals(
        extractLayouts(actual),
        List.of(
            List.of(0.0, 50.0, 150.0, 250.0),
            List.of(0.0, 50.0, 116.6667, 183.3333, 250.0),
            List.of(0.0, 25.0, 50.0, 150.0, 250.0),
            List.of(0.0, 25.0, 50.0, 116.6667, 183.3333, 250.0)
        )
    );
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
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 84.0, 177.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 96.0, 183.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 90.0, 180.0, 270.0), true, 0, 1, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithTwoTryToAvoidObstructionsAndOneCanBeAvoided() {
    // given
    final var runLength = 270;
    init(runLength, List.of(tryToAvoid(4, 90), tryToAvoid(4, 177.40)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 96.0, 183.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 91.7, 183.4, 270.0), false, 0, 1, 0),
        option(List.of(0.0, 84.0, 177.0, 270.0), false, 0, 1, 0),
        option(List.of(0.0, 85.7, 171.4, 270.0), false, 0, 1, 0)
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
    init(runLength, List.of(tryToAvoid(4, 90), mustAvoid(4, 177.40)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 96.0, 183.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 91.7, 183.4, 270.0), false, 0, 1, 0),
        option(List.of(0.0, 85.7, 171.4, 270.0), false, 0, 1, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithTwoMustAvoidObstructionsAndExistedSolutionForFixedPost() {
    // given
    final var runLength = 270;
    init(runLength, List.of(mustAvoid(4, 90), mustAvoid(4, 177.40)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 96.0, 183.0, 270.0), false, 0, 0, 0)
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
        option(List.of(0.0, 84.0, 174.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 96.0, 186.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 96.0, 174.0, 270.0), false, 0, 0, 0)
    ));
  }

  @Test
  void shouldCombineResultsCorrectly() {
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
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0, 337.5, 405.0, 472.5, 540.0), true, 2, 0, 0),
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0, 354.0, 444.0, 540.0), false, 1, 0, 0),
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0, 366.0, 456.0, 540.0), false, 1, 0, 0),
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0, 366.0, 444.0, 540.0), false, 1, 0, 0),
        option(List.of(0.0, 84.0, 177.0, 270.0, 354.0, 444.0, 540.0), false, 0, 1, 0),
        option(List.of(0.0, 84.0, 177.0, 270.0, 366.0, 456.0, 540.0), false, 0, 1, 0),
        option(List.of(0.0, 96.0, 183.0, 270.0, 354.0, 444.0, 540.0), false, 0, 1, 0),
        option(List.of(0.0, 96.0, 183.0, 270.0, 366.0, 456.0, 540.0), false, 0, 1, 0),
        option(List.of(0.0, 87.0, 174.0, 270.0, 354.0, 444.0, 540.0), false, 0, 1, 0),
        option(List.of(0.0, 87.0, 174.0, 270.0, 366.0, 456.0, 540.0), false, 0, 1, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyWithOneMustAvoidObstruction() {
    // given
    final var runLength = 270;
    init(runLength, List.of(mustAvoid(4, 90)));
    // when
    final var actual = calculator.calculate();
    // then
    assertThat(actual).isEqualTo(List.of(
        option(List.of(0.0, 67.5, 135.0, 202.5, 270.0), true, 1, 0, 0),
        option(List.of(0.0, 84.0, 177.0, 270.0), false, 0, 0, 0),
        option(List.of(0.0, 96.0, 183.0, 270.0), false, 0, 0, 0)
    ));
  }

  @Test
  void shouldCalculateCorrectlyIfSolutionExistOnlyForExtraPostLayout() {
    // given
    final var runLength = 300;
    init(
        runLength,
        List.of(
            mustAvoid(4, 100),
            mustAvoid(4, 200),
            mustAvoid(4, 75),
            mustAvoid(4, 150)
        )
    );
    // when
    final var actual = calculator.calculate();
    // then
    assertRecursivelyEquals(
        actual,
        List.of(
            option(List.of(0.0, 69.0, 144.0, 225.0, 300.0), false, 1, 0, 0),
            option(List.of(0.0, 81.0, 156.0, 225.0, 300.0), false, 1, 0, 0),
            option(List.of(0.0, 69.0, 156.0, 225.0, 300.0), false, 1, 0, 0),
            option(List.of(0.0, 81.0, 144.0, 225.0, 300.0), false, 1, 0, 0),
            option(List.of(0.0, 94.0, 162.6667, 231.3333, 300.0), false, 1, 0, 0),
            option(List.of(0.0, 68.6667, 137.3333, 206.0, 300.0), false, 1, 0, 0)
        )
    );
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

  private <T> void assertRecursivelyEquals(T actual, T expected) {
    assertThat(actual)
        .usingRecursiveComparison()
        .withComparatorForType(new DoubleComparator(0.0001), Double.class)
        .isEqualTo(expected);
  }
}
