/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.impl.idea.codeInsight.breadcrumbs.FileBreadcrumbsCollector;
import consulo.ide.impl.idea.openapi.fileEditor.impl.RecentPlacesListener;
import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.application.ui.UISettings;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.EditorHighlighter;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.codeEditor.HighlighterIterator;
import consulo.codeEditor.LightHighlighterClient;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.openapi.fileEditor.ex.IdeDocumentHistory;
import consulo.ide.impl.idea.openapi.fileEditor.impl.IdeDocumentHistoryImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.application.util.NotNullLazyValue;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.application.util.registry.Registry;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.ui.components.breadcrumbs.Crumb;
import consulo.document.util.DocumentUtil;
import consulo.ide.impl.idea.util.concurrency.SynchronizedClearableLazy;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.component.messagebus.MessageBusConnection;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * from kotlin
 */
public class RecentLocationsDataModel {
  private final Project project;
  private final List<Editor> editorsToRelease;

  private final MessageBusConnection projectConnection;

  private final SynchronizedClearableLazy<List<RecentLocationItem>> navigationPlaces;
  private final SynchronizedClearableLazy<List<RecentLocationItem>> changedPlaces;

  private NotNullLazyValue<Map<IdeDocumentHistoryImpl.PlaceInfo, String>> changedPlacedBreadcrumbsMap;
  private NotNullLazyValue<Map<IdeDocumentHistoryImpl.PlaceInfo, String>> navigationPlacesBreadcrumbsMap;

  public RecentLocationsDataModel(Project project, List<Editor> editorsToRelease) {
    this.project = project;
    this.editorsToRelease = editorsToRelease;
    projectConnection = this.project.getMessageBus().connect();

    projectConnection.subscribe(RecentPlacesListener.class, new RecentPlacesListener() {
      @Override
      public void recentPlaceAdded(@Nonnull IdeDocumentHistoryImpl.PlaceInfo changePlace, boolean isChanged) {
        resetPlaces(isChanged);
      }

      @Override
      public void recentPlaceRemoved(@Nonnull IdeDocumentHistoryImpl.PlaceInfo changePlace, boolean isChanged) {
        resetPlaces(isChanged);
      }

      private void resetPlaces(boolean isChanged) {
        if (isChanged) {
          changedPlaces.drop();
        }
        else {
          navigationPlaces.drop();
        }
      }
    });


    navigationPlaces = calculateItems(project, false);
    changedPlaces = calculateItems(project, true);

    navigationPlacesBreadcrumbsMap = NotNullLazyValue.createValue(() -> collectBreadcrumbs(project, navigationPlaces.getValue()));
    changedPlacedBreadcrumbsMap = NotNullLazyValue.createValue(() -> collectBreadcrumbs(project, changedPlaces.getValue()));
  }

  private SynchronizedClearableLazy<List<RecentLocationItem>> calculateItems(Project project, boolean changed) {
    return new SynchronizedClearableLazy<>(() -> {
      List<RecentLocationItem> items = createPlaceLinePairs(project, changed);
      editorsToRelease.addAll(ContainerUtil.map(items, RecentLocationItem::getEditor));
      return items;
    });
  }

  @Nonnull
  private List<RecentLocationItem> createPlaceLinePairs(Project project, boolean changed) {
    return getPlaces(project, changed).stream().map(placeInfo -> {
      EditorEx editor = createEditor(project, placeInfo);
      if (editor == null) {
        return null;
      }
      return new RecentLocationItem(editor, placeInfo);
    }).filter(Objects::nonNull).limit(UISettings.getInstance().getRecentLocationsLimit()).collect(Collectors.toList());
  }

  @Nullable
  private EditorEx createEditor(Project project, IdeDocumentHistoryImpl.PlaceInfo placeInfo) {
    RangeMarker positionOffset = placeInfo.getCaretPosition();
    if (positionOffset == null || !positionOffset.isValid()) {
      return null;
    }

    assert positionOffset.getStartOffset() == positionOffset.getEndOffset();

    Document fileDocument = positionOffset.getDocument();
    int lineNumber = fileDocument.getLineNumber(positionOffset.getStartOffset());
    TextRange actualTextRange = getTrimmedRange(fileDocument, lineNumber);
    CharSequence documentText = fileDocument.getText(actualTextRange);
    if (actualTextRange.isEmpty()) {
      documentText = IdeLocalize.recentLocationsPopupEmptyFileText().get();
    }

    EditorFactory editorFactory = EditorFactory.getInstance();
    Document editorDocument = editorFactory.createDocument(documentText);
    EditorEx editor = (EditorEx)editorFactory.createEditor(editorDocument, project);

    EditorGutterComponentEx gutterComponentEx = editor.getGutterComponentEx();
    int linesShift = fileDocument.getLineNumber(actualTextRange.getStartOffset());

    gutterComponentEx.setLineNumberConvertor(index -> index + linesShift);

    gutterComponentEx.setPaintBackground(false);
    JScrollPane scrollPane = editor.getScrollPane();
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

    fillEditorSettings(editor.getSettings());
    setHighlighting(project, editor, fileDocument, placeInfo, actualTextRange);

    return editor;
  }

  private void setHighlighting(
    Project project,
    EditorEx editor,
    Document document,
    IdeDocumentHistoryImpl.PlaceInfo placeInfo,
    TextRange textRange
  ) {
    EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();

    applySyntaxHighlighting(project, editor, document, colorsScheme, textRange, placeInfo);
    applyHighlightingPasses(project, editor, document, colorsScheme, textRange);
  }

  private void applySyntaxHighlighting(
    Project project,
    EditorEx editor,
    Document document,
    EditorColorsScheme colorsScheme,
    TextRange textRange,
    IdeDocumentHistoryImpl.PlaceInfo placeInfo
  ) {
    EditorHighlighter editorHighlighter =
      EditorHighlighterFactory.getInstance().createEditorHighlighter(placeInfo.getFile(), colorsScheme, project);
    editorHighlighter.setEditor(new LightHighlighterClient(document, project));
    editorHighlighter.setText(document.getText(TextRange.create(0, textRange.getEndOffset())));
    int startOffset = textRange.getStartOffset();

    for (HighlighterIterator iterator = editorHighlighter.createIterator(startOffset);
         !iterator.atEnd() && iterator.getEnd() <= textRange.getEndOffset(); iterator.advance()) {
      if (iterator.getStart() >= startOffset) {
        editor.getMarkupModel().addRangeHighlighter(
          iterator.getStart() - startOffset,
          iterator.getEnd() - startOffset,
          999,
          iterator.getTextAttributes(),
          HighlighterTargetArea.EXACT_RANGE
        );
      }
    }
  }

  private void applyHighlightingPasses(
    Project project,
    final EditorEx editor,
    Document document,
    final EditorColorsScheme colorsScheme,
    final TextRange rangeMarker
  ) {
    final int startOffset = rangeMarker.getStartOffset();
    final int endOffset = rangeMarker.getEndOffset();
    DaemonCodeAnalyzerEx.processHighlights(document, project, null, startOffset, endOffset, i -> {
      HighlightInfoImpl info = (HighlightInfoImpl)i;

      if (info.getStartOffset() >= startOffset && info.getEndOffset() <= endOffset) {
        HighlightSeverity highlightSeverity = info.getSeverity();
        if (highlightSeverity == HighlightSeverity.ERROR ||
            highlightSeverity == HighlightSeverity.WARNING ||
            highlightSeverity == HighlightSeverity.WEAK_WARNING ||
            highlightSeverity == HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING) {
          return true;
        }

        TextAttributes textAttributes =
          info.forcedTextAttributes != null ? info.forcedTextAttributes : colorsScheme.getAttributes(info.forcedTextAttributesKey);
        editor.getMarkupModel().addRangeHighlighter(
          info.getActualStartOffset() - rangeMarker.getStartOffset(),
          info.getActualEndOffset() - rangeMarker.getStartOffset(),
          1000,
          textAttributes,
          HighlighterTargetArea.EXACT_RANGE
        );
        return true;
      }
      else {
        return true;
      }
    });
  }

  private TextRange getTrimmedRange(Document document, int lineNumber) {
    TextRange range = getLinesRange(document, lineNumber);
    String text = document.getText(TextRange.create(range.getStartOffset(), range.getEndOffset()));

    int newLinesBefore = StringUtil.countNewLines(Objects.requireNonNull(StringUtil.substringBefore(text, StringUtil.trimLeading(text))));
    int newLinesAfter = StringUtil.countNewLines(Objects.requireNonNull(StringUtil.substringAfter(text, StringUtil.trimTrailing(text))));

    int firstLine = document.getLineNumber(range.getStartOffset());
    int firstLineAdjusted = firstLine + newLinesBefore;

    int lastLine = document.getLineNumber(range.getEndOffset());
    int lastLineAdjusted = lastLine - newLinesAfter;

    int startOffset = document.getLineStartOffset(firstLineAdjusted);
    int endOffset = document.getLineEndOffset(lastLineAdjusted);

    return TextRange.create(startOffset, endOffset);
  }

  @Nonnull
  private TextRange getLinesRange(Document document, int line) {
    int lineCount = document.getLineCount();
    if (lineCount == 0) {
      return TextRange.EMPTY_RANGE;
    }

    int beforeAfterLinesCount = Registry.intValue("recent.locations.lines.before.and.after", 2);

    int before = Math.min(beforeAfterLinesCount, line);
    int after = Math.min(beforeAfterLinesCount, lineCount - line);

    int linesBefore = before + beforeAfterLinesCount - after;
    int linesAfter = after + beforeAfterLinesCount - before;

    int startLine = Math.max(line - linesBefore, 0);
    int endLine = Math.min(line + linesAfter, lineCount - 1);

    int startOffset = document.getLineStartOffset(startLine);
    int endOffset = document.getLineEndOffset(endLine);

    if (startOffset <= endOffset) {
      return TextRange.create(startOffset, endOffset);
    }
    else {
      return TextRange.create(DocumentUtil.getLineTextRange(document, line));
    }
  }

  private void fillEditorSettings(EditorSettings settings) {
    settings.setLineNumbersShown(true);
    settings.setCaretRowShown(false);
    settings.setLineMarkerAreaShown(false);
    settings.setFoldingOutlineShown(false);
    settings.setAdditionalColumnsCount(0);
    settings.setAdditionalLinesCount(0);
    settings.setRightMarginShown(false);
    settings.setUseSoftWraps(false);
    settings.setAdditionalPageAtBottom(false);
  }

  @Nonnull
  private List<IdeDocumentHistoryImpl.PlaceInfo> getPlaces(Project project, boolean changed) {
    IdeDocumentHistory ideDocumentHistory = IdeDocumentHistory.getInstance(project);
    List<IdeDocumentHistoryImpl.PlaceInfo> infos =
      ContainerUtil.reverse(changed ? ideDocumentHistory.getChangePlaces() : ideDocumentHistory.getBackPlaces());

    List<IdeDocumentHistoryImpl.PlaceInfo> infosCopy = new ArrayList<>();
    for (IdeDocumentHistoryImpl.PlaceInfo info : infos) {
      if (infosCopy.stream().noneMatch(info1 -> IdeDocumentHistoryImpl.isSame(info, info1))) {
        infosCopy.add(info);
      }
    }
    return infosCopy;
  }

  public List<RecentLocationItem> getPlaces(boolean changed) {
    return changed ? this.changedPlaces.getValue() : this.navigationPlaces.getValue();
  }

  public List<Editor> getEditorsToRelease() {
    return editorsToRelease;
  }

  public MessageBusConnection getProjectConnection() {
    return projectConnection;
  }

  @Nonnull
  private Map<IdeDocumentHistoryImpl.PlaceInfo, String> collectBreadcrumbs(Project project, List<RecentLocationItem> items) {
    return items.stream().map(RecentLocationItem::getInfo).collect(Collectors.toMap(it -> it, it -> getBreadcrumbs(project, it)));
  }

  private String getBreadcrumbs(Project project, IdeDocumentHistoryImpl.PlaceInfo placeInfo) {
    RangeMarker rangeMarker = placeInfo.getCaretPosition();
    String fileName = placeInfo.getFile().getName();
    if (rangeMarker == null) {
      return fileName;
    }

    FileBreadcrumbsCollector collector = FileBreadcrumbsCollector.findBreadcrumbsCollector(project, placeInfo.getFile());
    if (collector == null) {
      return fileName;
    }

    Iterable<? extends Crumb> crumbs =
      collector.computeCrumbs(placeInfo.getFile(), rangeMarker.getDocument(), rangeMarker.getStartOffset(), true);

    if (!crumbs.iterator().hasNext()) {
      return fileName;
    }

    String breadcrumbsText = StringUtil.join(crumbs, o -> o.getText(), " > ");
    return StringUtil.shortenTextWithEllipsis(breadcrumbsText, 50, 0);
  }

  public Map<IdeDocumentHistoryImpl.PlaceInfo, String> getBreadcrumbsMap(boolean changed) {
    return changed ? changedPlacedBreadcrumbsMap.getValue() : navigationPlacesBreadcrumbsMap.getValue();
  }
}
