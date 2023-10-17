package com.fencegenius.postlayoutapi.services.calculators;

import static java.util.Collections.emptyList;

import com.fencegenius.postlayoutapi.entity.Obstruction;
import com.fencegenius.postlayoutapi.entity.ObstructionType;
import com.fencegenius.postlayoutapi.entity.PostLayoutDescription;
import com.fencegenius.postlayoutapi.entity.PostLayoutOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PostLayoutCalculator {

  private static final double MAX_ALLOWED_INTERSECTION = 0.1;
  private static final int SOLUTIONS_DESIRED = 10;

  // post style size
  private final double postSize;
  // maximal allowed panel length
  private final double panelMaxLength;
  // run horizontal length (post center to post center)
  private final double runLength;
  // obstructions list (Obstruction(size, location, type[MUST_AVOID, TRY_TO_AVOID, PLACE_POST])) sorted left to right
  private final List<Obstruction> obstructions;

  public PostLayoutCalculator(
      double postSize,
      double panelMaxLength,
      double runLength,
      List<Obstruction> obstructions
  ) {
    this.postSize = postSize;
    this.panelMaxLength = panelMaxLength;
    this.runLength = runLength;
    this.obstructions = obstructions;
  }

  /**
   * @return list of found layout prioritized options
   */
  public List<PostLayoutOption> calculate() {
    final var placePostObstructions = obstructions.stream()
        .filter(obstruction -> obstruction.type() == ObstructionType.PLACE_POST)
        .sorted(Comparator.comparingDouble(Obstruction::location))// can be omitted if all obstructions are sorted
        .filter(obstruction -> obstruction.location() < runLength)
        .toList();

    final List<SegmentResult> segmentResultList = new ArrayList<>();

    // Divide by segments and find result(solutions) for each of them
    for (int i = 0; i <= placePostObstructions.size(); i++) {
      final var segmentRedPost = i == 0
          ? 0
          : placePostObstructions.get(i - 1).location();
      final var segmentGreenPost = i == placePostObstructions.size()
          ? runLength
          : placePostObstructions.get(i).location();

      final var segmentObstructions = findSegmentObstructions(segmentRedPost, segmentGreenPost);
      final var segmentLength = segmentGreenPost - segmentRedPost;

      // find solutions for particular segment
      final var segmentSolutions = findSolutionsForSegment(segmentLength, segmentObstructions);

      segmentResultList.add(new SegmentResult(segmentRedPost, segmentSolutions));
    }

    // Combine all results for segments in one with all possible combinations by segments
    SegmentResult combinedResult = segmentResultList.get(0);
    for (int i = 1; i < segmentResultList.size(); i++) {
      final List<SegmentSolution> combinedSolutions = new ArrayList<>();
      final SegmentResult currentSegment = segmentResultList.get(i);

      for (final SegmentSolution mergedSolution : combinedResult.solutions()) {
        for (final SegmentSolution segmentSolution : currentSegment.solutions()) {
          combinedSolutions.add(mergedSolution.combine(currentSegment.location(), segmentSolution));
        }
      }

      combinedResult = new SegmentResult(combinedResult.location(), combinedSolutions);
    }

    // prioritize results and save only desired quantity
    return combinedResult.solutions().stream()
        .sorted(new SolutionComparator())
        .limit(SOLUTIONS_DESIRED)
        .map(this::mapSolutionToOption)
        .toList();
  }

  /**
   * Finds available solutions for given segment (base solution and +1 post solution in worst case)
   *
   * @param segmentLength       segment red post center to green post center length
   * @param segmentObstructions obstructions in segment (with locations related to 0 (segment start point))
   * @return list of solutions available in this segment
   */
  private List<SegmentSolution> findSolutionsForSegment(
      double segmentLength,
      List<Obstruction> segmentObstructions
  ) {
    final List<SegmentSolution> solutions = new ArrayList<>();

    final List<Double> baseLayout = getPostsEvenLayout(segmentLength, 0);
    final var numberOfBasePosts = baseLayout.size();

    // base case (there are no inner posts)
    if (numberOfBasePosts == 0) {
      solutions.add(SegmentSolution.emptySolution(segmentLength));

      return solutions;
    }

    final List<Obstruction> intersectedObstructions = findIntersectedObstructions(baseLayout, segmentObstructions);

    // Check If only <=10% post falls on “Try to avoid” obstruction for base layout
    if (checkIfOnly10PcFallsOnTryAvoid(intersectedObstructions, numberOfBasePosts)) {
      final var solution = new SegmentSolution(
          segmentLength,
          baseLayout,
          new SolutionOptions(true, 0, intersectedObstructions.size(), 0)
      );
      solutions.add(solution);

      // best solution, just return it
      return solutions;
    }

    // Check If only <=10% post falls on “Try to avoid” obstruction for extra post layout
    final List<Double> extraPostLayout = getPostsEvenLayout(segmentLength, 1);
    final List<Obstruction> intersectedObstructionsWithExtraPost =
        findIntersectedObstructions(extraPostLayout, segmentObstructions);
    if (checkIfOnly10PcFallsOnTryAvoid(intersectedObstructionsWithExtraPost, extraPostLayout.size())) {
      final var solution = new SegmentSolution(
          segmentLength,
          extraPostLayout,
          new SolutionOptions(true, 1, intersectedObstructionsWithExtraPost.size(), 0)
      );
      solutions.add(solution);
    }

    // Find solutions with posts shifting for base and +1 post layouts
    for (final List<Double> layout : List.of(baseLayout, extraPostLayout)) {
      final List<SegmentSolution> solutionsWithShifting =
          findSolutionsByShiftingPosts(segmentLength, layout, segmentObstructions, numberOfBasePosts);

      solutions.addAll(solutionsWithShifting);
    }

    // Add base solution as is. I think it shouldn't ever happen
    if (solutions.isEmpty()) {
      solutions.add(getSolutionForBaseLayout(baseLayout, intersectedObstructions, 0, segmentLength));
      solutions.add(getSolutionForBaseLayout(extraPostLayout, intersectedObstructionsWithExtraPost, 1, segmentLength));
    }

    return solutions;
  }

  /**
   * Gets solution for base layout (even panels without any post shifting)
   */
  private SegmentSolution getSolutionForBaseLayout(
      List<Double> baseLayout,
      List<Obstruction> intersectedObstructions,
      int extraPosts,
      double segmentLength
  ) {
    final int tryToAvoidObstructionsNumber = (int) intersectedObstructions.stream()
        .filter(obstruction -> obstruction.type() == ObstructionType.TRY_TO_AVOID)
        .count();
    final int mustAvoidObstructionsNumber = intersectedObstructions.size() - tryToAvoidObstructionsNumber;

    return new SegmentSolution(
        segmentLength,
        baseLayout,
        new SolutionOptions(true, extraPosts, tryToAvoidObstructionsNumber, mustAvoidObstructionsNumber)
    );
  }

  /**
   * Finds solutions by shifting posts falling on obstruction.
   * Includes:
   * - shifting posts one by one around each obstruction (result with fixed post and even panel from both sides)
   * - shifting posts from "Must avoid" obstructions
   * without changing locations for all other posts (including fell on "Try to avoid")
   *
   * @param segmentLength       segment red post center to green post center length
   * @param baseLayout          base layout for which solutions be found
   * @param segmentObstructions obstructions in segment (with locations related to 0 (segment start point))
   * @param initPostsNumb       posts number for initial layout(even panels, no extra posts)
   * @return list of available solutions (can be empty)
   */
  private List<SegmentSolution> findSolutionsByShiftingPosts(
      double segmentLength,
      List<Double> baseLayout,
      List<Obstruction> segmentObstructions,
      int initPostsNumb
  ) {
    final List<SegmentSolution> solutions = new ArrayList<>();
    final List<Obstruction> intersectedObstructions = findIntersectedObstructions(baseLayout, segmentObstructions);

    /*
      At this point we fall on more than 10% of try to avoid or some must avoid obstruction.
      We will move post to the left and to the right of each obstruction and try to find desired result.
    */
    for (final Obstruction intersectedObstruction : intersectedObstructions) {
      final var obstructionLocation = intersectedObstruction.location();
      final var offset = calcObstructionOffset(intersectedObstruction);

      final Optional<SegmentSolution> leftSideSolutionOpt = findSolutionForLayoutWithFixedPostAndEvenPanels(
          segmentLength,
          obstructionLocation - offset,
          segmentObstructions,
          initPostsNumb
      );
      final Optional<SegmentSolution> rightSideSolutionOpt = findSolutionForLayoutWithFixedPostAndEvenPanels(
          segmentLength,
          obstructionLocation + offset,
          segmentObstructions,
          initPostsNumb
      );

      leftSideSolutionOpt.ifPresent(solutions::add);// add to solution if result present
      rightSideSolutionOpt.ifPresent(solutions::add);
    }

    if (!solutions.isEmpty()) {
      return solutions;
    }

    /*
    Try to move post falling on “Must avoid” obstruction to the left/right of obstruction.
    "Try to avoid obstructions" ignored here
     */
    final List<PostLocationObstructionPair> mustAvoidPairs =
        findObstructionByPostLocation(baseLayout, segmentObstructions).stream()
            .filter(pair -> pair.obstruction().type() == ObstructionType.MUST_AVOID)
            .toList();
    if (!mustAvoidPairs.isEmpty()) {
      final var numberOfTryToAvoid = intersectedObstructions.size() - mustAvoidPairs.size();
      final var layoutsWithShiftedPost = findLayoutsWithShiftedPosts(baseLayout, segmentLength, mustAvoidPairs);

      for (final List<Double> layout : layoutsWithShiftedPost) {
        final var layoutPostsNumb = layout.size();
        final var solution = new SegmentSolution(
            segmentLength,
            layout,
            new SolutionOptions(false, layoutPostsNumb - initPostsNumb, numberOfTryToAvoid, 0)
        );
        solutions.add(solution);
      }
    }

    return solutions;
  }

  /**
   * Finds all possible valid(no extra posts needed) layouts with posts shifted around "Must avoid obstructions".
   * All posts that don't fall on "Must avoid" obstruction stay at their place.
   *
   * @param baseLayout                  base layout with posts placed evenly in segment
   * @param segmentLength               segment length
   * @param invalidPostObstructionPairs list of post location/obstruction pairs. Each of this posts will be shifted
   *                                    from the obstruction.
   * @return list of valid all layouts with posts moved from "Must avoid" obstructions.
   * Layout considered as valid if there are no too wide panels after post shifting.
   */
  private List<List<Double>> findLayoutsWithShiftedPosts(
      List<Double> baseLayout,
      double segmentLength,
      List<PostLocationObstructionPair> invalidPostObstructionPairs
  ) {
    // forming list of posts and their shifted locations
    final List<PostShiftedLocations> locationsAtObstructionByPostIndex = new ArrayList<>();

    for (final PostLocationObstructionPair pair : invalidPostObstructionPairs) {
      final Obstruction obstruction = pair.obstruction();
      final double offset = calcObstructionOffset(obstruction);

      locationsAtObstructionByPostIndex.add(new PostShiftedLocations(
          baseLayout.indexOf(pair.location()),
          List.of(obstruction.location() - offset, obstruction.location() + offset))
      );
    }

    /*
     * This list stores all possible combinations - each post + its shifted location.
     * For 2 obstructions it should look like:
     * [[1L, 2L], [1L, 2R], [1R, 2L], [1R, 2R]]
     * where number corresponds to post location index and "L"/"R" - locations from the left/right side of obstruction
     */
    List<List<PostShiftedLocation>> shiftedLocationByPostIndex = new ArrayList<>();

    final PostShiftedLocations firstShiftedPost = locationsAtObstructionByPostIndex.get(0);

    for (double locationAtFirstObstruction : firstShiftedPost.locations()) {
      shiftedLocationByPostIndex.add(
          List.of(new PostShiftedLocation(firstShiftedPost.baseIndex(), locationAtFirstObstruction))
      );
    }

    for (int i = 1; i < locationsAtObstructionByPostIndex.size(); i++) {
      final List<List<PostShiftedLocation>> shiftedLocationByPostIndexBuffer = new ArrayList<>();
      final var currLocationsAtObstruction = locationsAtObstructionByPostIndex.get(i);

      for (final List<PostShiftedLocation> locationByPostIndexList : shiftedLocationByPostIndex) {
        for (double locationAtObstruction : currLocationsAtObstruction.locations()) {
          final List<PostShiftedLocation> newCombination = new ArrayList<>();

          newCombination.addAll(locationByPostIndexList);
          newCombination.add(new PostShiftedLocation(currLocationsAtObstruction.baseIndex(), locationAtObstruction));

          shiftedLocationByPostIndexBuffer.add(newCombination);
        }
      }

      shiftedLocationByPostIndex = new ArrayList<>(shiftedLocationByPostIndexBuffer);
    }

    /*
    Mapping each posts shifting combination into layout and check this layout.
    If layout valid (all panels have valid length) - save it.
    Return all valid layouts.
     */
    return shiftedLocationByPostIndex.stream()
        .map(locations -> getLayoutWithShiftedPosts(baseLayout, locations))
        .filter(layout -> checkIfLayoutPanelsHaveValidLength(layout, segmentLength))
        .toList();
  }

  /**
   * Gets modified base layout with shifted posts
   * @param baseLayout base layout
   * @param shiftedLocations list of locations that should be shifted in base layout
   * @return new list with required locations shifted relatively to base layout
   */
  private List<Double> getLayoutWithShiftedPosts(List<Double> baseLayout, List<PostShiftedLocation> shiftedLocations) {
    final List<Double> shiftedLayout = new ArrayList<>(baseLayout); // copy

    for (final PostShiftedLocation shiftedLocation : shiftedLocations) {
      shiftedLayout.set(shiftedLocation.baseIndex(), shiftedLocation.location());
    }

    return shiftedLayout;
  }

  /**
   * Checks layout for validity (all panels have permitted length).
   * @param layout posts layout
   * @param sectionLength section length
   * @return layout validity
   */
  private boolean checkIfLayoutPanelsHaveValidLength(List<Double> layout, double sectionLength) {
    final double maxCenterToCenter = panelMaxLength + postSize;

    for (int i = 0; i < layout.size() + 1; i++) {
      final double prevLocation = i == 0
          ? 0
          : layout.get(i - 1);
      final double currLocation = i == layout.size()
          ? sectionLength
          : layout.get(i);

      final double postCenterToCenter = currLocation - prevLocation;

      if (postCenterToCenter > maxCenterToCenter) {
        return false;
      }
    }
    return true;
  }

  /**
   * Calculates offset required for post to be shifted from obstruction's center.
   * @param obstruction obstruction
   * @return calculated offset
   */
  private double calcObstructionOffset(Obstruction obstruction) {
    return (obstruction.size() + postSize) / 2;
  }

  /**
   * Finds solution(if exist) with post placed in fixed position inside segment.
   * Segment is divided into two separate parts and each part filled with even panels.
   * If there are no critical intersections with obstructions then solution (covered by Optional) returned,
   * otherwise Optional empty returned
   *
   * @param segmentLength       given segment length
   * @param fixedPostLocation   post location inside segment
   * @param segmentObstructions obstructions present in segment (all locations should be related to segment 0)
   * @param initPostsNumb       number of posts for initial layout(even panels, no extra posts)
   * @return solution if it exists, otherwise empty
   */
  private Optional<SegmentSolution> findSolutionForLayoutWithFixedPostAndEvenPanels(
      double segmentLength,
      double fixedPostLocation,
      List<Obstruction> segmentObstructions,
      int initPostsNumb
  ) {
    final var layoutWithFixedPost = getLayoutWithFixedPost(segmentLength, fixedPostLocation);
    final var intersectedObstructions = findIntersectedObstructions(layoutWithFixedPost, segmentObstructions);

    if (checkIfOnly10PcFallsOnTryAvoid(intersectedObstructions, layoutWithFixedPost.size())) {
      final var layoutPostsNumb = layoutWithFixedPost.size();
      final var solution = new SegmentSolution(
          segmentLength,
          layoutWithFixedPost,
          new SolutionOptions(false, layoutPostsNumb - initPostsNumb, intersectedObstructions.size(), 0)
      );

      return Optional.of(solution);
    }

    return Optional.empty();
  }

  /**
   * Checks if only <=10% post falls on “Try avoid” obstruction
   * @param intersectedObstructions obstructions intersected with posts in layout
   * @param postsNumber posts number in layout
   * @return true if checking passed
   */
  private boolean checkIfOnly10PcFallsOnTryAvoid(Collection<Obstruction> intersectedObstructions, int postsNumber) {
    if (intersectedObstructions.isEmpty()) {
      return true;
    }

    final var onlyTryToAvoid = intersectedObstructions.stream()
        .noneMatch(obstruction -> obstruction.type() == ObstructionType.MUST_AVOID);

    final int maxPermittedFalling = (int) Math.ceil((double) postsNumber / 10);

    return onlyTryToAvoid && maxPermittedFalling >= intersectedObstructions.size();
  }

  /**
   * Creates layout with fixed post location inside segment
   * @param segmentLength segment length
   * @param fixedPostLocation location inside segment length
   * @return layout fixed post and even left/right layout
   */
  private List<Double> getLayoutWithFixedPost(double segmentLength, double fixedPostLocation) {
    final List<Double> layout = new ArrayList<>();
    final var leftLayout = getPostsEvenLayout(fixedPostLocation, 0);
    final var rightLayout = getPostsEvenLayout(segmentLength - fixedPostLocation, 0).stream()
        .map(location -> location + fixedPostLocation)
        .toList();

    layout.addAll(leftLayout);
    layout.add(fixedPostLocation);
    layout.addAll(rightLayout);

    return layout;
  }

  /**
   * Creates even layout for given segment length (only inner posts included)
   * @param segmentLength segment length
   * @param extraPosts number of extra posts to add in default layout(based on panel max length)
   * @return posts even layout
   */
  private List<Double> getPostsEvenLayout(double segmentLength, int extraPosts) {
    final double maxCenterToCenter = panelMaxLength + postSize;
    final List<Double> baseLayout = new ArrayList<>();

    if (segmentLength <= maxCenterToCenter) {
      return baseLayout;
    }

    final int numberOfInnerPosts = (int) (Math.ceil(segmentLength / maxCenterToCenter)) + extraPosts - 1;
    final double defaultCenterToCenter = segmentLength / (numberOfInnerPosts + 1);

    for (int i = 0; i < numberOfInnerPosts; i++) {
      baseLayout.add((i + 1) * defaultCenterToCenter);
    }

    return baseLayout;
  }

  /**
   * Finds obstructions that are located in given range.
   * @param redPostLocation segment red post location (absolute coordinate)
   * @param greenPostLocation segment green post location (absolute coordinate)
   * @return list of obstructions located in provided range.
   * Obstruction's locations related to red post location (has related coordinate)
   */
  private List<Obstruction> findSegmentObstructions(double redPostLocation, double greenPostLocation) {
    return obstructions.stream()
        .filter(obstruction -> {
          final var halfSize = obstruction.size() / 2;
          final var location = obstruction.location();

          return location - halfSize > redPostLocation && location + halfSize < greenPostLocation;
        })
        .map(obstruction -> new Obstruction(
            obstruction.size(),
            obstruction.location() - redPostLocation,
            obstruction.type()
        ))
        .toList();
  }

  /**
   * Finds all obstruction-location pairs for given layout and obstructions
   * @param layout posts layout
   * @param segmentObstructions list of obstructions related to given layout
   * @return list of locations with intersected obstructions
   */
  private List<PostLocationObstructionPair> findObstructionByPostLocation(
      List<Double> layout,
      List<Obstruction> segmentObstructions
  ) {
    return layout.stream()
        .map(location -> findIntersectedObstruction(location, segmentObstructions)// if found use it else set null
            .map(obstruction -> new PostLocationObstructionPair(location, obstruction))
            .orElse(null))
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Finds intersected obstructions for given layout
   * @param layout posts layout
   * @param segmentObstructions list of obstructions related to given layout
   * @return list of intersected obstructions
   */
  private List<Obstruction> findIntersectedObstructions(List<Double> layout, List<Obstruction> segmentObstructions) {
    return layout.stream()
        .map(location -> findIntersectedObstruction(location, segmentObstructions))
        .flatMap(Optional::stream)// filter empty values
        .toList();
  }

  /**
   * Tries to find intersected obstruction for given post location.
   * If found returns this obstruction covered by Optional, otherwise - returns empty
   * @param postLocation post location
   * @param obstructions list of obstructions related to this post location
   * @return obstruction if intersection exists, otherwise empty
   */
  private Optional<Obstruction> findIntersectedObstruction(double postLocation, List<Obstruction> obstructions) {
    return obstructions.stream()
        .filter(obstruction -> {
          final var size = obstruction.size();
          final var location = obstruction.location();
          final var zoneForIntersection = (0.5 - MAX_ALLOWED_INTERSECTION) * size + postSize / 2;

          return (postLocation > location - zoneForIntersection) && (postLocation < location + zoneForIntersection);
        })
        .findFirst(); // return covered first found or empty
  }

  /**
   * Mapper method. additionally add first and last post to segment and builds layout option
   * @param solution solution for segment(all run considered as segment at this place)
   * @return post layout option as projection of solution
   * Where - PostLayoutOption(List<Double> postLocations)
   */
  private PostLayoutOption mapSolutionToOption(SegmentSolution solution) {
    final List<Double> postLayout = new ArrayList<>();
    postLayout.add(0.0);
    postLayout.addAll(solution.postLocations());
    postLayout.add(runLength);

    final var options = solution.options();

    return PostLayoutOption.builder()
        .postLocations(postLayout)
        .description(PostLayoutDescription.builder()
            .evenLayout(options.evenLayout())
            .additionalPosts(options.extraPosts())
            .postsFallOnTryToAvoid(options.placedOnTryToAvoid())
            .postsFallOnMustAvoid(options.placedOnMustAvoid())
            .build())
        .build();
  }

  /**
   * auxiliary classes
   */

  private record SegmentResult(double location, List<SegmentSolution> solutions) {
  }

  // solution that holds segment inner posts layout with its creation options. locations are related to segment
  private record SegmentSolution(double segmentLength, List<Double> postLocations, SolutionOptions options) {

    public static SegmentSolution emptySolution(double segmentLength) {
      return new SegmentSolution(
          segmentLength,
          emptyList(),
          new SolutionOptions(true, 0, 0, 0)
      );
    }

    public SegmentSolution combine(double commonPostLocation, SegmentSolution nextSolution) {
      final List<Double> combinedLocations = new ArrayList<>();

      final var nextLocations = nextSolution.postLocations().stream()
          .map(location -> location + commonPostLocation)
          .toList();

      combinedLocations.addAll(this.postLocations());
      combinedLocations.add(commonPostLocation);
      combinedLocations.addAll(nextLocations);

      final var combinedOptions = options().combine(nextSolution.options());
      final var combinedLength = segmentLength() + nextSolution.segmentLength();

      return new SegmentSolution(combinedLength, combinedLocations, combinedOptions);
    }
  }

  private record SolutionOptions(boolean evenLayout, int extraPosts, int placedOnTryToAvoid, int placedOnMustAvoid) {

    public SolutionOptions combine(SolutionOptions next) {
      return new SolutionOptions(
          evenLayout() && next.evenLayout(),
          extraPosts() + next.extraPosts(),
          placedOnTryToAvoid() + next.placedOnTryToAvoid(),
          placedOnMustAvoid() + next.placedOnTryToAvoid()
      );
    }
  }

  private record PostShiftedLocation(int baseIndex, double location) {
  }

  private record PostShiftedLocations(int baseIndex, List<Double> locations) {
  }

  private record PostLocationObstructionPair(double location, Obstruction obstruction) {
  }

  private static class SolutionComparator implements Comparator<SegmentSolution> {

    @Override
    public int compare(SegmentSolution s1, SegmentSolution s2) {
      final var options1 = s1.options();
      final var options2 = s2.options();

      if (Objects.equals(s1, s2)) {
        return 0;
      }

      if (!Objects.equals(options1.placedOnMustAvoid(), options2.placedOnMustAvoid())) {
        return Double.compare(options1.placedOnMustAvoid(), options2.placedOnMustAvoid());
      }

      if (!Objects.equals(options1.placedOnTryToAvoid(), options2.placedOnTryToAvoid())) {
        return Double.compare(options1.placedOnTryToAvoid(), options2.placedOnTryToAvoid());
      }

      if (!Objects.equals(options1.evenLayout(), options2.evenLayout())) {
        final var extraPostsDiff = options1.extraPosts() - options2.extraPosts();

        // +2 extra posts is worse than even layout
        final var firstOptionBetter = (options1.evenLayout() && extraPostsDiff <= 1)
            || (options2.evenLayout() && extraPostsDiff < -1);

        return firstOptionBetter ? -1 : 1;
      }

      if (!Objects.equals(options1.extraPosts(), options2.extraPosts())) {
        return Double.compare(options1.extraPosts(), options2.extraPosts());
      }

      return compareByDeviation(s1, s2);
    }

    private int compareByDeviation(SegmentSolution s1, SegmentSolution s2) {
      return Double.compare(calcLayoutDispersion(s1), calcLayoutDispersion(s2));
    }

    private double calcLayoutDispersion(SegmentSolution solution) {
      final List<Double> centerToCenterLength = new ArrayList<>();
      final var layout = solution.postLocations();
      final var segmentLength = solution.segmentLength();

      if (layout.isEmpty()) {
        return 0;
      }

      for (int i = 0; i <= layout.size(); i++) {
        final var prevLocation = i == 0
            ? 0
            : layout.get(i - 1);
        final var currLocation = i == layout.size()
            ? segmentLength
            : layout.get(i);
        final var c2c = currLocation - prevLocation;

        centerToCenterLength.add(c2c);
      }

      final double average = segmentLength / (layout.size() + 1);

      final var deviationSum = centerToCenterLength.stream()
          .mapToDouble(c2c -> Math.pow(c2c - average, 2))
          .sum();

      return Math.sqrt(deviationSum / layout.size());
    }
  }
}
