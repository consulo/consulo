/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.setting;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.*;
import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.codeStyle.*;
import consulo.language.codeStyle.ui.internal.ChangesDiffCalculator;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.util.ProjectUIUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CustomLineBorder;
import consulo.ui.ex.awt.UserActivityWatcher;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.Alarm;
import consulo.undoRedo.CommandProcessor;
import consulo.util.io.FileUtil;
import consulo.util.lang.LocalTimeCounter;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class CodeStyleAbstractPanel implements Disposable {
    private static final long TIME_TO_HIGHLIGHT_PREVIEW_CHANGES_IN_MILLIS = TimeUnit.SECONDS.toMillis(3);

    private static final Logger LOG = Logger.getInstance(CodeStyleAbstractPanel.class);

    private final ChangesDiffCalculator myDiffCalculator = new ChangesDiffCalculator();
    private final List<TextRange> myPreviewRangesToHighlight = new ArrayList<>();

    private final Editor myEditor;
    private final CodeStyleSettings mySettings;
    private boolean myShouldUpdatePreview;
    protected static final int[] ourWrappings = {
        CommonCodeStyleSettings.DO_NOT_WRAP,
        CommonCodeStyleSettings.WRAP_AS_NEEDED,
        CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM,
        CommonCodeStyleSettings.WRAP_ALWAYS
    };
    private long myLastDocumentModificationStamp;
    private String myTextToReformat = null;
    private final UserActivityWatcher myUserActivityWatcher = new UserActivityWatcher();

    private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

    private CodeStyleSchemesModel myModel;
    private boolean mySomethingChanged = false;
    private long myEndHighlightPreviewChangesTimeMillis = -1;
    private boolean myShowsPreviewHighlighters;
    private final CodeStyleSettings myCurrentSettings;
    private final Language myDefaultLanguage;
    private Document myDocumentBeforeChanges;

    @RequiredUIAccess
    protected CodeStyleAbstractPanel(@Nonnull CodeStyleSettings settings) {
        this(null, null, settings);
    }

    @RequiredUIAccess
    protected CodeStyleAbstractPanel(
        @Nullable Language defaultLanguage,
        @Nullable CodeStyleSettings currentSettings,
        @Nonnull CodeStyleSettings settings
    ) {
        myCurrentSettings = currentSettings;
        mySettings = settings;
        myDefaultLanguage = defaultLanguage;
        myEditor = createEditor();

        if (myEditor != null) {
            myUpdateAlarm.setActivationComponent(myEditor.getComponent());
        }
        myUserActivityWatcher.addUserActivityListener(this::somethingChanged);

        updatePreview(true);
    }

    protected void setShouldUpdatePreview(boolean shouldUpdatePreview) {
        myShouldUpdatePreview = shouldUpdatePreview;
    }

    private synchronized void setSomethingChanged(boolean b) {
        mySomethingChanged = b;
    }

    private synchronized boolean isSomethingChanged() {
        return mySomethingChanged;
    }

    public void setModel(@Nonnull CodeStyleSchemesModel model) {
        myModel = model;
    }

    protected void somethingChanged() {
        if (myModel != null) {
            myModel.fireCurrentSettingsChanged();
        }
    }

    protected void addPanelToWatch(Component component) {
        myUserActivityWatcher.register(component);
    }

    @Nullable
    private Editor createEditor() {
        if (getPreviewText() == null) {
            return null;
        }
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document editorDocument = editorFactory.createDocument("");
        EditorEx editor = (EditorEx)editorFactory.createEditor(editorDocument);
        fillEditorSettings(editor.getSettings());
        myLastDocumentModificationStamp = editor.getDocument().getModificationStamp();
        return editor;
    }

    private static void fillEditorSettings(EditorSettings editorSettings) {
        editorSettings.setWhitespacesShown(true);
        editorSettings.setLineMarkerAreaShown(false);
        editorSettings.setIndentGuidesShown(false);
        editorSettings.setLineNumbersShown(false);
        editorSettings.setFoldingOutlineShown(false);
        editorSettings.setAdditionalColumnsCount(0);
        editorSettings.setAdditionalLinesCount(1);
        editorSettings.setUseSoftWraps(false);
    }

    @RequiredUIAccess
    protected void updatePreview(boolean useDefaultSample) {
        if (myEditor == null) {
            return;
        }
        updateEditor(useDefaultSample);
        updatePreviewHighlighter((EditorEx)myEditor);
    }

    @RequiredUIAccess
    private void updateEditor(boolean useDefaultSample) {
        if (!myShouldUpdatePreview || !(Application.get().isUnitTestMode() || myEditor.getComponent().isShowing())) {
            return;
        }

        if (myLastDocumentModificationStamp != myEditor.getDocument().getModificationStamp()) {
            myTextToReformat = myEditor.getDocument().getText();
        }
        else if (useDefaultSample || myTextToReformat == null) {
            myTextToReformat = getPreviewText();
        }

        int currOffs = myEditor.getScrollingModel().getVerticalScrollOffset();

        Project project = ProjectUIUtil.guessCurrentProject(getPanel());
        CommandProcessor.getInstance().newCommand()
            .project(ProjectUIUtil.guessCurrentProject(getPanel()))
            .run(() -> replaceText(project));

        myEditor.getSettings().setRightMargin(getAdjustedRightMargin());
        myLastDocumentModificationStamp = myEditor.getDocument().getModificationStamp();
        myEditor.getScrollingModel().scrollVertically(currOffs);
    }

    private int getAdjustedRightMargin() {
        int result = getRightMargin();
        return result > 0
            ? result
            : CodeStyle.getSettings(ProjectUIUtil.guessCurrentProject(getPanel())).getRightMargin(getDefaultLanguage());
    }

    protected abstract int getRightMargin();

    @RequiredUIAccess
    private void replaceText(Project project) {
        Application.get().runWriteAction(() -> {
            try {
                Document beforeReformat = null;
                beforeReformat = collectChangesBeforeCurrentSettingsAppliance(project);

                //important not mark as generated not to get the classes before setting language level
                PsiFile psiFile = createFileFromText(project, myTextToReformat);
                prepareForReformat(psiFile);

                try {
                    apply(mySettings);
                }
                catch (ConfigurationException ignore) {
                }
                CodeStyleSettings clone = mySettings.clone();
                clone.setRightMargin(getDefaultLanguage(), getAdjustedRightMargin());
                CodeStyleSettingsManager.getInstance(project).setTemporarySettings(clone);
                PsiFile formatted;
                try {
                    formatted = doReformat(project, psiFile);
                }
                finally {
                    CodeStyleSettingsManager.getInstance(project).dropTemporarySettings();
                }

                myEditor.getSettings().setTabSize(clone.getTabSize(getFileType()));
                Document document = myEditor.getDocument();
                document.replaceString(0, document.getTextLength(), formatted.getText());
                if (beforeReformat != null) {
                    highlightChanges(beforeReformat);
                }
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        });
    }

    /**
     * Reformats {@link #myTextToReformat target text} with the {@link #mySettings current code style settings} and returns
     * list of changes applied to the target text during that.
     *
     * @param project project to use
     * @return list of changes applied to the {@link #myTextToReformat target text} during reformatting. It is sorted
     * by change start offset in ascending order
     */
    @Nullable
    @RequiredReadAction
    private Document collectChangesBeforeCurrentSettingsAppliance(Project project) {
        PsiFile psiFile = createFileFromText(project, myTextToReformat);
        prepareForReformat(psiFile);
        CodeStyleSettings clone = mySettings.clone();
        clone.setRightMargin(getDefaultLanguage(), getAdjustedRightMargin());
        CodeStyle.doWithTemporarySettings(project, clone, () -> CodeStyleManager.getInstance(project).reformat(psiFile));
        return getDocumentBeforeChanges(project, psiFile);
    }

    @RequiredReadAction
    private Document getDocumentBeforeChanges(@Nonnull Project project, @Nonnull PsiFile file) {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        if (documentManager != null) {
            Document document = documentManager.getDocument(file);
            if (document != null) {
                return document;
            }
        }
        if (myDocumentBeforeChanges == null) {
            myDocumentBeforeChanges = DocumentFactory.getInstance().createDocument(file.getText(), false);
        }
        else {
            myDocumentBeforeChanges.replaceString(0, myDocumentBeforeChanges.getTextLength(), file.getText());
        }
        return myDocumentBeforeChanges;
    }

    protected void prepareForReformat(PsiFile psiFile) {
    }

    protected String getFileExt() {
        return getFileTypeExtension(getFileType());
    }

    protected PsiFile createFileFromText(Project project, String text) {
        return PsiFileFactory.getInstance(project)
            .createFileFromText("a." + getFileExt(), getFileType(), text, LocalTimeCounter.currentTime(), true);
    }

    protected PsiFile doReformat(Project project, PsiFile psiFile) {
        CodeStyleManager.getInstance(project).reformat(psiFile);
        return psiFile;
    }

    private void highlightChanges(Document beforeReformat) {
        myPreviewRangesToHighlight.clear();
        MarkupModel markupModel = myEditor.getMarkupModel();
        markupModel.removeAllHighlighters();
        int textLength = myEditor.getDocument().getTextLength();
        boolean highlightPreview = false;
        Collection<TextRange> ranges = ChangesDiffCalculator.calculateDiff(beforeReformat, myEditor.getDocument());
        for (TextRange range : ranges) {
            if (range.getStartOffset() >= textLength) {
                continue;
            }
            highlightPreview = true;
            TextRange rangeToUse = calculateChangeHighlightRange(range);
            myPreviewRangesToHighlight.add(rangeToUse);
        }

        if (highlightPreview) {
            myEndHighlightPreviewChangesTimeMillis = System.currentTimeMillis() + TIME_TO_HIGHLIGHT_PREVIEW_CHANGES_IN_MILLIS;
            myShowsPreviewHighlighters = true;
        }
    }

    /**
     * Allows to answer if particular visual position belongs to visual rectangle identified by the given visual position of
     * its top-left and bottom-right corners.
     *
     * @param targetPosition position which belonging to target visual rectangle should be checked
     * @param startPosition  visual position of top-left corner of the target visual rectangle
     * @param endPosition    visual position of bottom-right corner of the target visual rectangle
     * @return <code>true</code> if given visual position belongs to the target visual rectangle;
     * <code>false</code> otherwise
     */
    private static boolean isWithinBounds(VisualPosition targetPosition, VisualPosition startPosition, VisualPosition endPosition) {
        return targetPosition.line >= startPosition.line
            && targetPosition.line <= endPosition.line
            && targetPosition.column >= startPosition.column
            && targetPosition.column <= endPosition.column;
    }

    /**
     * We want to highlight document formatting changes introduced by particular formatting property value change.
     * However, there is a possible effect that white space region is removed. We still want to highlight that, hence, it's necessary
     * to highlight neighbour region.
     * <p/>
     * This method encapsulates logic of adjusting preview highlight change if necessary.
     *
     * @param range initial range to highlight
     * @return resulting range to highlight
     */
    private TextRange calculateChangeHighlightRange(TextRange range) {
        CharSequence text = myEditor.getDocument().getCharsSequence();

        if (range.getLength() <= 0) {
            int offset = range.getStartOffset();
            while (offset < text.length() && text.charAt(offset) == ' ') {
                offset++;
            }
            return offset > range.getStartOffset() ? new TextRange(offset, offset) : range;
        }

        int startOffset = range.getStartOffset() + 1;
        int endOffset = range.getEndOffset() + 1;
        boolean useSameRange = true;
        while (endOffset <= text.length() && StringUtil.equals(
            text.subSequence(range.getStartOffset(), range.getEndOffset()),
            text.subSequence(startOffset, endOffset)
        )) {
            useSameRange = false;
            startOffset++;
            endOffset++;
        }
        startOffset--;
        endOffset--;

        return useSameRange ? range : new TextRange(startOffset, endOffset);
    }

    private void updatePreviewHighlighter(EditorEx editor) {
        EditorColorsScheme scheme = editor.getColorsScheme();
        editor.getSettings().setCaretRowShown(false);
        editor.setHighlighter(createHighlighter(scheme));
    }

    @Nullable
    protected abstract EditorHighlighter createHighlighter(EditorColorsScheme scheme);

    @Nonnull
    protected abstract FileType getFileType();

    @Nullable
    protected abstract String getPreviewText();

    public abstract void apply(CodeStyleSettings settings) throws ConfigurationException;

    public final void reset(CodeStyleSettings settings) {
        myShouldUpdatePreview = false;
        try {
            resetImpl(settings);
        }
        finally {
            myShouldUpdatePreview = true;
        }
    }

    protected static int getIndexForWrapping(int value) {
        for (int i = 0; i < ourWrappings.length; i++) {
            int ourWrapping = ourWrappings[i];
            if (ourWrapping == value) {
                return i;
            }
        }
        LOG.assertTrue(false);
        return 0;
    }

    public abstract boolean isModified(CodeStyleSettings settings);

    @Nullable
    public abstract JComponent getPanel();

    @Override
    public void dispose() {
        myUpdateAlarm.cancelAllRequests();
        if (myEditor != null) {
            EditorFactory.getInstance().releaseEditor(myEditor);
        }
    }

    protected abstract void resetImpl(CodeStyleSettings settings);

    protected static void fillWrappingCombo(JComboBox wrapCombo) {
        wrapCombo.addItem(ApplicationLocalize.wrappingDoNotWrap().get());
        wrapCombo.addItem(ApplicationLocalize.wrappingWrapIfLong().get());
        wrapCombo.addItem(ApplicationLocalize.wrappingChopDownIfLong().get());
        wrapCombo.addItem(ApplicationLocalize.wrappingWrapAlways().get());
    }

    @Nonnull
    public static String readFromFile(Class resourceContainerClass, String fileName) {
        try {
            InputStream stream = resourceContainerClass.getClassLoader().getResourceAsStream("codeStyle/preview/" + fileName);
            return FileUtil.loadTextAndClose(stream, true);
        }
        catch (IOException e) {
            return "";
        }
    }

    protected void installPreviewPanel(JPanel previewPanel) {
        previewPanel.setLayout(new BorderLayout());
        previewPanel.add(getEditor().getComponent(), BorderLayout.CENTER);
        previewPanel.setBorder(new CustomLineBorder(0, 1, 0, 0));
    }

    protected String getFileTypeExtension(FileType fileType) {
        return fileType.getDefaultExtension();
    }

    @RequiredUIAccess
    public void onSomethingChanged() {
        setSomethingChanged(true);
        if (myEditor != null) {
            if (Application.get().isUnitTestMode()) {
                updateEditor(true);
            }
            else {
                UiNotifyConnector.doWhenFirstShown(myEditor.getComponent(), this::addUpdatePreviewRequest);
            }
        }
    }

    private void addUpdatePreviewRequest() {
        myUpdateAlarm.addComponentRequest(new Runnable() {
            @Override
            @RequiredUIAccess
            public void run() {
                try {
                    myUpdateAlarm.cancelAllRequests();
                    if (isSomethingChanged()) {
                        updateEditor(false);
                    }
                    if (System.currentTimeMillis() <= myEndHighlightPreviewChangesTimeMillis && !myPreviewRangesToHighlight.isEmpty()) {
                        blinkHighlighters();
                        myUpdateAlarm.addComponentRequest(this, 500);
                    }
                    else {
                        myEditor.getMarkupModel().removeAllHighlighters();
                    }
                }
                finally {
                    setSomethingChanged(false);
                }
            }
        }, 300);
    }

    private void blinkHighlighters() {
        MarkupModel markupModel = myEditor.getMarkupModel();
        if (myShowsPreviewHighlighters) {
            Rectangle visibleArea = myEditor.getScrollingModel().getVisibleArea();
            VisualPosition visualStart = myEditor.xyToVisualPosition(visibleArea.getLocation());
            VisualPosition visualEnd =
                myEditor.xyToVisualPosition(new Point(visibleArea.x + visibleArea.width, visibleArea.y + visibleArea.height));

            // There is a possible case that viewport is located at its most bottom position and last document symbol
            // is located at the start of the line, hence, resulting visual end column has a small value and doesn't actually
            // indicates target visible rectangle. Hence, we need to correct that if necessary.
            int endColumnCandidate =
                visibleArea.width / CodeEditorInternalHelper.getInstance().getSpaceWidth(myEditor) + visualStart.column;
            if (endColumnCandidate > visualEnd.column) {
                visualEnd = new VisualPosition(visualEnd.line, endColumnCandidate);
            }
            int offsetToScroll = -1;
            CharSequence text = myEditor.getDocument().getCharsSequence();
            TextAttributes backgroundAttributes = myEditor.getColorsScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
            TextAttributes borderAttributes =
                new TextAttributes(null, null, backgroundAttributes.getBackgroundColor(), EffectType.BOXED, Font.PLAIN);
            boolean scrollToChange = true;
            for (TextRange range : myPreviewRangesToHighlight) {
                if (scrollToChange) {
                    boolean rangeVisible = isWithinBounds(myEditor.offsetToVisualPosition(range.getStartOffset()), visualStart, visualEnd)
                        || isWithinBounds(myEditor.offsetToVisualPosition(range.getEndOffset()), visualStart, visualEnd);
                    scrollToChange = !rangeVisible;
                    if (offsetToScroll < 0) {
                        if (text.charAt(range.getStartOffset()) != '\n') {
                            offsetToScroll = range.getStartOffset();
                        }
                        else if (range.getEndOffset() > 0 && text.charAt(range.getEndOffset() - 1) != '\n') {
                            offsetToScroll = range.getEndOffset() - 1;
                        }
                    }
                }

                TextAttributes attributesToUse = range.getLength() > 0 ? backgroundAttributes : borderAttributes;
                markupModel.addRangeHighlighter(
                    range.getStartOffset(),
                    range.getEndOffset(),
                    HighlighterLayer.SELECTION,
                    attributesToUse,
                    HighlighterTargetArea.EXACT_RANGE
                );
            }

            if (scrollToChange) {
                if (offsetToScroll < 0 && !myPreviewRangesToHighlight.isEmpty()) {
                    offsetToScroll = myPreviewRangesToHighlight.get(0).getStartOffset();
                }
                if (offsetToScroll >= 0 && offsetToScroll < text.length() - 1 && text.charAt(offsetToScroll) != '\n') {
                    // There is a possible case that target offset is located too close to the right edge. However, our point is to show
                    // highlighted region at target offset, hence, we need to scroll to the visual symbol end. Hence, we're trying to ensure
                    // that by scrolling to the symbol's end over than its start.
                    offsetToScroll++;
                }
                if (offsetToScroll >= 0 && offsetToScroll < myEditor.getDocument().getTextLength()) {
                    myEditor.getScrollingModel().scrollTo(myEditor.offsetToLogicalPosition(offsetToScroll), ScrollType.RELATIVE);
                }
            }
        }
        else {
            markupModel.removeAllHighlighters();
        }
        myShowsPreviewHighlighters = !myShowsPreviewHighlighters;
    }

    protected Editor getEditor() {
        return myEditor;
    }

    @Nonnull
    protected CodeStyleSettings getSettings() {
        return mySettings;
    }

    public Set<String> processListOptions() {
        return Collections.emptySet();
    }

    @RequiredUIAccess
    public final void applyPredefinedSettings(@Nonnull PredefinedCodeStyle codeStyle) {
        CodeStyleSettings settings = CodeStyleSettingsManager.getSettings(ProjectUIUtil.guessCurrentProject(getPanel())).clone();
        codeStyle.apply(settings);
        reset(settings);
        onSomethingChanged();
    }

    /**
     * Override this method if the panel is linked to a specific language.
     *
     * @return The language this panel is associated with.
     */
    @Nullable
    public Language getDefaultLanguage() {
        return myDefaultLanguage;
    }

    @Nonnull
    protected LocalizeValue getTabTitle() {
        return LocalizeValue.localizeTODO("Other");
    }

    protected CodeStyleSettings getCurrentSettings() {
        return myCurrentSettings;
    }

    public void setupCopyFromMenu(JPopupMenu copyMenu) {
        copyMenu.removeAll();
    }

    public boolean isCopyFromMenuAvailable() {
        return false;
    }
}
