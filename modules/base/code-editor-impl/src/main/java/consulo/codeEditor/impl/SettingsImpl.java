/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.codeEditor.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author max
 * @since 2002-06-19
 */
public class SettingsImpl implements EditorSettings {
    @Nullable
    protected final EditorEx myEditor;
    @Nullable
    private final Language myLanguage;
    private Boolean myIsCamelWords;

    // This group of settings does not have UI
    private SoftWrapAppliancePlaces mySoftWrapAppliancePlace = SoftWrapAppliancePlaces.MAIN_EDITOR;
    private int myAdditionalLinesCount = 5;
    private int myAdditionalColumnsCount = 3;
    private int myLineCursorWidth = 2;
    private boolean myLineMarkerAreaShown = true;
    private boolean myAllowSingleLogicalLineFolding = false;
    private boolean myAutoCodeFoldingEnabled = true;

    // These comes from CodeStyleSettings
    protected Integer myTabSize = null;
    protected Integer myCachedTabSize = null;
    protected Boolean myUseTabCharacter = null;

    // These comes from EditorSettingsExternalizable defaults.
    private Boolean myIsVirtualSpace = null;
    private Boolean myIsCaretInsideTabs = null;
    private Boolean myIsCaretBlinking = null;
    private Integer myCaretBlinkingPeriod = null;
    private Boolean myIsRightMarginShown = null;
    private Integer myRightMargin = null;
    private Boolean myAreLineNumbersShown = null;
    private Boolean myGutterIconsShown = null;
    private Boolean myIsFoldingOutlineShown = null;
    private Boolean myIsSmartHome = null;
    private Boolean myIsBlockCursor = null;
    private Boolean myCaretRowShown = null;
    private Boolean myIsWhitespacesShown = null;
    private Boolean myIsLeadingWhitespacesShown = null;
    private Boolean myIsInnerWhitespacesShown = null;
    private Boolean myIsTrailingWhitespacesShown = null;
    private Boolean myIndentGuidesShown = null;
    private Boolean myIsAnimatedScrolling = null;
    private Boolean myIsAdditionalPageAtBottom = null;
    private Boolean myIsDndEnabled = null;
    private Boolean myIsWheelFontChangeEnabled = null;
    private Boolean myIsMouseClickSelectionHonorsCamelWords = null;
    private Boolean myIsRenameVariablesInplace = null;
    private Boolean myIsRefrainFromScrolling = null;
    private Boolean myUseSoftWraps = null;
    private Boolean myIsAllSoftWrapsShown = null;
    private Boolean myUseCustomSoftWrapIndent = null;
    private Integer myCustomSoftWrapIndent = null;
    private Boolean myRenamePreselect = null;
    private Boolean myWrapWhenTypingReachesRightMargin = null;
    private Boolean myShowIntentionBulb = null;
    private Boolean myIsHighlightSelectionOccurrences = null;
    private Boolean myStickyLinesEnabled;
    private Integer myStickLinesCount;

    private List<Integer> mySoftMargins = null;
    
    private final EditorSettingsExternalizable myPersistentEditorSettings;

    public SettingsImpl() {
        this(null, null, EditorKind.MAIN_EDITOR);
    }

    public SettingsImpl(@Nullable EditorEx editor, @Nullable Project project, @Nonnull EditorKind kind) {
        myEditor = editor;
        myLanguage = editor != null && project != null ? getDocumentLanguage(project, editor.getDocument()) : null;
        if (EditorKind.CONSOLE.equals(kind)) {
            mySoftWrapAppliancePlace = SoftWrapAppliancePlaces.CONSOLE;
        }
        else if (EditorKind.PREVIEW.equals(kind)) {
            mySoftWrapAppliancePlace = SoftWrapAppliancePlaces.PREVIEW;
        }
        else {
            mySoftWrapAppliancePlace = SoftWrapAppliancePlaces.MAIN_EDITOR;
        }

        myPersistentEditorSettings = EditorSettingsExternalizable.getInstance();
    }

    @Override
    public boolean isRightMarginShown() {
        return myIsRightMarginShown != null ? myIsRightMarginShown : myPersistentEditorSettings.isRightMarginShown();
    }

    @Override
    public void setRightMarginShown(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myIsRightMarginShown)) {
            return;
        }
        myIsRightMarginShown = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isWhitespacesShown() {
        return myIsWhitespacesShown != null ? myIsWhitespacesShown : myPersistentEditorSettings.isWhitespacesShown();
    }

    @Override
    public void setWhitespacesShown(boolean val) {
        myIsWhitespacesShown = val;
    }

    @Override
    public boolean isLeadingWhitespaceShown() {
        return myIsLeadingWhitespacesShown != null ? myIsLeadingWhitespacesShown : myPersistentEditorSettings.isLeadingWhitespacesShown();
    }

    @Override
    public void setLeadingWhitespaceShown(boolean val) {
        myIsLeadingWhitespacesShown = val;
    }

    @Override
    public boolean isInnerWhitespaceShown() {
        return myIsInnerWhitespacesShown != null ? myIsInnerWhitespacesShown : myPersistentEditorSettings.isInnerWhitespacesShown();
    }

    @Override
    public void setInnerWhitespaceShown(boolean val) {
        myIsInnerWhitespacesShown = val;
    }

    @Override
    public boolean isTrailingWhitespaceShown() {
        return myIsTrailingWhitespacesShown != null ? myIsTrailingWhitespacesShown : myPersistentEditorSettings.isTrailingWhitespacesShown();
    }

    @Override
    public void setTrailingWhitespaceShown(boolean val) {
        myIsTrailingWhitespacesShown = val;
    }

    @Override
    public boolean isIndentGuidesShown() {
        return myIndentGuidesShown != null ? myIndentGuidesShown : myPersistentEditorSettings.isIndentGuidesShown();
    }

    @Override
    public void setIndentGuidesShown(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myIndentGuidesShown)) {
            return;
        }

        myIndentGuidesShown = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isLineNumbersShown() {
        return myAreLineNumbersShown != null ? myAreLineNumbersShown : myPersistentEditorSettings.isLineNumbersShown();
    }

    @Override
    public void setLineNumbersShown(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myAreLineNumbersShown)) {
            return;
        }
        myAreLineNumbersShown = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean areGutterIconsShown() {
        return myGutterIconsShown != null ? myGutterIconsShown : myPersistentEditorSettings.areGutterIconsShown();
    }

    @Override
    public void setGutterIconsShown(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myGutterIconsShown)) {
            return;
        }
        myGutterIconsShown = newValue;
        fireEditorRefresh();
    }

    @Override
    public int getRightMargin(Project project) {
        return myRightMargin != null ? myRightMargin : CodeStyle.getProjectOrDefaultSettings(project).getRightMargin(myLanguage);
    }

    @Nullable
    @RequiredReadAction
    private static Language getDocumentLanguage(@Nullable Project project, @Nonnull Document document) {
        if (project != null) {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            PsiFile file = documentManager.getPsiFile(document);
            if (file != null) {
                return file.getLanguage();
            }
        }
        return null;
    }

    @Override
    public boolean isWrapWhenTypingReachesRightMargin(Project project) {
        if (myWrapWhenTypingReachesRightMargin != null) {
            return myWrapWhenTypingReachesRightMargin;
        }
        return myEditor == null ? CodeStyle.getDefaultSettings().isWrapOnTyping(myLanguage) : CodeStyle.getSettings(myEditor.getProject(), myEditor.getDocument()).isWrapOnTyping(myLanguage);
    }

    @Override
    public void setWrapWhenTypingReachesRightMargin(boolean val) {
        myWrapWhenTypingReachesRightMargin = val;
    }

    @Override
    public void setRightMargin(int rightMargin) {
        final Integer newValue = rightMargin;
        if (newValue.equals(myRightMargin)) {
            return;
        }
        myRightMargin = newValue;
        fireEditorRefresh();
    }

    @Nonnull
    @Override
    public List<Integer> getSoftMargins() {
        if (mySoftMargins != null) {
            return mySoftMargins;
        }
        return
            myEditor == null ?
                CodeStyle.getDefaultSettings().getSoftMargins(myLanguage) :
                CodeStyle.getSettings(myEditor.getProject(), myEditor.getDocument()).getSoftMargins(myLanguage);
    }

    @Override
    public void setSoftMargins(@Nullable List<Integer> softMargins) {
        if (Objects.equals(mySoftMargins, softMargins)) {
            return;
        }
        mySoftMargins = softMargins != null ? new ArrayList<>(softMargins) : null;
        fireEditorRefresh();
    }


    @Override
    public int getAdditionalLinesCount() {
        return myAdditionalLinesCount;
    }

    @Override
    public void setAdditionalLinesCount(int additionalLinesCount) {
        if (myAdditionalLinesCount == additionalLinesCount) {
            return;
        }
        myAdditionalLinesCount = additionalLinesCount;
        fireEditorRefresh();
    }

    @Override
    public int getAdditionalColumnsCount() {
        return myAdditionalColumnsCount;
    }

    @Override
    public void setAdditionalColumnsCount(int additionalColumnsCount) {
        if (myAdditionalColumnsCount == additionalColumnsCount) {
            return;
        }
        myAdditionalColumnsCount = additionalColumnsCount;
        fireEditorRefresh();
    }

    @Override
    public boolean isLineMarkerAreaShown() {
        return myLineMarkerAreaShown;
    }

    @Override
    public void setLineMarkerAreaShown(boolean lineMarkerAreaShown) {
        if (myLineMarkerAreaShown == lineMarkerAreaShown) {
            return;
        }
        myLineMarkerAreaShown = lineMarkerAreaShown;
        fireEditorRefresh();
    }

    @Override
    public boolean isFoldingOutlineShown() {
        return myIsFoldingOutlineShown != null ? myIsFoldingOutlineShown : myPersistentEditorSettings.isFoldingOutlineShown();
    }

    @Override
    public void setFoldingOutlineShown(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myIsFoldingOutlineShown)) {
            return;
        }
        myIsFoldingOutlineShown = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isAutoCodeFoldingEnabled() {
        return myAutoCodeFoldingEnabled;
    }

    @Override
    public void setAutoCodeFoldingEnabled(boolean val) {
        myAutoCodeFoldingEnabled = val;
    }

    @Override
    public boolean isUseTabCharacter(Project project) {
        PsiFile file = getPsiFile(project);
        return myUseTabCharacter != null ? myUseTabCharacter : CodeStyleSettingsManager.getSettings(project).getIndentOptionsByFile(file).USE_TAB_CHARACTER;
    }

    @Override
    public void setUseTabCharacter(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myUseTabCharacter)) {
            return;
        }
        myUseTabCharacter = newValue;
        fireEditorRefresh();
    }

    @Override
    public void setSoftWrapAppliancePlace(SoftWrapAppliancePlaces softWrapAppliancePlace) {
        if (softWrapAppliancePlace != mySoftWrapAppliancePlace) {
            mySoftWrapAppliancePlace = softWrapAppliancePlace;
            fireEditorRefresh();
        }
    }

    public SoftWrapAppliancePlaces getSoftWrapAppliancePlace() {
        return mySoftWrapAppliancePlace;
    }

    public void reinitSettings() {
        myCachedTabSize = null;
        reinitDocumentIndentOptions();
    }

    private void reinitDocumentIndentOptions() {
        if (myEditor == null || myEditor.isViewer()) {
            return;
        }
        final Project project = myEditor.getProject();
        final Document document = myEditor.getDocument();

        if (project == null || project.isDisposed()) {
            return;
        }

        final PsiDocumentManager psiManager = PsiDocumentManager.getInstance(project);
        final PsiFile file = psiManager.getPsiFile(document);
        if (file == null) {
            return;
        }

        CodeStyle.updateDocumentIndentOptions(project, document);
    }

    @Override
    public int getTabSize(Project project) {
        if (myTabSize != null) {
            return myTabSize;
        }
        if (myCachedTabSize != null) {
            return myCachedTabSize;
        }
        int tabSize;
        if (project == null || project.isDisposed()) {
            tabSize = CodeStyleSettingsManager.getSettings(null).getTabSize(null);
        }
        else {
            PsiFile file = getPsiFile(project);
            if (myEditor != null && myEditor.isViewer()) {
                FileType fileType = file != null ? file.getFileType() : null;
                tabSize = CodeStyleSettingsManager.getSettings(project).getIndentOptions(fileType).TAB_SIZE;
            }
            else {
                tabSize = CodeStyleSettingsManager.getSettings(project).getIndentOptionsByFile(file).TAB_SIZE;
            }
        }
        myCachedTabSize = tabSize;
        return tabSize;
    }

    @Nullable
    protected PsiFile getPsiFile(@Nullable Project project) {
        if (project != null && myEditor != null) {
            return PsiDocumentManager.getInstance(project).getPsiFile(myEditor.getDocument());
        }
        return null;
    }

    @Override
    public void setTabSize(int tabSize) {
        final Integer newValue = tabSize;
        if (newValue.equals(myTabSize)) {
            return;
        }
        myTabSize = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isSmartHome() {
        return myIsSmartHome != null ? myIsSmartHome : myPersistentEditorSettings.isSmartHome();
    }

    @Override
    public void setSmartHome(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myIsSmartHome)) {
            return;
        }
        myIsSmartHome = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isVirtualSpace() {
        if (myEditor != null && myEditor.isColumnMode()) {
            return true;
        }
        return myIsVirtualSpace != null ? myIsVirtualSpace : myPersistentEditorSettings.isVirtualSpace();
    }

    @Override
    public void setVirtualSpace(boolean allow) {
        final Boolean newValue = allow;
        if (newValue.equals(myIsVirtualSpace)) {
            return;
        }
        myIsVirtualSpace = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isAdditionalPageAtBottom() {
        return myIsAdditionalPageAtBottom != null ? myIsAdditionalPageAtBottom : myPersistentEditorSettings.isAdditionalPageAtBottom();
    }

    @Override
    public void setAdditionalPageAtBottom(boolean val) {
        myIsAdditionalPageAtBottom = val;
    }

    @Override
    public boolean isCaretInsideTabs() {
        if (myEditor != null && myEditor.isColumnMode()) {
            return true;
        }
        return myIsCaretInsideTabs != null ? myIsCaretInsideTabs : myPersistentEditorSettings.isCaretInsideTabs();
    }

    @Override
    public void setCaretInsideTabs(boolean allow) {
        final Boolean newValue = allow;
        if (newValue.equals(myIsCaretInsideTabs)) {
            return;
        }
        myIsCaretInsideTabs = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isBlockCursor() {
        return myIsBlockCursor != null ? myIsBlockCursor : myPersistentEditorSettings.isBlockCursor();
    }

    @Override
    public void setBlockCursor(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myIsBlockCursor)) {
            return;
        }
        myIsBlockCursor = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isCaretRowShown() {
        return myCaretRowShown != null ? myCaretRowShown : myPersistentEditorSettings.isCaretRowShown();
    }

    @Override
    public void setCaretRowShown(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myCaretRowShown)) {
            return;
        }
        myCaretRowShown = newValue;
        fireEditorRefresh();
    }

    @Override
    public int getLineCursorWidth() {
        return myLineCursorWidth;
    }

    @Override
    public void setLineCursorWidth(int width) {
        myLineCursorWidth = width;
    }

    @Override
    public boolean isAnimatedScrolling() {
        return myIsAnimatedScrolling != null ? myIsAnimatedScrolling : myPersistentEditorSettings.isSmoothScrolling();
    }

    @Override
    public void setAnimatedScrolling(boolean val) {
        myIsAnimatedScrolling = val ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isCamelWords() {
        return myIsCamelWords != null ? myIsCamelWords : myPersistentEditorSettings.isCamelWords();
    }

    @Override
    public void setCamelWords(boolean val) {
        myIsCamelWords = val ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public void resetCamelWords() {
        myIsCamelWords = null;
    }

    @Override
    public boolean isBlinkCaret() {
        return myIsCaretBlinking != null ? myIsCaretBlinking : myPersistentEditorSettings.isBlinkCaret();
    }

    @Override
    public void setBlinkCaret(boolean val) {
        final Boolean newValue = val ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myIsCaretBlinking)) {
            return;
        }
        myIsCaretBlinking = newValue;
        fireEditorRefresh();
    }

    @Override
    public int getCaretBlinkPeriod() {
        return myCaretBlinkingPeriod != null ? myCaretBlinkingPeriod : myPersistentEditorSettings.getBlinkPeriod();
    }

    @Override
    public void setCaretBlinkPeriod(int blinkPeriod) {
        final Integer newValue = blinkPeriod;
        if (newValue.equals(myCaretBlinkingPeriod)) {
            return;
        }
        myCaretBlinkingPeriod = newValue;
        fireEditorRefresh();
    }

    @Override
    public boolean isDndEnabled() {
        return myIsDndEnabled != null ? myIsDndEnabled : myPersistentEditorSettings.isDndEnabled();
    }

    @Override
    public void setDndEnabled(boolean val) {
        myIsDndEnabled = val ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isWheelFontChangeEnabled() {
        return myIsWheelFontChangeEnabled != null ? myIsWheelFontChangeEnabled : myPersistentEditorSettings.isWheelFontChangeEnabled();
    }

    @Override
    public void setWheelFontChangeEnabled(boolean val) {
        myIsWheelFontChangeEnabled = val ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isMouseClickSelectionHonorsCamelWords() {
        return myIsMouseClickSelectionHonorsCamelWords != null
            ? myIsMouseClickSelectionHonorsCamelWords
            : myPersistentEditorSettings.isMouseClickSelectionHonorsCamelWords();
    }

    @Override
    public void setMouseClickSelectionHonorsCamelWords(boolean val) {
        myIsMouseClickSelectionHonorsCamelWords = val ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isVariableInplaceRenameEnabled() {
        return myIsRenameVariablesInplace != null ? myIsRenameVariablesInplace : myPersistentEditorSettings.isVariableInplaceRenameEnabled();
    }

    @Override
    public void setVariableInplaceRenameEnabled(boolean val) {
        myIsRenameVariablesInplace = val ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isRefrainFromScrolling() {
        if (myIsRefrainFromScrolling != null) {
            return myIsRefrainFromScrolling;
        }
        return myPersistentEditorSettings.isRefrainFromScrolling();
    }


    @Override
    public void setRefrainFromScrolling(boolean b) {
        myIsRefrainFromScrolling = b ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isUseSoftWraps() {
        return myUseSoftWraps != null ? myUseSoftWraps : myPersistentEditorSettings.isUseSoftWraps(mySoftWrapAppliancePlace);
    }

    @Override
    public void setUseSoftWraps(boolean use) {
        final Boolean newValue = use ? Boolean.TRUE : Boolean.FALSE;
        if (newValue.equals(myUseSoftWraps)) {
            return;
        }
        myUseSoftWraps = newValue;
        fireEditorRefresh();
    }

    public void setUseSoftWrapsQuiet() {
        myUseSoftWraps = Boolean.TRUE;
    }

    @Override
    public boolean isAllSoftWrapsShown() {
        return myIsAllSoftWrapsShown != null ? myIsWhitespacesShown : myPersistentEditorSettings.isAllSoftWrapsShown();
    }

    @Override
    public boolean isUseCustomSoftWrapIndent() {
        return myUseCustomSoftWrapIndent == null ? myPersistentEditorSettings.isUseCustomSoftWrapIndent() : myUseCustomSoftWrapIndent;
    }

    @Override
    public void setUseCustomSoftWrapIndent(boolean useCustomSoftWrapIndent) {
        myUseCustomSoftWrapIndent = useCustomSoftWrapIndent;
    }

    @Override
    public int getCustomSoftWrapIndent() {
        return myCustomSoftWrapIndent == null ? myPersistentEditorSettings.getCustomSoftWrapIndent() : myCustomSoftWrapIndent;
    }

    @Override
    public void setCustomSoftWrapIndent(int indent) {
        myCustomSoftWrapIndent = indent;
    }

    @Override
    public boolean isAllowSingleLogicalLineFolding() {
        return myAllowSingleLogicalLineFolding;
    }

    @Override
    public void setAllowSingleLogicalLineFolding(boolean allow) {
        myAllowSingleLogicalLineFolding = allow;
    }

    private void fireEditorRefresh() {
        if (myEditor != null) {
            myEditor.reinitSettings();
        }
    }

    @Override
    public boolean isPreselectRename() {
        return myRenamePreselect == null ? myPersistentEditorSettings.isPreselectRename() : myRenamePreselect;
    }

    @Override
    public void setPreselectRename(boolean val) {
        myRenamePreselect = val;
    }

    @Override
    public boolean isShowIntentionBulb() {
        return myShowIntentionBulb == null ? myPersistentEditorSettings.isShowIntentionBulb() : myShowIntentionBulb;
    }

    @Override
    public void setShowIntentionBulb(boolean show) {
        myShowIntentionBulb = show;
    }

    @Override
    public boolean isHighlightSelectionOccurrences() {
        return myIsHighlightSelectionOccurrences == null ? myPersistentEditorSettings.isHighlightSelectionOccurrences() : myIsHighlightSelectionOccurrences;
    }

    @Override
    public void setHighlightSelectionOccurrences(boolean val) {
        myIsHighlightSelectionOccurrences = val;
    }

    @Override
    public boolean isStickyLineShown() {
        if (myStickyLinesEnabled != null) {
            return myStickyLinesEnabled;
        }
        return myPersistentEditorSettings.isStickyLineShown();
    }

    @Override
    public void setStickyLinesShown(boolean value) {
        myStickyLinesEnabled = value;
    }

    @Override
    public int getStickyLinesLimit() {
        if (myStickLinesCount != null) {
            return myStickLinesCount;
        }
        return myPersistentEditorSettings.getStickyLinesLimit();
    }

    @Override
    public void setStickyLinesLimit(int value) {
        myStickLinesCount = value;
    }
}
