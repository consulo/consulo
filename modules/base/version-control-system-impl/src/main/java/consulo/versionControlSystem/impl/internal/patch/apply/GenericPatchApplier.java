/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.impl.internal.patch.apply;

import consulo.application.util.LineTokenizer;
import consulo.diff.util.IntPair;
import consulo.document.util.TextRange;
import consulo.document.util.UnfairTextRange;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.BeforeAfter;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.change.patch.PatchLine;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class GenericPatchApplier {
  private static final Logger LOG = Logger.getInstance(GenericPatchApplier.class);
  private final static int ourMaxWalk = 1000;

  private final TreeMap<TextRange, MyAppliedData> myTransformations;
  private final List<String> myLines;
  private final List<PatchHunk> myHunks;
  private final boolean myBaseFileEndsWithNewLine;
  private boolean myHadAlreadyAppliedMet;

  private final ArrayList<SplitHunk> myNotBound;
  private final ArrayList<SplitHunk> myNotExact;
  private boolean mySuppressNewLineInEnd;
  @Nonnull
  private List<AppliedTextPatch.AppliedSplitPatchHunk> myAppliedInfo;
  private static IntPair EMPTY_OFFSET = new IntPair(0, 0);

  private static void debug(String s) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(s);
    }
  }

  public GenericPatchApplier(CharSequence text, List<PatchHunk> hunks) {
    debug("GenericPatchApplier created, hunks: " + hunks.size());
    myLines = new ArrayList<>();
    Collections.addAll(myLines, LineTokenizer.tokenize(text, false));
    myBaseFileEndsWithNewLine = StringUtil.endsWithLineBreak(text);
    myHunks = hunks;
    Comparator<TextRange> textRangeComparator =
            (o1, o2) -> Integer.compareUnsigned(o1.getStartOffset(), o2.getStartOffset());
    myTransformations = new TreeMap<>(textRangeComparator);
    myNotExact = new ArrayList<>();
    myNotBound = new ArrayList<>();
    myAppliedInfo = new ArrayList<>();
  }

  public ApplyPatchStatus getStatus() {
    if (! myNotExact.isEmpty()) {
      return ApplyPatchStatus.FAILURE;
    } else {
      if (myTransformations.isEmpty() && myHadAlreadyAppliedMet) return ApplyPatchStatus.ALREADY_APPLIED;
      boolean haveAlreadyApplied = myHadAlreadyAppliedMet;
      boolean haveTrue = false;
      for (MyAppliedData data : myTransformations.values()) {
        if (data.isHaveAlreadyApplied()) {
          haveAlreadyApplied |= true;
        } else {
          haveTrue = true;
        }
      }
      if (haveAlreadyApplied && ! haveTrue) return ApplyPatchStatus.ALREADY_APPLIED;
      if (haveAlreadyApplied) return ApplyPatchStatus.PARTIAL;
      return ApplyPatchStatus.SUCCESS;
    }
  }

  private void printTransformations(String comment) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(comment + " GenericPatchApplier.printTransformations ---->");
      int cnt = 0;
      for (Map.Entry<TextRange, MyAppliedData> entry : myTransformations.entrySet()) {
        TextRange key = entry.getKey();
        MyAppliedData value = entry.getValue();
        LOG.info(String.valueOf(cnt) +
                 " lines " +
                 key.getStartOffset() +
                 ":" +
                 key.getEndOffset() +
                 " will replace into: " +
                 StringUtil.join(value.getList(), "\n"));
      }
      LOG.debug("<------ GenericPatchApplier.printTransformations");
    }
  }

  public int weightContextMatch(int maxWalk, int maxPartsToCheck) {
    List<SplitHunk> hunks = new ArrayList<>(myHunks.size());
    for (PatchHunk hunk : myHunks) {
      hunks.addAll(SplitHunk.read(hunk));
    }
    int cntPlus = 0;
    int cnt = maxPartsToCheck;
    for (SplitHunk hunk : hunks) {
      SplitHunk copy = createWithAllContextCopy(hunk);
      if (copy.isInsertion()) continue;
      if (testForPartialContextMatch(copy, new ExactMatchSolver(copy), maxWalk, null)) {
        ++cntPlus;
      }
      -- cnt;
      if (cnt == 0) break;
    }
    return cntPlus;
  }

  public boolean execute() {
    debug("GenericPatchApplier execute started");
    if (! myHunks.isEmpty()) {
      mySuppressNewLineInEnd = myHunks.get(myHunks.size() - 1).isNoNewLineAtEnd();
    }
    for (PatchHunk hunk : myHunks) {
      myNotExact.addAll(SplitHunk.read(hunk));
    }
    for (Iterator<SplitHunk> iterator = myNotExact.iterator(); iterator.hasNext(); ) {
      SplitHunk splitHunk = iterator.next();
      SplitHunk copy = createWithAllContextCopy(splitHunk);
      if (testForExactMatch(copy, splitHunk)) {
        iterator.remove();
      }
    }
    printTransformations("after exact match");
    /*for (SplitHunk hunk : myNotExact) {
      complementInsertAndDelete(hunk);
    }*/

    for (Iterator<SplitHunk> iterator = myNotExact.iterator(); iterator.hasNext(); ) {
      SplitHunk hunk = iterator.next();
      SplitHunk copy = createWithAllContextCopy(hunk);
      if (copy.isInsertion()) continue;
      if (testForPartialContextMatch(copy, new ExactMatchSolver(copy), ourMaxWalk, hunk)) {
        iterator.remove();
      }
    }
    printTransformations("after exact but without context");
    for (Iterator<SplitHunk> iterator = myNotExact.iterator(); iterator.hasNext(); ) {
      SplitHunk hunk = iterator.next();
      SplitHunk original = copySplitHunk(hunk, hunk.getContextAfter(), hunk.getContextBefore());
      complementInsertAndDelete(hunk);
      if (hunk.isInsertion()) {
        processAppliedInfoForUnApplied(original);
        continue;
      }
      if (testForPartialContextMatch(hunk, new ExactMatchSolver(hunk), ourMaxWalk, original)) {
        iterator.remove();
      }
      else {
        processAppliedInfoForUnApplied(original);
      }
    }
    printTransformations("after variable place match");
    return myNotExact.isEmpty();
  }

  @Nonnull
  private static SplitHunk copySplitHunk(@Nonnull SplitHunk hunk, @Nonnull List<String> contextAfter, @Nonnull List<String> contextBefore) {
    ArrayList<BeforeAfter<List<String>>> steps = new ArrayList<>();
    for (BeforeAfter<List<String>> step : hunk.getPatchSteps()) {
      steps.add(new BeforeAfter<>(new ArrayList<>(step.getBefore()), new ArrayList<>(step.getAfter())));
    }
    return new SplitHunk(hunk.getStartLineBefore(), hunk.getStartLineAfter(), steps, new ArrayList<>(contextAfter),
                         new ArrayList<>(contextBefore));
  }

  private static SplitHunk createWithAllContextCopy(SplitHunk hunk) {
    SplitHunk copy = copySplitHunk(hunk, new ArrayList<>(), new ArrayList<>());

    List<BeforeAfter<List<String>>> steps = copy.getPatchSteps();
    if (steps.isEmpty()) {
      int contextSize = hunk.getContextBefore().size() + hunk.getContextAfter().size();
      LOG.warn(constructHunkWarnMessage(hunk.getStartLineBefore(), hunk.getStartLineAfter(), contextSize, contextSize));
      StringBuilder sb = new StringBuilder();
      StringUtil.join(hunk.getContextBefore(), "\n", sb);
      StringUtil.join(hunk.getContextAfter(), "\n", sb);
      LOG.debug(sb.toString());
      return copy;
    }
    BeforeAfter<List<String>> first = steps.get(0);
    int lastStepIndex = steps.size() - 1;
    BeforeAfter<List<String>> last = steps.get(lastStepIndex);

    BeforeAfter<List<String>> firstCopy = copyBeforeAfter(first);
    steps.set(0, firstCopy);
    firstCopy.getBefore().addAll(0, hunk.getContextBefore());
    firstCopy.getAfter().addAll(0, hunk.getContextBefore());

    if (first == last) {
      firstCopy.getBefore().addAll(hunk.getContextAfter());
      firstCopy.getAfter().addAll(hunk.getContextAfter());
    } else {
      BeforeAfter<List<String>> lastCopy = copyBeforeAfter(last);
      lastCopy.getBefore().addAll(hunk.getContextAfter());
      lastCopy.getAfter().addAll(hunk.getContextAfter());
      steps.set(lastStepIndex, lastCopy);
    }
    return copy;
  }

  @Nonnull
  private static String constructHunkWarnMessage(int startLineBefore, int startLineAfter, int sizeBefore, int sizeAfter) {
    return String.format("Can't detect hunk modification lines for: -%d,%d +%d,%d", startLineBefore, sizeBefore,
                         startLineAfter, sizeAfter);
  }

  private static BeforeAfter<List<String>> copyBeforeAfter(BeforeAfter<List<String>> first) {
    return new BeforeAfter<>(new ArrayList<>(first.getBefore()), new ArrayList<>(first.getAfter()));
  }

  private static void complementInsertAndDelete(SplitHunk hunk) {
    List<BeforeAfter<List<String>>> steps = hunk.getPatchSteps();
    BeforeAfter<List<String>> first = steps.get(0);
    BeforeAfter<List<String>> last = steps.get(steps.size() - 1);
    boolean complementFirst = first.getBefore().isEmpty() || first.getAfter().isEmpty();
    boolean complementLast = last.getBefore().isEmpty() || last.getAfter().isEmpty();

    List<String> contextBefore = hunk.getContextBefore();
    if (complementFirst && ! contextBefore.isEmpty()) {
      String firstContext = contextBefore.get(contextBefore.size() - 1);
      first.getBefore().add(0, firstContext);
      first.getAfter().add(0, firstContext);
      contextBefore.remove(contextBefore.size() - 1);
    }
    List<String> contextAfter = hunk.getContextAfter();
    if (complementLast && ! contextAfter.isEmpty()) {
      String firstContext = contextAfter.get(0);
      last.getBefore().add(firstContext);
      last.getAfter().add(firstContext);
      contextAfter.remove(0);
    }
  }

  private static boolean complementIfShort(SplitHunk hunk) {
    List<BeforeAfter<List<String>>> steps = hunk.getPatchSteps();
    if (steps.size() > 1) return false;
    BeforeAfter<List<String>> first = steps.get(0);
    boolean complementFirst = first.getBefore().isEmpty() || first.getAfter().isEmpty() ||
                                    first.getBefore().size() == 1 || first.getAfter().size() == 1;
    if (! complementFirst) return false;

    List<String> contextBefore = hunk.getContextBefore();
    if (! contextBefore.isEmpty()) {
      String firstContext = contextBefore.get(contextBefore.size() - 1);
      first.getBefore().add(0, firstContext);
      first.getAfter().add(0, firstContext);
      contextBefore.remove(contextBefore.size() - 1);
      return true;
    }
    List<String> contextAfter = hunk.getContextAfter();
    if (! contextAfter.isEmpty()) {
      String firstContext = contextAfter.get(0);
      first.getBefore().add(firstContext);
      first.getAfter().add(firstContext);
      contextAfter.remove(0);
      return true;
    }
    return false;
  }

  // applies in a way that patch _can_ be solved manually even in the case of total mismatch
  public void trySolveSomehow() {
    assert !myNotExact.isEmpty();
    for (Iterator<SplitHunk> iterator = myNotExact.iterator(); iterator.hasNext(); ) {
      SplitHunk hunk = iterator.next();
      hunk.cutSameTail();
      if (!testForPartialContextMatch(hunk, new LongTryMismatchSolver(hunk), ourMaxWalk, null)) {
        if (complementIfShort(hunk)) {
          if (!testForPartialContextMatch(hunk, new LongTryMismatchSolver(hunk), ourMaxWalk, null)) {
            myNotBound.add(hunk);
          }
        }
        else {
          myNotBound.add(hunk);
        }
      }
    }
    Collections.sort(myNotBound, HunksComparator.getInstance());
    myNotExact.clear();
  }

  private boolean testForPartialContextMatch(SplitHunk splitHunkWithExtendedContext,
                                             MismatchSolver mismatchSolver,
                                             int maxWalkFromBinding, @Nullable SplitHunk originalSplitHunk) {
    List<BeforeAfter<List<String>>> steps = splitHunkWithExtendedContext.getPatchSteps();
    BetterPoint betterPoint = new BetterPoint();

    if (splitHunkWithExtendedContext.isInsertion()) return false;

    // if it is not just insertion, then in first step will be both parts
    Iterator<FirstLineDescriptor> iterator = mismatchSolver.getStartLineVariationsIterator();
    while (iterator.hasNext() && (betterPoint.getPoint() == null || !betterPoint.getPoint().idealFound())) {
      FirstLineDescriptor descriptor = iterator.next();

      Iterator<Integer> matchingIterator =
              getMatchingIterator(descriptor.getLine(), splitHunkWithExtendedContext.getStartLineBefore() + descriptor.getOffset(),
                                  maxWalkFromBinding);
      while (matchingIterator.hasNext() && (betterPoint.getPoint() == null || !betterPoint.getPoint().idealFound())) {
        Integer lineNumber = matchingIterator.next();

        // go back and forward from point
        List<BeforeAfter<List<String>>> patchSteps = splitHunkWithExtendedContext.getPatchSteps();
        BeforeAfter<List<String>> step = patchSteps.get(descriptor.getStepNumber());
        FragmentResult fragmentResult = checkFragmented(lineNumber, descriptor.getOffsetInStep(), step, descriptor.isIsInBefore());

        // we go back - if middle fragment ok
        if (descriptor.getStepNumber() > 0 && fragmentResult.isStartAtEdge()) {
          // not including step number here
          List<BeforeAfter<List<String>>> list = Collections.unmodifiableList(patchSteps.subList(0, descriptor.getStepNumber()));
          int offsetForStart = - descriptor.getOffsetInStep() - 1;
          SequentialStepsChecker backChecker = new SequentialStepsChecker(lineNumber + offsetForStart, false);
          backChecker.go(list);

          fragmentResult.setContainAlreadyApplied(fragmentResult.isContainAlreadyApplied() || backChecker.isUsesAlreadyApplied());
          fragmentResult.setStart(fragmentResult.getStart() - backChecker.getSizeOfFragmentToBeReplaced());
          fragmentResult.addDistance(backChecker.getDistance());
          fragmentResult.setStartAtEdge(backChecker.getDistance() == 0);
        }

        if (steps.size() > descriptor.getStepNumber() + 1 && fragmentResult.isEndAtEdge()) {
          // forward
          List<BeforeAfter<List<String>>> list = Collections.unmodifiableList(patchSteps.subList(descriptor.getStepNumber() + 1, patchSteps.size()));
          if (! list.isEmpty()) {
            SequentialStepsChecker checker = new SequentialStepsChecker(fragmentResult.getEnd() + 1, true);
            checker.go(list);

            fragmentResult.setContainAlreadyApplied(fragmentResult.isContainAlreadyApplied() || checker.isUsesAlreadyApplied());
            fragmentResult.setEnd(fragmentResult.getEnd() + checker.getSizeOfFragmentToBeReplaced());
            fragmentResult.addDistance(checker.getDistance());
            fragmentResult.setEndAtEdge(checker.getDistance() == 0);
          }
        }

        TextRange textRangeInOldDocument = new UnfairTextRange(fragmentResult.getStart(), fragmentResult.getEnd());
        // ignore too short fragments
        //if (pointCanBeUsed(textRangeInOldDocument) && (! mismatchSolver.isAllowMismatch() || fragmentResult.getEnd() - fragmentResult.getStart() > 1)) {
        if (pointCanBeUsed(textRangeInOldDocument)) {
          int distance = fragmentResult.myDistance;
          int commonPart = fragmentResult.getEnd() - fragmentResult.getStart() + 1;
          int contextDistance = 0;
          if (distance == 0 || commonPart < 2) {
            int distanceBack = getDistanceBack(fragmentResult.getStart() - 1, splitHunkWithExtendedContext.getContextBefore());
            int distanceInContextAfter = getDistance(fragmentResult.getEnd() + 1, splitHunkWithExtendedContext.getContextAfter());
            contextDistance = distanceBack + distanceInContextAfter;
          }
          betterPoint.feed(new Point(distance, textRangeInOldDocument, fragmentResult.isContainAlreadyApplied(), contextDistance, commonPart));
        }
      }
    }
    Point pointPoint = betterPoint.getPoint();
    if (pointPoint == null) return false;
    if (! mismatchSolver.isAllowMismatch()) {
      if (pointPoint.getDistance() > 0) return false;
      if (pointPoint.myCommon < 2) {
        int contextCommon =
                splitHunkWithExtendedContext.getContextBefore().size() + splitHunkWithExtendedContext.getContextAfter().size() -
                pointPoint.myContextDistance;
        if (contextCommon == 0) return false;
      }
    }
    putCutIntoTransformations(pointPoint.getInOldDocument(), originalSplitHunk,
                              new MyAppliedData(splitHunkWithExtendedContext.getAfterAll(), pointPoint.myUsesAlreadyApplied,
                                                false, pointPoint.getDistance() == 0, ChangeType.REPLACE),
                              originalSplitHunk == null ? EMPTY_OFFSET
                                                        : new IntPair(
                                                                originalSplitHunk.getContextBefore().size() -
                                                                splitHunkWithExtendedContext.getContextBefore().size(),
                                                                originalSplitHunk.getContextAfter().size() - splitHunkWithExtendedContext.getContextAfter().size()));
    return true;
  }

  private FragmentResult checkFragmented(int lineInTheMiddle, int offsetInStep, BeforeAfter<List<String>> step, boolean inBefore) {
    List<String> lines = inBefore ? step.getBefore() : step.getAfter();
    List<String> start = lines.subList(0, offsetInStep);
    int startDistance = 0;
    if (! start.isEmpty()) {
      if (lineInTheMiddle - 1 < 0) {
        startDistance = start.size();
      } else {
        startDistance = getDistanceBack(lineInTheMiddle - 1, start);
      }
    }
    List<String> end = lines.subList(offsetInStep, lines.size());
    int endDistance = 0;
    if (! end.isEmpty()) {
      endDistance = getDistance(lineInTheMiddle, end);
    }
    FragmentResult fragmentResult =
            new FragmentResult(lineInTheMiddle - (start.size() - startDistance), lineInTheMiddle + (end.size() - endDistance) - 1, !inBefore);
    fragmentResult.addDistance(startDistance + endDistance);
    fragmentResult.setStartAtEdge(startDistance == 0);
    fragmentResult.setEndAtEdge(endDistance == 0);
    return fragmentResult;
  }

  @Nonnull
  public List<AppliedTextPatch.AppliedSplitPatchHunk> getAppliedInfo() {
    return myAppliedInfo;
  }

  private static class FragmentResult {
    private int myStart;
    private int myEnd;
    private boolean myContainAlreadyApplied;
    private int myDistance;

    private boolean myStartAtEdge;
    private boolean myEndAtEdge;

    private FragmentResult(int start, int end, boolean containAlreadyApplied) {
      myStart = start;
      myEnd = end;
      myContainAlreadyApplied = containAlreadyApplied;
      myDistance = 0;
    }

    public boolean isStartAtEdge() {
      return myStartAtEdge;
    }

    public void setStartAtEdge(boolean startAtEdge) {
      myStartAtEdge = startAtEdge;
    }

    public boolean isEndAtEdge() {
      return myEndAtEdge;
    }

    public void setEndAtEdge(boolean endAtEdge) {
      myEndAtEdge = endAtEdge;
    }

    public void addDistance(int distance) {
      myDistance += distance;
    }

    public int getStart() {
      return myStart;
    }

    public int getEnd() {
      return myEnd;
    }

    public boolean isContainAlreadyApplied() {
      return myContainAlreadyApplied;
    }

    public void setStart(int start) {
      myStart = start;
    }

    public void setEnd(int end) {
      myEnd = end;
    }

    public void setContainAlreadyApplied(boolean containAlreadyApplied) {
      myContainAlreadyApplied = containAlreadyApplied;
    }
  }

  private int getDistanceBack(int idxStart, List<String> lines) {
    if (idxStart < 0) return lines.size();
    int cnt = lines.size() - 1;
    for (int i = idxStart; i >= 0 && cnt >= 0; i--, cnt--) {
      if (! myLines.get(i).equals(lines.get(cnt))) return (cnt + 1);
    }
    return cnt + 1;
  }

  private int getDistance(int idxStart, List<String> lines) {
    if (idxStart >= myLines.size()) return lines.size();
    int cnt = 0;
    for (int i = idxStart; i < myLines.size() && cnt < lines.size(); i++, cnt++) {
      if (! myLines.get(i).equals(lines.get(cnt))) return (lines.size() - cnt);
    }
    return lines.size() - cnt;
  }

  public void putCutIntoTransformations(TextRange range, MyAppliedData value) {
    putCutIntoTransformations(range, null, value, EMPTY_OFFSET);
  }

  public void putCutIntoTransformations(TextRange range,
                                        @Nullable SplitHunk splitHunk,
                                        MyAppliedData value,
                                        @Nonnull IntPair contextOffsetInPatchSteps) {
    // cut last lines but not the very first
    List<String> list = value.getList();
    //last line should be taken from includeConsumer even it seems to be equal with base context line( they can differ with line separator)
    boolean eofHunkAndLastLineShouldBeChanged =
            containsLastLine(range) && splitHunk != null && splitHunk.getContextAfter().isEmpty() && !splitHunk.getAfterAll().isEmpty();
    int cnt = list.size() - 1;
    int i = range.getEndOffset();
    if (!eofHunkAndLastLineShouldBeChanged) {
      for (; i > range.getStartOffset() && cnt >= 0; i--, cnt--) {
        if (!list.get(cnt).equals(myLines.get(i))) {
          break;
        }
      }
    }
    int endSize = list.size();
    if (cnt + 1 <= list.size() - 1) {
      endSize = cnt + 1;
    }
    int cntStart = 0;
    int j = range.getStartOffset();

    if (endSize > 0) {
      int lastProcessedIndex = eofHunkAndLastLineShouldBeChanged ? list.size() - 1 : list.size();
      for (; j < range.getEndOffset() && cntStart < lastProcessedIndex; j++, cntStart++) {
        if (!list.get(cntStart).equals(myLines.get(j))) {
          break;
        }
      }
    }

    if (j != range.getStartOffset() || i != range.getEndOffset()) {
      if (cntStart >= endSize) {
        // +1 since we set end index inclusively
        if (list.size() == (range.getLength() + 1)) {
          // for already applied
          myHadAlreadyAppliedMet = value.isHaveAlreadyApplied();
          processAppliedInfo(splitHunk, range, contextOffsetInPatchSteps,
                             AppliedTextPatch.HunkStatus.ALREADY_APPLIED);
        }
        else {
          // deletion
          UnfairTextRange textRange = new UnfairTextRange(j, i + (cntStart - endSize));
          myTransformations.put(textRange, new MyAppliedData(Collections.emptyList(), value.isHaveAlreadyApplied(),
                                                             value.isPlaceCoinside(), value.isChangedCoinside(), value.myChangeType));
          processAppliedInfo(splitHunk, range, contextOffsetInPatchSteps, AppliedTextPatch.HunkStatus.EXACTLY_APPLIED);
        }
      }
      else {
        if (i < j) {
          // insert case
          // just took one line
          assert cntStart > 0;
          MyAppliedData newData =
                  new MyAppliedData(new ArrayList<>(list.subList(cntStart - (j - i), endSize)), value.isHaveAlreadyApplied(),
                                    value.isPlaceCoinside(),
                                    value.isChangedCoinside(), value.myChangeType);
          TextRange newRange = new TextRange(i, i);
          myTransformations.put(newRange, newData);
          processAppliedInfo(splitHunk, range, contextOffsetInPatchSteps, AppliedTextPatch.HunkStatus.EXACTLY_APPLIED);
          return;
        }
        MyAppliedData newData =
                new MyAppliedData(new ArrayList<>(list.subList(cntStart, endSize)), value.isHaveAlreadyApplied(), value.isPlaceCoinside(),
                                  value.isChangedCoinside(), value.myChangeType);
        TextRange newRange = new TextRange(j, i);
        myTransformations.put(newRange, newData);
        processAppliedInfo(splitHunk, range, contextOffsetInPatchSteps, AppliedTextPatch.HunkStatus.EXACTLY_APPLIED);
      }
    }
    else {
      myTransformations.put(range, value);
      processAppliedInfo(splitHunk, range, contextOffsetInPatchSteps, AppliedTextPatch.HunkStatus.EXACTLY_APPLIED);
    }
  }


  private void processAppliedInfoForUnApplied(@Nonnull SplitHunk original) {
    myAppliedInfo.add(new AppliedTextPatch.AppliedSplitPatchHunk(original, -1, -1, AppliedTextPatch.HunkStatus.NOT_APPLIED));
  }

  private void processAppliedInfo(@Nullable SplitHunk hunk, @Nonnull TextRange lineWithPartContextApplied,
                                  @Nonnull IntPair contextRangeShift,
                                  AppliedTextPatch.HunkStatus hunkStatus) {
    if (hunk != null) {
      // +1 to the end  because end range is always not included -> [i;j); except add modification;
      int newStart = lineWithPartContextApplied.getStartOffset() + contextRangeShift.val1;
      int newEnd = hunk.isInsertion() && hunkStatus != AppliedTextPatch.HunkStatus.ALREADY_APPLIED
                   ? newStart : lineWithPartContextApplied.getEndOffset() + 1 - contextRangeShift.val2;
      myAppliedInfo.add(new AppliedTextPatch.AppliedSplitPatchHunk(hunk, newStart, newEnd, hunkStatus));
    }
  }

  private boolean pointCanBeUsed(TextRange range) {
    // we check with offset only one
    Map.Entry<TextRange, MyAppliedData> entry = myTransformations.ceilingEntry(range);
    if (entry != null && entry.getKey().intersects(range)) {
      return false;
    }
    return true;
  }

  private static class BetterPoint {
    private Point myPoint;

    public void feed(@Nonnull Point point) {
      if (myPoint == null || point.meBetter(myPoint)) {
        myPoint = point;
      }
    }

    public Point getPoint() {
      return myPoint;
    }
  }

  private static class Point {
    private int myDistance;
    private int myContextDistance;
    private int myCommon;
    private final boolean myUsesAlreadyApplied;
    private TextRange myInOldDocument;

    private Point(int distance, TextRange inOldDocument, boolean usesAlreadyApplied, int contextDistance, int common) {
      myDistance = distance;
      myInOldDocument = inOldDocument;
      myUsesAlreadyApplied = usesAlreadyApplied;
      myContextDistance = contextDistance;
      myCommon = common;
    }

    public boolean meBetter(Point maxPoint) {
      if (myCommon <= 1 && maxPoint.myCommon > 1) return false;
      if (maxPoint.myCommon <= 1 && myCommon > 1) return true;

      return (myDistance < maxPoint.getDistance()) || (myDistance == 0 && maxPoint.getDistance() == 0 && myContextDistance < maxPoint.myContextDistance);
    }

    public int getDistance() {
      return myDistance;
    }

    public boolean idealFound() {
      return myDistance == 0 && myContextDistance == 0;
    }

    public TextRange getInOldDocument() {
      return myInOldDocument;
    }
  }

  private class SequentialStepsChecker implements SequenceDescriptor {
    private int myDistance;
    // in the end, will be [excluding] end of changing interval
    private int myIdx;
    private int myStartIdx;
    private final boolean myForward;
    private boolean myUsesAlreadyApplied;

    private SequentialStepsChecker(int lineNumber, boolean forward) {
      myStartIdx = lineNumber;
      myIdx = lineNumber;
      myForward = forward;
    }

    public boolean isUsesAlreadyApplied() {
      return myUsesAlreadyApplied;
    }

    @Override
    public int getDistance() {
      return myDistance;
    }

    public void go(List<BeforeAfter<List<String>>> steps) {
      Consumer<BeforeAfter<List<String>>> stepConsumer = listBeforeAfter -> {
        if (myDistance == 0) {
          // until this point, it all had being doing well
          if (listBeforeAfter.getBefore().isEmpty()) {
            // just take it
            return;
          }
          FragmentMatcher fragmentMatcher = new FragmentMatcher(myIdx, listBeforeAfter);
          Pair<Integer, Boolean> pair = fragmentMatcher.find(true);
          myDistance = pair.getFirst();
          if (myDistance == 0) {
            myIdx += pair.getSecond() ? listBeforeAfter.getBefore().size() : listBeforeAfter.getAfter().size();
          }
          else {
            myIdx += (pair.getSecond() ? listBeforeAfter.getBefore().size() : listBeforeAfter.getAfter().size()) - pair.getFirst();
          }
          myUsesAlreadyApplied = !pair.getSecond();
        }
        else {
          // we just count the distance
          myDistance += listBeforeAfter.getBefore().size();
        }
      };

      if (myForward) {
        for (BeforeAfter<List<String>> step : steps) {
          stepConsumer.accept(step);
        }
      } else {
        for (int i = steps.size() - 1; i >= 0; i--) {
          BeforeAfter<List<String>> step = steps.get(i);
          stepConsumer.accept(step);
        }
      }
    }

    @Override
    public int getSizeOfFragmentToBeReplaced() {
      return myForward ? (myIdx - myStartIdx) : (myStartIdx - myIdx);
    }
  }

  private static class FirstLineDescriptor {
    private final String myLine;
    private final int myOffset;
    private final int myStepNumber;
    private final int myOffsetInStep;
    private final boolean myIsInBefore;

    private FirstLineDescriptor(String line, int offset, int stepNumber, int offsetInStep, boolean isInBefore) {
      myLine = line;
      myOffset = offset;
      myStepNumber = stepNumber;
      myOffsetInStep = offsetInStep;
      myIsInBefore = isInBefore;
    }

    public String getLine() {
      return myLine;
    }

    public int getOffset() {
      return myOffset;
    }

    public int getStepNumber() {
      return myStepNumber;
    }

    public int getOffsetInStep() {
      return myOffsetInStep;
    }

    public boolean isIsInBefore() {
      return myIsInBefore;
    }
  }

  private static class ExactMatchSolver extends MismatchSolver {
    private ExactMatchSolver(SplitHunk hunk) {
      super(false);
      List<BeforeAfter<List<String>>> steps = hunk.getPatchSteps();
      BeforeAfter<List<String>> first = steps.get(0);
      if (steps.size() == 1 && first.getBefore().isEmpty()) {
        myResult.add(new FirstLineDescriptor(first.getBefore().get(0), 0, 0,0,true));
      }
      if (! first.getBefore().isEmpty()) {
        myResult.add(new FirstLineDescriptor(first.getBefore().get(0), 0, 0,0,true));
      }
      if (! first.getAfter().isEmpty()) {
        myResult.add(new FirstLineDescriptor(first.getAfter().get(0), 0, 0,0,false));
      }
      assert ! myResult.isEmpty();
    }
  }

  public static class LongTryMismatchSolver extends MismatchSolver {
    // let it be 3 first steps plus 2 lines as an attempt
    public LongTryMismatchSolver(SplitHunk hunk) {
      super(true);
      List<BeforeAfter<List<String>>> steps = hunk.getPatchSteps();
      int beforeOffset = 0;
      int afterOffset = 0;
      for (int i = 0; i < steps.size() && i < 3; i++) {
        BeforeAfter<List<String>> list = steps.get(i);
        List<String> before = list.getBefore();
        for (int j = 0; j < before.size() && j < 2; j++) {
          String s = before.get(j);
          myResult.add(new FirstLineDescriptor(s, beforeOffset + j, i, j, true));
        }
        List<String> after = list.getAfter();
        for (int j = 0; j < after.size() && j < 2; j++) {
          String s = after.get(j);
          myResult.add(new FirstLineDescriptor(s, afterOffset + j, i, j, false));
        }
        beforeOffset += before.size();
        afterOffset += after.size();
      }
    }
  }

  private abstract static class MismatchSolver {
    protected final ArrayList<FirstLineDescriptor> myResult;
    private final boolean myAllowMismatch;

    protected MismatchSolver(boolean allowMismatch) {
      myResult = new ArrayList<>();
      myAllowMismatch = allowMismatch;
    }

    public Iterator<FirstLineDescriptor> getStartLineVariationsIterator() {
      return myResult.iterator();
    }

    public boolean isAllowMismatch() {
      return myAllowMismatch;
    }
  }

  private Iterator<Integer> getMatchingIterator(String line, int originalStart, int maxWalkFromBinding) {
    return ContainerUtil.concatIterators(new WalkingIterator(line, originalStart, maxWalkFromBinding, true),
                                         new WalkingIterator(line, originalStart, maxWalkFromBinding, false));
  }

  private class WalkingIterator implements Iterator<Integer> {
    private final String myLine;
    // true = down
    private final boolean myDirection;

    private int myLeftWalk;
    // == -1 when end
    private int myCurrentIdx;

    private WalkingIterator(String line, int start, int leftWalk, boolean direction) {
      myLine = line;
      myLeftWalk = leftWalk;

      myDirection = direction;
      myCurrentIdx = start - 1;
      step();
    }

    @Override
    public boolean hasNext() {
      return myCurrentIdx != -1;
    }

    @Override
    public Integer next() {
      int currentIdx = myCurrentIdx;
      step();
      return currentIdx;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    public void step() {
      if (myDirection) {
        int i = myCurrentIdx + 1;
        int maxWalk = myLeftWalk + i;
        myCurrentIdx = -1;
        for (; i < myLines.size() && i < maxWalk; i++) {
          String s = myLines.get(i);
          if (myLine.equals(s) && ! isSeized(i)) {
            myCurrentIdx = i;
            myLeftWalk = maxWalk - 1 - i;
            break;
          }
        }
      } else {
        int i = myCurrentIdx;
        int maxWalk = Math.max(-1, i - myLeftWalk);
        myCurrentIdx = -1;
        for (; i >= 0 && i > maxWalk && i < myLines.size(); i--) {
          String s = myLines.get(i);
          if (myLine.equals(s) && ! isSeized(i)) {
            myCurrentIdx = i;
            myLeftWalk = i - (maxWalk + 1); // todo +-
            break;
          }
        }
      }
    }

    private boolean isSeized(int lineNumber) {
      TextRange art = new TextRange(lineNumber, lineNumber);
      TextRange floor = myTransformations.floorKey(art);
      return (floor != null && floor.intersects(art));
    }
  }

  private boolean testForExactMatch(SplitHunk splitHunk, SplitHunk originalHunk) {
    int offset = splitHunk.getContextBefore().size();
    List<BeforeAfter<List<String>>> steps = splitHunk.getPatchSteps();
    if (splitHunk.isInsertion()) {
      boolean emptyFile = myLines.isEmpty() || myLines.size() == 1 && myLines.get(0).trim().length() == 0;
      if (emptyFile) {
        myNotBound.add(splitHunk);
      }
      return emptyFile;
    }

    int idx = splitHunk.getStartLineBefore() + offset;
    int cnt = 0;
    boolean hadAlreadyApplied = false;
    for (BeforeAfter<List<String>> step : steps) {
      if (myLines.size() <= idx) return false;
      if (step.getBefore().isEmpty()) continue; // can occur only in the end

      Pair<Integer, Boolean> distance = new FragmentMatcher(idx + cnt, step).find(false);
      if (distance.getFirst() > 0) {
        return false;
      }
      // fits!
      int length;
      if (distance.getSecond()) {
        length = step.getBefore().size();
      }
      else {
        length = step.getAfter().size();
        hadAlreadyApplied = true;
      }
      cnt += length;
      //idx += length - 1;
    }
    putCutIntoTransformations(new TextRange(idx, idx + cnt - 1), originalHunk,
                              new MyAppliedData(splitHunk.getAfterAll(), hadAlreadyApplied, true, true, ChangeType.REPLACE), new IntPair(
            originalHunk.getContextBefore().size() - splitHunk.getContextBefore().size(),
            originalHunk.getContextAfter().size() - splitHunk.getContextAfter().size()));
    return true;
  }

  // will not find consider fragments that intersect
  private class FragmentMatcher {
    private int myIdx;
    private int myOffsetIdxInHunk;
    // if we set index in hunk != 0, then we will check only one side
    private Boolean myBeforeSide;
    private boolean myIsInBefore;
    private int myIdxInHunk;
    private final BeforeAfter<List<String>> myBeforeAfter;

    private FragmentMatcher(int idx, BeforeAfter<List<String>> beforeAfter) {
      myOffsetIdxInHunk = 0;
      myIdx = idx;
      myBeforeAfter = beforeAfter;
      myIdxInHunk = 0;
      //myBeforeSide = true;
    }

    public void setSideAndIdx(int startInHunk, boolean beforeSide) {
      myOffsetIdxInHunk = startInHunk;
      myBeforeSide = beforeSide;
      if (myBeforeSide) {
        assert myBeforeAfter.getBefore().size() > myOffsetIdxInHunk || (myOffsetIdxInHunk == 0 && myBeforeAfter.getBefore().size() == 0);
      } else {
        assert myBeforeAfter.getAfter().size() > myOffsetIdxInHunk || (myOffsetIdxInHunk == 0 && myBeforeAfter.getAfter().size() == 0);
      }
    }

    // true = before
    public Pair<Integer, Boolean> find(boolean canMismatch) {
      if (myBeforeSide != null) {
        if (myBeforeSide) {
          // check only one side
          return new Pair<>(checkSide(myBeforeAfter.getBefore(), canMismatch), true);
        } else {
          // check only one side
          return new Pair<>(checkSide(myBeforeAfter.getAfter(), canMismatch), false);
        }
      } else {
        int beforeCheckResult = checkSide(myBeforeAfter.getBefore(), canMismatch);
        int afterCheckResult = checkSide(myBeforeAfter.getAfter(), canMismatch);

        Pair<Integer, Boolean> beforePair = new Pair<>(beforeCheckResult, true);
        Pair<Integer, Boolean> afterPair = new Pair<>(afterCheckResult, false);
        if (! canMismatch) {
          if (beforeCheckResult == 0) {
            return beforePair;
          }
          if (afterCheckResult == 0) {
            return afterPair;
          }
          return beforePair;
        }

        // take longer coinsiding
        int beforeCommon = myBeforeAfter.getBefore().size() - beforeCheckResult;
        int afterCommon = myBeforeAfter.getAfter().size() - afterCheckResult;
        if (beforeCommon > 0 && afterCommon > 0) {
          // ignore insertion and deletion cases -> too few context -> if we have alternative
          if (beforeCommon == 1 && myBeforeAfter.getBefore().size() == 1 && afterCommon > 1) {
            return afterPair;
          }
          // ignore insertion and deletion cases -> too few context -> if we have alternative
          if (afterCommon == 1 && myBeforeAfter.getAfter().size() == 1 && beforeCommon > 1) {
            return beforePair;
          }
          if (beforeCommon >= afterCommon) {
            return beforePair;
          }
          return afterPair;
        }
        if (afterCommon > 0) {
          return afterPair;
        }
        return beforePair;
      }
    }

    private int checkSide(List<String> side, boolean canMismatch) {
      int distance = 0;
      if (myOffsetIdxInHunk > 0) {
        int linesIdx = myIdx - 1;
        int i = myOffsetIdxInHunk - 1;
        for (; i >= 0 && linesIdx >= 0; i--, linesIdx--) {
          if (! myLines.get(linesIdx).equals(side.get(i))) {
            break;
          }
        }
        // since ok -> === -1
        i += 1;
        if (i > 0 && ! canMismatch) return i; // don't go too deep if we need just == or > 0
        distance = i;
      }
      int linesEndIdx = myIdx;
      int j = myOffsetIdxInHunk;
      for (; j < side.size() && linesEndIdx < myLines.size(); j++, linesEndIdx++) {
        if (! myLines.get(linesEndIdx).equals(side.get(j))) {
          break;
        }
      }
      distance += side.size() - j;
      return distance;
    }
  }

  public String getAfter() {
    StringBuilder sb = new StringBuilder();
    // put not bind into the beginning
    for (SplitHunk hunk : myNotBound) {
      linesToSb(sb, hunk.getAfterAll(), true);
    }
    iterateTransformations(range -> {
      List<String> baseLineslist = myLines.subList(range.getStartOffset(), range.getEndOffset() + 1);
      boolean withLineBreak = !containsLastLine(range) || myBaseFileEndsWithNewLine;
      linesToSb(sb, baseLineslist, withLineBreak);
    }, range -> {
      MyAppliedData appliedData = myTransformations.get(range);
      List<String> list = appliedData.getList();
      boolean withLineBreak = !containsLastLine(range) || !mySuppressNewLineInEnd;
      linesToSb(sb, list, withLineBreak);
    });
    return sb.toString();
  }

  private boolean containsLastLine(@Nonnull TextRange range) {
    return range.getEndOffset() == myLines.size() - 1;
  }

  private static void linesToSb(StringBuilder sb, List<String> list, boolean withEndLineBreak) {
    StringUtil.join(list, "\n", sb);
    if (!list.isEmpty() && withEndLineBreak) {
      sb.append('\n');
    }
  }

  // indexes are passed inclusive
  private void iterateTransformations(Consumer<TextRange> consumerExcluded, Consumer<TextRange> consumerIncluded) {
    if (myTransformations.isEmpty()) {
      consumerExcluded.accept(new UnfairTextRange(0, myLines.size() - 1));
    } else {
      Set<Map.Entry<TextRange,MyAppliedData>> entries = myTransformations.entrySet();
      Iterator<Map.Entry<TextRange, MyAppliedData>> iterator = entries.iterator();
      assert iterator.hasNext();

      Map.Entry<TextRange, MyAppliedData> first = iterator.next();
      TextRange range = first.getKey();
      if (range.getStartOffset() > 0) {
        consumerExcluded.accept(new TextRange(0, range.getStartOffset() - 1));
      }
      consumerIncluded.accept(range);

      int previousEnd = range.getEndOffset() + 1;
      while (iterator.hasNext() && previousEnd < myLines.size()) {
        Map.Entry<TextRange, MyAppliedData> entry = iterator.next();
        TextRange key = entry.getKey();
        consumerExcluded.accept(new UnfairTextRange(previousEnd, key.getStartOffset() - 1));
        consumerIncluded.accept(key);
        previousEnd = key.getEndOffset() + 1;
      }
      if (previousEnd < myLines.size()) {
        consumerExcluded.accept(new TextRange(previousEnd, myLines.size() - 1));
      }
    }
  }

  public static class SplitHunk {
    private final List<String> myContextBefore;
    private final List<String> myContextAfter;
    @Nonnull
    private final List<BeforeAfter<List<String>>> myPatchSteps;
    private final int myStartLineBefore;
    private final int myStartLineAfter;

    public SplitHunk(int startLineBefore, int startLineAfter,
                     @Nonnull List<BeforeAfter<List<String>>> patchSteps,
                     List<String> contextAfter,
                     List<String> contextBefore) {
      myStartLineBefore = startLineBefore;
      myStartLineAfter = startLineAfter;
      myPatchSteps = patchSteps;
      myContextAfter = contextAfter;
      myContextBefore = contextBefore;
    }

    // todo
    public void cutSameTail() {
      BeforeAfter<List<String>> lastStep = myPatchSteps.get(myPatchSteps.size() - 1);
      List<String> before = lastStep.getBefore();
      List<String> after = lastStep.getAfter();
      int cntBefore = before.size() - 1;
      int cntAfter = after.size() - 1;

      for (; cntBefore >= 0 && cntAfter > 0; cntBefore--, cntAfter--) {
        if (! before.get(cntBefore).equals(after.get(cntAfter))) break;
      }

      // typically only 1 line
      int cutSame = before.size() - 1 - cntBefore;
      for (int i = 0; i < cutSame; i++) {
        before.remove(before.size() - 1);
        after.remove(after.size() - 1);
      }
    }

    public static List<SplitHunk> read(PatchHunk hunk) {
      List<SplitHunk> result = new ArrayList<>();
      List<PatchLine> lines = hunk.getLines();
      int i = 0;

      List<String> contextBefore = new ArrayList<>();
      int newSize = 0;
      int oldSize = 0;
      while (i < lines.size()) {
        int inheritedContext = contextBefore.size();
        List<String> contextAfter = new ArrayList<>();
        List<BeforeAfter<List<String>>> steps = new ArrayList<>();
        int endIdx = readOne(lines, contextBefore, contextAfter, steps, i);
        int startLineBefore = hunk.getStartLineBefore();
        int startLineAfter = hunk.getStartLineAfter();
        if (steps.isEmpty()) {
          // skip empty chunk, but warn
          LOG.warn(constructHunkWarnMessage(startLineBefore, startLineAfter, hunk.getEndLineBefore() - startLineBefore,
                                            hunk.getEndLineAfter() - startLineAfter));
          LOG.debug("Wrong chunk text: " + hunk.getText());
        }
        else {
          result.add(new SplitHunk(startLineBefore + i - inheritedContext - newSize,
                                   startLineAfter + i - inheritedContext - oldSize, steps, contextAfter, contextBefore));
        }
        for (BeforeAfter<List<String>> step : steps) {
          newSize += step.getAfter().size();
          oldSize += step.getBefore().size();
        }
        i = endIdx;
        if (i < lines.size()) {
          contextBefore = new ArrayList<>();
          contextBefore.addAll(contextAfter);
        }
      }
      return result;
    }

    private static int readOne(List<PatchLine> lines, List<String> contextBefore, List<String> contextAfter,
                               List<BeforeAfter<List<String>>> steps, int startI) {
      int i = startI;
      for (; i < lines.size(); i++) {
        PatchLine patchLine = lines.get(i);
        if (! PatchLine.Type.CONTEXT.equals(patchLine.getType())) break;
        contextBefore.add(patchLine.getText());
      }

      boolean addFirst = i < lines.size() && PatchLine.Type.ADD.equals(lines.get(i).getType());
      List<String> before = new ArrayList<>();
      List<String> after = new ArrayList<>();
      for (; i < lines.size(); i++) {
        PatchLine patchLine = lines.get(i);
        PatchLine.Type type = patchLine.getType();
        if (PatchLine.Type.CONTEXT.equals(type)) {
          break;
        }
        if (PatchLine.Type.ADD.equals(type)) {
          if (addFirst && ! before.isEmpty()) {
            // new piece
            steps.add(new BeforeAfter<>(before, after));
            before = new ArrayList<>();
            after = new ArrayList<>();
          }
          after.add(patchLine.getText());
        } else if (PatchLine.Type.REMOVE.equals(type)) {
          if (! addFirst && ! after.isEmpty()) {
            // new piece
            steps.add(new BeforeAfter<>(before, after));
            before = new ArrayList<>();
            after = new ArrayList<>();
          }
          before.add(patchLine.getText());
        }
      }
      if (! before.isEmpty() || ! after.isEmpty()) {
        steps.add(new BeforeAfter<>(before, after));
      }

      for (; i < lines.size(); i++) {
        PatchLine patchLine = lines.get(i);
        if (! PatchLine.Type.CONTEXT.equals(patchLine.getType())) {
          return i;
        }
        contextAfter.add(patchLine.getText());
      }
      return lines.size();
    }

    public boolean isInsertion() {
      return myPatchSteps.size() == 1 && myPatchSteps.get(0).getBefore().isEmpty();
    }

    public int getStartLineBefore() {
      return myStartLineBefore;
    }

    public int getStartLineAfter() {
      return myStartLineAfter;
    }

    public List<String> getContextBefore() {
      return myContextBefore;
    }

    public List<String> getContextAfter() {
      return myContextAfter;
    }

    @Nonnull
    public List<BeforeAfter<List<String>>> getPatchSteps() {
      return myPatchSteps;
    }

    public List<String> getAfterAll() {
      ArrayList<String> after = new ArrayList<>();
      for (BeforeAfter<List<String>> step : myPatchSteps) {
        after.addAll(step.getAfter());
      }
      return after;
    }
  }

  public static class MyAppliedData {
    private List<String> myList;
    private final boolean myHaveAlreadyApplied;
    private final boolean myPlaceCoinside;
    private final boolean myChangedCoinside;
    private final ChangeType myChangeType;

    public MyAppliedData(List<String> list,
                         boolean alreadyApplied,
                         boolean placeCoinside,
                         boolean changedCoinside,
                         ChangeType changeType) {
      myList = list;
      myHaveAlreadyApplied = alreadyApplied;
      myPlaceCoinside = placeCoinside;
      myChangedCoinside = changedCoinside;
      myChangeType = changeType;
    }

    public List<String> getList() {
      return myList;
    }

    public void cutToSize(int size) {
      assert size > 0 && size < myList.size();
      myList = new ArrayList<>(myList.subList(0, size));
    }

    public boolean isHaveAlreadyApplied() {
      return myHaveAlreadyApplied;
    }

    public boolean isPlaceCoinside() {
      return myPlaceCoinside;
    }

    public boolean isChangedCoinside() {
      return myChangedCoinside;
    }
  }

  public enum ChangeType {
    REPLACE
  }

  public TreeMap<TextRange, MyAppliedData> getTransformations() {
    return myTransformations;
  }

  private static class HunksComparator implements Comparator<SplitHunk> {
    private final static HunksComparator ourInstance = new HunksComparator();

    public static HunksComparator getInstance() {
      return ourInstance;
    }

    @Override
    public int compare(SplitHunk o1, SplitHunk o2) {
      return Integer.valueOf(o1.getStartLineBefore()).compareTo(Integer.valueOf(o2.getStartLineBefore()));
    }
  }
}
