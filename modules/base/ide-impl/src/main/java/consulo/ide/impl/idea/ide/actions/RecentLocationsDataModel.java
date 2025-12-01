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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ui.UISettings;
import consulo.application.util.SynchronizedClearableLazy;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.component.messagebus.MessageBusConnection;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.fileEditor.history.PlaceInfo;
import consulo.fileEditor.history.RecentPlacesListener;
import consulo.fileEditor.impl.internal.IdeDocumentHistoryImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * from kotlin
 */
public class RecentLocationsDataModel {
    @Nonnull
    private final Project myProject;
    private final List<Editor> myEditorsToRelease;

    private final MessageBusConnection myProjectConnection;

    private final SynchronizedClearableLazy<List<RecentLocationItem>> myNavigationPlaces;
    private final SynchronizedClearableLazy<List<RecentLocationItem>> myChangedPlaces;

    public RecentLocationsDataModel(@Nonnull Project project, List<Editor> editorsToRelease) {
        this.myProject = project;
        this.myEditorsToRelease = editorsToRelease;
        myProjectConnection = this.myProject.getMessageBus().connect();

        myProjectConnection.subscribe(RecentPlacesListener.class, new RecentPlacesListener() {
            @Override
            public void recentPlaceAdded(@Nonnull PlaceInfo changePlace, boolean isChanged) {
                resetPlaces(isChanged);
            }

            @Override
            public void recentPlaceRemoved(@Nonnull PlaceInfo changePlace, boolean isChanged) {
                resetPlaces(isChanged);
            }

            private void resetPlaces(boolean isChanged) {
                if (isChanged) {
                    myChangedPlaces.drop();
                }
                else {
                    myNavigationPlaces.drop();
                }
            }
        });


        myNavigationPlaces = calculateItems(false);
        myChangedPlaces = calculateItems(true);
    }

    private SynchronizedClearableLazy<List<RecentLocationItem>> calculateItems(boolean changed) {
        return new SynchronizedClearableLazy<>(() -> {
            List<RecentLocationItem> items = createPlaceLinePairs(changed);
            myEditorsToRelease.addAll(ContainerUtil.map(items, RecentLocationItem::getEditor));
            return items;
        });
    }

    @Nonnull
    private List<RecentLocationItem> createPlaceLinePairs(boolean changed) {
        return getPlaces(myProject, changed).stream().map(placeInfo -> {
            EditorEx editor = createEditor(placeInfo);
            if (editor == null) {
                return null;
            }
            return new RecentLocationItem(editor, placeInfo);
        }).filter(Objects::nonNull).limit(UISettings.getInstance().getRecentLocationsLimit()).collect(Collectors.toList());
    }

    @Nullable
    @RequiredReadAction
    private EditorEx createEditor(PlaceInfo placeInfo) {
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
        EditorEx editor = (EditorEx) editorFactory.createEditor(editorDocument, myProject);

        EditorGutterComponentEx gutterComponentEx = editor.getGutterComponentEx();
        int linesShift = fileDocument.getLineNumber(actualTextRange.getStartOffset());

        gutterComponentEx.setLineNumberConvertor(index -> index + linesShift);

        gutterComponentEx.setPaintBackground(false);
        JScrollPane scrollPane = editor.getScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        fillEditorSettings(editor.getSettings());
        setHighlighting(editor, fileDocument, placeInfo, actualTextRange);

        return editor;
    }

    @RequiredReadAction
    private void setHighlighting(
        EditorEx editor,
        Document document,
        PlaceInfo placeInfo,
        TextRange textRange
    ) {
        EditorColorsScheme colorsScheme = EditorColorsManager.getInstance().getGlobalScheme();

        applySyntaxHighlighting(editor, document, colorsScheme, textRange, placeInfo);
        applyHighlightingPasses(editor, document, colorsScheme, textRange);
    }

    private void applySyntaxHighlighting(
        EditorEx editor,
        Document document,
        EditorColorsScheme colorsScheme,
        TextRange textRange,
        PlaceInfo placeInfo
    ) {
        EditorHighlighter editorHighlighter =
            EditorHighlighterFactory.getInstance().createEditorHighlighter(placeInfo.getFile(), colorsScheme, myProject);
        editorHighlighter.setEditor(new LightHighlighterClient(document, myProject));
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

    @RequiredReadAction
    private void applyHighlightingPasses(
        EditorEx editor,
        Document document,
        EditorColorsScheme colorsScheme,
        TextRange rangeMarker
    ) {
        int startOffset = rangeMarker.getStartOffset();
        int endOffset = rangeMarker.getEndOffset();
        DaemonCodeAnalyzerInternal.processHighlights(
            document,
            myProject,
            null,
            startOffset,
            endOffset,
            i -> {
                HighlightInfoImpl info = (HighlightInfoImpl) i;

                if (info.getStartOffset() >= startOffset && info.getEndOffset() <= endOffset) {
                    HighlightSeverity highlightSeverity = info.getSeverity();
                    if (highlightSeverity == HighlightSeverity.ERROR ||
                        highlightSeverity == HighlightSeverity.WARNING ||
                        highlightSeverity == HighlightSeverity.WEAK_WARNING ||
                        highlightSeverity == HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING) {
                        return true;
                    }

                    TextAttributes textAttributes = info.myForcedTextAttributes != null
                        ? info.myForcedTextAttributes
                        : colorsScheme.getAttributes(info.myForcedTextAttributesKey);
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
            }
        );
    }

    private TextRange getTrimmedRange(Document document, int lineNumber) {
        TextRange range = getLinesRange(document, lineNumber);
        String text = document.getText(TextRange.create(range.getStartOffset(), range.getEndOffset()));

        int newLinesBefore =
            StringUtil.countNewLines(Objects.requireNonNull(StringUtil.substringBefore(text, StringUtil.trimLeading(text))));
        int newLinesAfter =
            StringUtil.countNewLines(Objects.requireNonNull(StringUtil.substringAfter(text, StringUtil.trimTrailing(text))));

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
    private List<PlaceInfo> getPlaces(Project project, boolean changed) {
        IdeDocumentHistory ideDocumentHistory = IdeDocumentHistory.getInstance(project);
        List<PlaceInfo> infos =
            ContainerUtil.reverse(changed ? ideDocumentHistory.getChangePlaces() : ideDocumentHistory.getBackPlaces());

        List<PlaceInfo> infosCopy = new ArrayList<>();
        for (PlaceInfo info : infos) {
            if (infosCopy.stream().noneMatch(info1 -> IdeDocumentHistoryImpl.isSame(info, info1))) {
                infosCopy.add(info);
            }
        }
        return infosCopy;
    }

    public List<RecentLocationItem> getPlaces(boolean changed) {
        return changed ? this.myChangedPlaces.getValue() : this.myNavigationPlaces.getValue();
    }

    public List<Editor> getEditorsToRelease() {
        return myEditorsToRelease;
    }

    public MessageBusConnection getProjectConnection() {
        return myProjectConnection;
    }
}
