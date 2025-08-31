// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.folding.impl;

import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.*;
import consulo.codeEditor.impl.CodeEditorFoldingModelBase;
import consulo.codeEditor.internal.FoldingUtil;
import consulo.document.util.TextRange;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.editor.internal.EditorFoldingInfoImpl;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author cdr
 */
class UpdateFoldRegionsOperation implements Runnable {
  enum ApplyDefaultStateMode {
    YES,
    EXCEPT_CARET_REGION,
    NO
  }

  private static final Logger LOG = Logger.getInstance(UpdateFoldRegionsOperation.class);
  private static final Key<Boolean> CAN_BE_REMOVED_WHEN_COLLAPSED = Key.create("canBeRemovedWhenCollapsed");
  static final Key<Boolean> COLLAPSED_BY_DEFAULT = Key.create("collapsedByDefault");
  static final Key<String> SIGNATURE = Key.create("signature");
  static final String NO_SIGNATURE = "no signature";

  private static final Comparator<PsiElement> COMPARE_BY_OFFSET_REVERSED = (element, element1) -> {
    int startOffsetDiff = element1.getTextRange().getStartOffset() - element.getTextRange().getStartOffset();
    return startOffsetDiff == 0 ? element1.getTextRange().getEndOffset() - element.getTextRange().getEndOffset() : startOffsetDiff;
  };

  private final Project myProject;
  private final Editor myEditor;
  private final PsiFile myFile;
  @Nonnull
  private final ApplyDefaultStateMode myApplyDefaultState;
  private final FoldingMap myElementsToFoldMap = new FoldingMap();
  private final Set<FoldingUpdate.RegionInfo> myRegionInfos = new LinkedHashSet<>();
  private final MultiMap<FoldingGroup, FoldingUpdate.RegionInfo> myGroupedRegionInfos = new MultiMap<>();
  private final boolean myKeepCollapsedRegions;
  private final boolean myForInjected;

  UpdateFoldRegionsOperation(@Nonnull Project project,
                             @Nonnull Editor editor,
                             @Nonnull PsiFile file,
                             @Nonnull List<FoldingUpdate.RegionInfo> elementsToFold,
                             @Nonnull ApplyDefaultStateMode applyDefaultState,
                             boolean keepCollapsedRegions,
                             boolean forInjected) {
    myProject = project;
    myEditor = editor;
    myFile = file;
    myApplyDefaultState = applyDefaultState;
    myKeepCollapsedRegions = keepCollapsedRegions;
    myForInjected = forInjected;
    for (FoldingUpdate.RegionInfo regionInfo : elementsToFold) {
      myElementsToFoldMap.putValue(regionInfo.element, regionInfo);
      myRegionInfos.add(regionInfo);
      FoldingGroup group = regionInfo.descriptor.getGroup();
      if (group != null) myGroupedRegionInfos.putValue(group, regionInfo);
    }
  }

  @Override
  public void run() {
    EditorFoldingInfoImpl info = EditorFoldingInfoImpl.get(myEditor);
    FoldingModelEx foldingModel = (FoldingModelEx)myEditor.getFoldingModel();
    Map<TextRange, Boolean> rangeToExpandStatusMap = new HashMap<>();

    removeInvalidRegions(info, foldingModel, rangeToExpandStatusMap);

    Map<FoldRegion, Boolean> shouldExpand = new HashMap<>();
    Map<FoldingGroup, Boolean> groupExpand = new HashMap<>();
    List<FoldRegion> newRegions = addNewRegions(info, foldingModel, rangeToExpandStatusMap, shouldExpand, groupExpand);

    applyExpandStatus(newRegions, shouldExpand, groupExpand);

    foldingModel.clearDocumentRangesModificationStatus();
  }

  private static void applyExpandStatus(@Nonnull List<? extends FoldRegion> newRegions, @Nonnull Map<FoldRegion, Boolean> shouldExpand, @Nonnull Map<FoldingGroup, Boolean> groupExpand) {
    for (FoldRegion region : newRegions) {
      FoldingGroup group = region.getGroup();
      Boolean expanded = group == null ? shouldExpand.get(region) : groupExpand.get(group);

      if (expanded != null) {
        region.setExpanded(expanded.booleanValue());
      }
    }
  }

  private List<FoldRegion> addNewRegions(@Nonnull EditorFoldingInfoImpl info,
                                         @Nonnull FoldingModelEx foldingModel,
                                         @Nonnull Map<TextRange, Boolean> rangeToExpandStatusMap,
                                         @Nonnull Map<FoldRegion, Boolean> shouldExpand,
                                         @Nonnull Map<FoldingGroup, Boolean> groupExpand) {
    List<FoldRegion> newRegions = new ArrayList<>();
    SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(myProject);
    for (FoldingUpdate.RegionInfo regionInfo : myRegionInfos) {
      ProgressManager.checkCanceled();
      FoldingDescriptor descriptor = regionInfo.descriptor;
      FoldingGroup group = descriptor.getGroup();
      TextRange range = descriptor.getRange();
      String placeholder = null;
      try {
        placeholder = descriptor.getPlaceholderText();
      }
      catch (IndexNotReadyException ignore) {
      }
      if (range.getEndOffset() > myEditor.getDocument().getTextLength()) {
        LOG.error(String.format("Invalid folding descriptor detected (%s). It ends beyond the document range (%d)", descriptor, myEditor.getDocument().getTextLength()));
        continue;
      }
      FoldRegion region = foldingModel.createFoldRegion(range.getStartOffset(), range.getEndOffset(), placeholder == null ? "..." : placeholder, group, descriptor.isNonExpandable());
      if (region == null) continue;

      if (descriptor.isNonExpandable()) region.putUserData(CodeEditorFoldingModelBase.SELECT_REGION_ON_CARET_NEARBY, Boolean.TRUE);

      PsiElement psi = descriptor.getElement().getPsi();

      if (psi == null || !psi.isValid() || !myFile.isValid()) {
        region.dispose();
        continue;
      }

      region.setGutterMarkEnabledForSingleLine(descriptor.isGutterMarkEnabledForSingleLine());

      if (descriptor.canBeRemovedWhenCollapsed()) region.putUserData(CAN_BE_REMOVED_WHEN_COLLAPSED, Boolean.TRUE);
      region.putUserData(COLLAPSED_BY_DEFAULT, regionInfo.collapsedByDefault);
      region.putUserData(SIGNATURE, ObjectUtil.chooseNotNull(regionInfo.signature, NO_SIGNATURE));

      info.addRegion(region, smartPointerManager.createSmartPsiElementPointer(psi));
      newRegions.add(region);

      boolean expandStatus = !descriptor.isNonExpandable() && shouldExpandNewRegion(range, rangeToExpandStatusMap, regionInfo.collapsedByDefault);
      if (group == null) {
        shouldExpand.put(region, expandStatus);
      }
      else {
        Boolean alreadyExpanded = groupExpand.get(group);
        groupExpand.put(group, alreadyExpanded == null ? expandStatus : alreadyExpanded.booleanValue() || expandStatus);
      }
    }

    return newRegions;
  }

  private boolean shouldExpandNewRegion(TextRange range, Map<TextRange, Boolean> rangeToExpandStatusMap, boolean collapsedByDefault) {
    if (myApplyDefaultState != ApplyDefaultStateMode.NO) {
      // Considering that this code is executed only on initial fold regions construction on editor opening.
      if (myApplyDefaultState == ApplyDefaultStateMode.EXCEPT_CARET_REGION) {
        TextRange lineRange = OpenFileDescriptorImpl.getRangeToUnfoldOnNavigation(myEditor);
        if (lineRange.intersects(range)) {
          return true;
        }
      }
      return !collapsedByDefault;
    }

    Boolean oldStatus = rangeToExpandStatusMap.get(range);
    return oldStatus == null || oldStatus.booleanValue() || FoldingUtil.caretInsideRange(myEditor, range);
  }

  private void removeInvalidRegions(@Nonnull EditorFoldingInfoImpl info, @Nonnull FoldingModelEx foldingModel, @Nonnull Map<TextRange, Boolean> rangeToExpandStatusMap) {
    List<FoldRegion> toRemove = new ArrayList<>();
    Ref<FoldingUpdate.RegionInfo> infoRef = Ref.create();
    Set<FoldingGroup> processedGroups = new HashSet<>();
    List<FoldingUpdate.RegionInfo> matchedInfos = new ArrayList<>();
    for (FoldRegion region : foldingModel.getAllFoldRegions()) {
      FoldingGroup group = region.getGroup();
      if (group != null && !processedGroups.add(group)) continue;

      List<FoldRegion> regionsToProcess = group == null ? Collections.singletonList(region) : foldingModel.getGroupedRegions(group);
      matchedInfos.clear();
      boolean shouldRemove = false;
      for (FoldRegion regionToProcess : regionsToProcess) {
        if (!regionToProcess.isValid() || shouldRemoveRegion(regionToProcess, info, rangeToExpandStatusMap, infoRef)) {
          shouldRemove = true;
        }
        FoldingUpdate.RegionInfo regionInfo = infoRef.get();
        matchedInfos.add(regionInfo);
      }
      if (!shouldRemove && group != null) {
        FoldingGroup requestedGroup = null;
        for (FoldingUpdate.RegionInfo matchedInfo : matchedInfos) {
          if (matchedInfo == null) {
            shouldRemove = true;
            break;
          }
          FoldingGroup g = matchedInfo.descriptor.getGroup();
          if (g == null) {
            shouldRemove = true;
            break;
          }
          if (requestedGroup == null) {
            requestedGroup = g;
          }
          else if (!requestedGroup.equals(g)) {
            shouldRemove = true;
            break;
          }
        }
        if (myGroupedRegionInfos.get(requestedGroup).size() != matchedInfos.size()) {
          shouldRemove = true;
        }
      }
      if (shouldRemove) {
        for (FoldRegion r : regionsToProcess) {
          rangeToExpandStatusMap.putIfAbsent(TextRange.create(r), r.isExpanded());
        }
        toRemove.addAll(regionsToProcess);
      }
      else {
        for (FoldingUpdate.RegionInfo matchedInfo : matchedInfos) {
          if (matchedInfo != null) {
            myElementsToFoldMap.remove(matchedInfo.element, matchedInfo);
            myRegionInfos.remove(matchedInfo);
          }
        }
      }
    }

    for (FoldRegion region : toRemove) {
      foldingModel.removeFoldRegion(region);
      info.removeRegion(region);
    }
  }

  private boolean shouldRemoveRegion(FoldRegion region, EditorFoldingInfoImpl info, Map<TextRange, Boolean> rangeToExpandStatusMap, Ref<? super FoldingUpdate.RegionInfo> matchingInfo) {
    matchingInfo.set(null);
    PsiElement element = info.getPsiElement(region);
    if (element != null) {
      PsiFile containingFile = element.getContainingFile();
      boolean isInjected = InjectedLanguageManager.getInstance(myProject).isInjectedFragment(containingFile);
      if (isInjected != myForInjected) return false;
    }
    boolean forceKeepRegion = myKeepCollapsedRegions && !region.isExpanded() && !regionOrGroupCanBeRemovedWhenCollapsed(region);
    Boolean storedCollapsedByDefault = region.getUserData(COLLAPSED_BY_DEFAULT);
    Collection<FoldingUpdate.RegionInfo> regionInfos;
    if (element != null && !(regionInfos = myElementsToFoldMap.get(element)).isEmpty()) {
      FoldingUpdate.RegionInfo[] array = regionInfos.toArray(new FoldingUpdate.RegionInfo[0]);
      for (FoldingUpdate.RegionInfo regionInfo : array) {
        FoldingDescriptor descriptor = regionInfo.descriptor;
        TextRange range = descriptor.getRange();
        if (TextRange.areSegmentsEqual(region, range)) {
          if (!region.getPlaceholderText().equals(descriptor.getPlaceholderText()) || range.getLength() < 2) {
            return true;
          }
          else if (storedCollapsedByDefault != null && storedCollapsedByDefault != regionInfo.collapsedByDefault) {
            rangeToExpandStatusMap.put(range, !regionInfo.collapsedByDefault);
            return true;
          }
          else {
            matchingInfo.set(regionInfo);
            return false;
          }
        }
      }
      if (!forceKeepRegion) {
        for (FoldingUpdate.RegionInfo regionInfo : regionInfos) {
          rangeToExpandStatusMap.put(regionInfo.descriptor.getRange(), region.isExpanded());
        }
        return true;
      }
    }
    else if (!forceKeepRegion && !(region.getUserData(SIGNATURE) == null /* 'light' region */)) {
      return true;
    }
    return false;
  }

  private boolean regionOrGroupCanBeRemovedWhenCollapsed(FoldRegion region) {
    FoldingGroup group = region.getGroup();
    List<FoldRegion> affectedRegions = group != null && myEditor instanceof EditorEx ? ((EditorEx)myEditor).getFoldingModel().getGroupedRegions(group) : Collections.singletonList(region);
    for (FoldRegion affectedRegion : affectedRegions) {
      if (regionCanBeRemovedWhenCollapsed(affectedRegion)) return true;
    }
    return false;
  }

  private boolean regionCanBeRemovedWhenCollapsed(FoldRegion region) {
    return Boolean.TRUE.equals(region.getUserData(CAN_BE_REMOVED_WHEN_COLLAPSED)) ||
           ((FoldingModelEx)myEditor.getFoldingModel()).hasDocumentRegionChangedFor(region) ||
           !region.isValid() ||
           isRegionInCaretLine(region);
  }

  private boolean isRegionInCaretLine(FoldRegion region) {
    int regionStartLine = myEditor.getDocument().getLineNumber(region.getStartOffset());
    int regionEndLine = myEditor.getDocument().getLineNumber(region.getEndOffset());
    int caretLine = myEditor.getCaretModel().getLogicalPosition().line;
    return caretLine >= regionStartLine && caretLine <= regionEndLine;
  }

  private static class FoldingMap extends MultiMap<PsiElement, FoldingUpdate.RegionInfo> {
    public FoldingMap() {
      super(new TreeMap<>(COMPARE_BY_OFFSET_REVERSED));
    }

    @Nonnull
    @Override
    protected Collection<FoldingUpdate.RegionInfo> createCollection() {
      return new ArrayList<>(1);
    }
  }

}
