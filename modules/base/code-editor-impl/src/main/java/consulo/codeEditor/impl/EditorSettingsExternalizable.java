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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.BidiTextDirection;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ui.ex.UINumericRange;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;
import org.intellij.lang.annotations.MagicConstant;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Singleton
@ServiceImpl
@State(name = "EditorSettings", storages = {@Storage("editor.xml")})
public class EditorSettingsExternalizable implements PersistentStateComponent<EditorSettingsExternalizable.OptionSet>, PersistentEditorSettings {
    public static final UINumericRange BLINKING_RANGE = new UINumericRange(500, 10, 1500);
    public static final UINumericRange TOOLTIPS_DELAY_RANGE = new UINumericRange(500, 1, 5000);

    //Q: make it interface?
    public static final class OptionSet {
        public String LINE_SEPARATOR;
        public String USE_SOFT_WRAPS;
        public boolean USE_CUSTOM_SOFT_WRAP_INDENT = false;
        public int CUSTOM_SOFT_WRAP_INDENT = 0;
        public boolean IS_VIRTUAL_SPACE = false;
        public boolean IS_CARET_INSIDE_TABS;
        public String STRIP_TRAILING_SPACES = STRIP_TRAILING_SPACES_CHANGED;
        public boolean IS_ENSURE_NEWLINE_AT_EOF = false;
        public boolean SHOW_QUICK_DOC_ON_MOUSE_OVER_ELEMENT = true;
        public int TOOLTIPS_DELAY_MS = TOOLTIPS_DELAY_RANGE.initial;
        public boolean SHOW_INTENTION_BULB = true;
        public boolean IS_CARET_BLINKING = true;
        public int CARET_BLINKING_PERIOD = BLINKING_RANGE.initial;
        public boolean IS_RIGHT_MARGIN_SHOWN = true;
        public boolean ARE_LINE_NUMBERS_SHOWN = true;
        public boolean ARE_GUTTER_ICONS_SHOWN = true;
        public boolean IS_FOLDING_OUTLINE_SHOWN = true;
        public boolean SHOW_BREADCRUMBS = true;
        public boolean SHOW_BREADCRUMBS_ABOVE = false;
        public boolean SHOW_INSPECTION_WIDGET = true;
        public boolean IS_FOLDING_ENDINGS_SHOWN = false;
        public boolean IS_HIGHLIGHT_SELECTION_OCCURRENCES = true;

        public boolean SMART_HOME = true;

        public boolean IS_BLOCK_CURSOR = false;
        public boolean IS_WHITESPACES_SHOWN = false;
        public boolean IS_LEADING_WHITESPACES_SHOWN = true;
        public boolean IS_INNER_WHITESPACES_SHOWN = true;
        public boolean IS_TRAILING_WHITESPACES_SHOWN = true;
        @SuppressWarnings("SpellCheckingInspection")
        public boolean IS_ALL_SOFTWRAPS_SHOWN = false;
        public boolean IS_INDENT_GUIDES_SHOWN = true;
        public boolean IS_ANIMATED_SCROLLING = true;
        public boolean IS_CAMEL_WORDS = false;
        public boolean ADDITIONAL_PAGE_AT_BOTTOM = false;

        public boolean IS_DND_ENABLED = true;
        @SuppressWarnings("SpellCheckingInspection")
        public boolean IS_WHEEL_FONTCHANGE_ENABLED = false;
        public boolean IS_MOUSE_CLICK_SELECTION_HONORS_CAMEL_WORDS = true;

        public boolean RENAME_VARIABLES_INPLACE = true;
        public boolean PRESELECT_RENAME = true;
        public boolean SHOW_INLINE_DIALOG = true;

        public boolean REFRAIN_FROM_SCROLLING = false;

        public boolean SHOW_NOTIFICATION_AFTER_REFORMAT_CODE_ACTION = true;
        public boolean SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION = true;

        public boolean ADD_CARETS_ON_DOUBLE_CTRL = true;

        public BidiTextDirection BIDI_TEXT_DIRECTION = BidiTextDirection.CONTENT_BASED;

        public boolean SHOW_PARAMETER_NAME_HINTS = true;
        public int MIN_PARAM_NAME_LENGTH_TO_SHOW = 3;
        public int MIN_PARAMS_TO_SHOW = 2;

        public boolean SHOW_STICKY_LINES = true;
        public int STICKY_LINES_LIMIT = 5;

        public boolean USE_EDITOR_FONT_IN_INLAYS = false;

        public boolean KEEP_TRAILING_SPACE_ON_CARET_LINE = true;

        private final Map<String, Boolean> mapLanguageBreadcrumbs = new HashMap<>();

        @SuppressWarnings("unused")
        public Map<String, Boolean> getLanguageBreadcrumbsMap() {
            return mapLanguageBreadcrumbs;
        }

        @SuppressWarnings("unused")
        public void setLanguageBreadcrumbsMap(Map<String, Boolean> map) {
            if (this.mapLanguageBreadcrumbs != map) {
                this.mapLanguageBreadcrumbs.clear();
                this.mapLanguageBreadcrumbs.putAll(map);
            }
        }
    }

    private static final String COMPOSITE_PROPERTY_SEPARATOR = ":";

    private final Set<SoftWrapAppliancePlaces> myPlacesToUseSoftWraps = EnumSet.noneOf(SoftWrapAppliancePlaces.class);
    private OptionSet myOptions = new OptionSet();
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    private int myBlockIndent;
    //private int myTabSize = 4;
    //private boolean myUseTabCharacter = false;

    private int myAdditionalLinesCount = 10;
    private int myAdditionalColumnsCount = 20;
    private boolean myLineMarkerAreaShown = true;

    public static final String STRIP_TRAILING_SPACES_NONE = "None";
    public static final String STRIP_TRAILING_SPACES_CHANGED = "Changed";
    public static final String STRIP_TRAILING_SPACES_WHOLE = "Whole";

    @MagicConstant(stringValues = {STRIP_TRAILING_SPACES_NONE, STRIP_TRAILING_SPACES_CHANGED, STRIP_TRAILING_SPACES_WHOLE})
    public @interface StripTrailingSpaces {
    }

    public static EditorSettingsExternalizable getInstance() {
        Application application = Application.get();
        if (application.isDisposed()) {
            return new EditorSettingsExternalizable();
        }
        else {
            return (EditorSettingsExternalizable) application.getInstance(PersistentEditorSettings.class);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Nullable
    @Override
    public OptionSet getState() {
        return myOptions;
    }

    @Override
    public void loadState(OptionSet state) {
        myOptions = state;
        parseRawSoftWraps();
    }

    private void parseRawSoftWraps() {
        if (StringUtil.isEmpty(myOptions.USE_SOFT_WRAPS)) {
            return;
        }

        String[] placeNames = myOptions.USE_SOFT_WRAPS.split(COMPOSITE_PROPERTY_SEPARATOR);
        for (String placeName : placeNames) {
            try {
                SoftWrapAppliancePlaces place = SoftWrapAppliancePlaces.valueOf(placeName);
                myPlacesToUseSoftWraps.add(place);
            }
            catch (IllegalArgumentException e) {
                // Ignore bad value
            }
        }

        // There is a possible case that there were invalid/old format values. We want to replace them by up-to-date data.
        storeRawSoftWraps();
    }

    private void storeRawSoftWraps() {
        StringBuilder buffer = new StringBuilder();
        for (SoftWrapAppliancePlaces placeToStore : myPlacesToUseSoftWraps) {
            buffer.append(placeToStore).append(COMPOSITE_PROPERTY_SEPARATOR);
        }
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length() - 1);
        }
        myOptions.USE_SOFT_WRAPS = buffer.toString();
    }

    public OptionSet getOptions() {
        return myOptions;
    }

    @Override
    public boolean isRightMarginShown() {
        return myOptions.IS_RIGHT_MARGIN_SHOWN;
    }

    @Override
    public void setRightMarginShown(boolean val) {
        myOptions.IS_RIGHT_MARGIN_SHOWN = val;
    }

    @Override
    public boolean isLineNumbersShown() {
        return myOptions.ARE_LINE_NUMBERS_SHOWN;
    }

    @Override
    public void setLineNumbersShown(boolean val) {
        myOptions.ARE_LINE_NUMBERS_SHOWN = val;
    }

    @Override
    public boolean areGutterIconsShown() {
        return myOptions.ARE_GUTTER_ICONS_SHOWN;
    }

    @Override
    public void setGutterIconsShown(boolean val) {
        myOptions.ARE_GUTTER_ICONS_SHOWN = val;
    }

    @Override
    public int getAdditionalLinesCount() {
        return myAdditionalLinesCount;
    }

    @Override
    public void setAdditionalLinesCount(int additionalLinesCount) {
        myAdditionalLinesCount = additionalLinesCount;
    }

    @Override
    @SuppressWarnings({"UnusedDeclaration", "SpellCheckingInspection"})
    public int getAdditinalColumnsCount() {
        return myAdditionalColumnsCount;
    }

    @Override
    public void setAdditionalColumnsCount(int value) {
        myAdditionalColumnsCount = value;
    }

    @Override
    public boolean isLineMarkerAreaShown() {
        return myLineMarkerAreaShown;
    }

    @Override
    public void setLineMarkerAreaShown(boolean lineMarkerAreaShown) {
        myLineMarkerAreaShown = lineMarkerAreaShown;
    }

    @Override
    public boolean isFoldingOutlineShown() {
        return myOptions.IS_FOLDING_OUTLINE_SHOWN;
    }

    @Override
    public void setFoldingOutlineShown(boolean val) {
        myOptions.IS_FOLDING_OUTLINE_SHOWN = val;
    }

    /**
     * @return {@code true} if breadcrumbs should be shown above the editor, {@code false} otherwise
     */
    @Override
    public boolean isBreadcrumbsAbove() {
        return myOptions.SHOW_BREADCRUMBS_ABOVE;
    }

    /**
     * @param value {@code true} if breadcrumbs should be shown above the editor, {@code false} otherwise
     * @return {@code true} if an option was modified, {@code false} otherwise
     */
    @Override
    public boolean setBreadcrumbsAbove(boolean value) {
        if (myOptions.SHOW_BREADCRUMBS_ABOVE == value) {
            return false;
        }
        myOptions.SHOW_BREADCRUMBS_ABOVE = value;
        return true;
    }

    /**
     * @return {@code true} if breadcrumbs should be shown, {@code false} otherwise
     */
    @Override
    public boolean isBreadcrumbsShown() {
        return myOptions.SHOW_BREADCRUMBS;
    }

    /**
     * @param value {@code true} if breadcrumbs should be shown, {@code false} otherwise
     * @return {@code true} if an option was modified, {@code false} otherwise
     */
    @Override
    public boolean setBreadcrumbsShown(boolean value) {
        if (myOptions.SHOW_BREADCRUMBS == value) {
            return false;
        }
        myOptions.SHOW_BREADCRUMBS = value;
        return true;
    }

    /**
     * @param languageID the language identifier to configure
     * @return {@code true} if breadcrumbs should be shown for the specified language, {@code false} otherwise
     */
    @Override
    public boolean isBreadcrumbsShownFor(String languageID) {
        Boolean visible = myOptions.mapLanguageBreadcrumbs.get(languageID);
        return visible == null || visible;
    }

    @Override
    public boolean hasBreadcrumbSettings(String languageID) {
        return myOptions.mapLanguageBreadcrumbs.containsKey(languageID);
    }

    /**
     * @param languageID the language identifier to configure
     * @param value      {@code true} if breadcrumbs should be shown for the specified language, {@code false} otherwise
     * @return {@code true} if an option was modified, {@code false} otherwise
     */
    @Override
    public boolean setBreadcrumbsShownFor(String languageID, boolean value) {
        Boolean visible = myOptions.mapLanguageBreadcrumbs.put(languageID, value);
        return (visible == null || visible) != value;
    }

    @Override
    public boolean isBlockCursor() {
        return myOptions.IS_BLOCK_CURSOR;
    }

    @Override
    public void setBlockCursor(boolean val) {
        myOptions.IS_BLOCK_CURSOR = val;
    }

    @Override
    public boolean isCaretRowShown() {
        return true;
    }

    @Override
    public int getBlockIndent() {
        return myBlockIndent;
    }

    @Override
    public void setBlockIndent(int blockIndent) {
        myBlockIndent = blockIndent;
    }

    @Override
    public boolean isSmartHome() {
        return myOptions.SMART_HOME;
    }

    @Override
    public void setSmartHome(boolean val) {
        myOptions.SMART_HOME = val;
    }

    @Override
    public boolean isUseSoftWraps() {
        return isUseSoftWraps(SoftWrapAppliancePlaces.MAIN_EDITOR);
    }

    @Override
    public boolean isUseSoftWraps(@Nonnull SoftWrapAppliancePlaces place) {
        return myPlacesToUseSoftWraps.contains(place);
    }

    @Override
    public void setUseSoftWraps(boolean use) {
        setUseSoftWraps(use, SoftWrapAppliancePlaces.MAIN_EDITOR);
    }

    @Override
    public void setUseSoftWraps(boolean use, @Nonnull SoftWrapAppliancePlaces place) {
        boolean update = use ^ myPlacesToUseSoftWraps.contains(place);
        if (!update) {
            return;
        }

        if (use) {
            myPlacesToUseSoftWraps.add(place);
        }
        else {
            myPlacesToUseSoftWraps.remove(place);
        }
        storeRawSoftWraps();
    }

    @Override
    public boolean isUseCustomSoftWrapIndent() {
        return myOptions.USE_CUSTOM_SOFT_WRAP_INDENT;
    }

    @Override
    public void setUseCustomSoftWrapIndent(boolean use) {
        myOptions.USE_CUSTOM_SOFT_WRAP_INDENT = use;
    }

    @Override
    public int getCustomSoftWrapIndent() {
        return myOptions.CUSTOM_SOFT_WRAP_INDENT;
    }

    @Override
    public void setCustomSoftWrapIndent(int indent) {
        myOptions.CUSTOM_SOFT_WRAP_INDENT = indent;
    }

    @Override
    public boolean isVirtualSpace() {
        return myOptions.IS_VIRTUAL_SPACE;
    }

    @Override
    public void setVirtualSpace(boolean val) {
        myOptions.IS_VIRTUAL_SPACE = val;
    }

    @Override
    public boolean isCaretInsideTabs() {
        return myOptions.IS_CARET_INSIDE_TABS;
    }

    @Override
    public void setCaretInsideTabs(boolean val) {
        myOptions.IS_CARET_INSIDE_TABS = val;
    }

    @Override
    public boolean isBlinkCaret() {
        return myOptions.IS_CARET_BLINKING;
    }

    @Override
    public void setBlinkCaret(boolean blinkCaret) {
        myOptions.IS_CARET_BLINKING = blinkCaret;
    }

    @Override
    public int getBlinkPeriod() {
        return BLINKING_RANGE.fit(myOptions.CARET_BLINKING_PERIOD);
    }

    @Override
    public void setBlinkPeriod(int blinkInterval) {
        myOptions.CARET_BLINKING_PERIOD = BLINKING_RANGE.fit(blinkInterval);
    }


    @Override
    public boolean isEnsureNewLineAtEOF() {
        return myOptions.IS_ENSURE_NEWLINE_AT_EOF;
    }

    @Override
    public void setEnsureNewLineAtEOF(boolean ensure) {
        myOptions.IS_ENSURE_NEWLINE_AT_EOF = ensure;
    }

    @Override
    @StripTrailingSpaces
    public String getStripTrailingSpaces() {
        return myOptions.STRIP_TRAILING_SPACES;
    } // TODO: move to CodeEditorManager or something else

    @Override
    public void setStripTrailingSpaces(@StripTrailingSpaces String stripTrailingSpaces) {
        myOptions.STRIP_TRAILING_SPACES = stripTrailingSpaces;
    }

    @Override
    public boolean isShowQuickDocOnMouseOverElement() {
        return myOptions.SHOW_QUICK_DOC_ON_MOUSE_OVER_ELEMENT;
    }

    @Override
    public void setShowQuickDocOnMouseOverElement(boolean show) {
        myOptions.SHOW_QUICK_DOC_ON_MOUSE_OVER_ELEMENT = show;
    }

    /**
     * @deprecated Use {@link #getTooltipsDelay()} instead
     */
    @Override
    @Deprecated
    public int getQuickDocOnMouseOverElementDelayMillis() {
        return getTooltipsDelay();
    }

    @Override
    public int getTooltipsDelay() {
        return TOOLTIPS_DELAY_RANGE.fit(myOptions.TOOLTIPS_DELAY_MS);
    }

    @Override
    public void setTooltipsDelay(int delay) {
        myOptions.TOOLTIPS_DELAY_MS = TOOLTIPS_DELAY_RANGE.fit(delay);
    }

    @Override
    @Deprecated
    public void setQuickDocOnMouseOverElementDelayMillis(int delay) {
        setTooltipsDelay(delay);
    }

    @Override
    public boolean isShowIntentionBulb() {
        return myOptions.SHOW_INTENTION_BULB;
    }

    @Override
    public void setShowIntentionBulb(boolean show) {
        myOptions.SHOW_INTENTION_BULB = show;
    }

    @Override
    public boolean isRefrainFromScrolling() {
        return myOptions.REFRAIN_FROM_SCROLLING;
    }

    @Override
    public void setRefrainFromScrolling(boolean b) {
        myOptions.REFRAIN_FROM_SCROLLING = b;
    }

    @Override
    public boolean isWhitespacesShown() {
        return myOptions.IS_WHITESPACES_SHOWN;
    }

    @Override
    public void setWhitespacesShown(boolean val) {
        myOptions.IS_WHITESPACES_SHOWN = val;
    }

    @Override
    public boolean isLeadingWhitespacesShown() {
        return myOptions.IS_LEADING_WHITESPACES_SHOWN;
    }

    @Override
    public void setLeadingWhitespacesShown(boolean val) {
        myOptions.IS_LEADING_WHITESPACES_SHOWN = val;
    }

    @Override
    public boolean isInnerWhitespacesShown() {
        return myOptions.IS_INNER_WHITESPACES_SHOWN;
    }

    @Override
    public void setInnerWhitespacesShown(boolean val) {
        myOptions.IS_INNER_WHITESPACES_SHOWN = val;
    }

    @Override
    public boolean isTrailingWhitespacesShown() {
        return myOptions.IS_TRAILING_WHITESPACES_SHOWN;
    }

    @Override
    public void setTrailingWhitespacesShown(boolean val) {
        myOptions.IS_TRAILING_WHITESPACES_SHOWN = val;
    }

    @Override
    public boolean isAllSoftWrapsShown() {
        return myOptions.IS_ALL_SOFTWRAPS_SHOWN;
    }

    @Override
    public void setAllSoftwrapsShown(boolean val) {
        myOptions.IS_ALL_SOFTWRAPS_SHOWN = val;
    }

    @Override
    public boolean isIndentGuidesShown() {
        return myOptions.IS_INDENT_GUIDES_SHOWN;
    }

    @Override
    public void setIndentGuidesShown(boolean val) {
        myOptions.IS_INDENT_GUIDES_SHOWN = val;
    }

    @Override
    public boolean isSmoothScrolling() {
        return myOptions.IS_ANIMATED_SCROLLING;
    }

    @Override
    public void setSmoothScrolling(boolean val) {
        myOptions.IS_ANIMATED_SCROLLING = val;
    }

    @Override
    public boolean isCamelWords() {
        return myOptions.IS_CAMEL_WORDS;
    }

    @Override
    public void setCamelWords(boolean val) {
        myOptions.IS_CAMEL_WORDS = val;
    }

    @Override
    public boolean isAdditionalPageAtBottom() {
        return myOptions.ADDITIONAL_PAGE_AT_BOTTOM;
    }

    @Override
    public void setAdditionalPageAtBottom(boolean val) {
        myOptions.ADDITIONAL_PAGE_AT_BOTTOM = val;
    }

    @Override
    public boolean isDndEnabled() {
        return myOptions.IS_DND_ENABLED;
    }

    @Override
    public void setDndEnabled(boolean val) {
        myOptions.IS_DND_ENABLED = val;
    }

    @Override
    public boolean isWheelFontChangeEnabled() {
        return myOptions.IS_WHEEL_FONTCHANGE_ENABLED;
    }

    @Override
    public void setWheelFontChangeEnabled(boolean val) {
        myOptions.IS_WHEEL_FONTCHANGE_ENABLED = val;
    }

    @Override
    public boolean isMouseClickSelectionHonorsCamelWords() {
        return myOptions.IS_MOUSE_CLICK_SELECTION_HONORS_CAMEL_WORDS;
    }

    @Override
    public void setMouseClickSelectionHonorsCamelWords(boolean val) {
        myOptions.IS_MOUSE_CLICK_SELECTION_HONORS_CAMEL_WORDS = val;
    }

    @Override
    public boolean isVariableInplaceRenameEnabled() {
        return myOptions.RENAME_VARIABLES_INPLACE;
    }

    @Override
    public void setVariableInplaceRenameEnabled(final boolean val) {
        myOptions.RENAME_VARIABLES_INPLACE = val;
    }

    @Override
    public boolean isPreselectRename() {
        return myOptions.PRESELECT_RENAME;
    }

    @Override
    public void setPreselectRename(final boolean val) {
        myOptions.PRESELECT_RENAME = val;
    }

    @Override
    public boolean isShowInlineLocalDialog() {
        return myOptions.SHOW_INLINE_DIALOG;
    }

    @Override
    public void setShowInlineLocalDialog(final boolean val) {
        myOptions.SHOW_INLINE_DIALOG = val;
    }

    @Override
    public boolean addCaretsOnDoubleCtrl() {
        return myOptions.ADD_CARETS_ON_DOUBLE_CTRL;
    }

    @Override
    public void setAddCaretsOnDoubleCtrl(boolean val) {
        myOptions.ADD_CARETS_ON_DOUBLE_CTRL = val;
    }

    @Override
    public BidiTextDirection getBidiTextDirection() {
        return myOptions.BIDI_TEXT_DIRECTION;
    }

    @Override
    public void setBidiTextDirection(BidiTextDirection direction) {
        myOptions.BIDI_TEXT_DIRECTION = direction;
    }

    @Override
    public boolean isShowParameterNameHints() {
        return myOptions.SHOW_PARAMETER_NAME_HINTS;
    }

    @Override
    public void setShowParameterNameHints(boolean value) {
        myOptions.SHOW_PARAMETER_NAME_HINTS = value;
    }

    @Override
    public int getMinParamNameLengthToShow() {
        return myOptions.MIN_PARAM_NAME_LENGTH_TO_SHOW;
    }

    @Override
    public void setMinParamNameLengthToShow(int value) {
        myOptions.MIN_PARAM_NAME_LENGTH_TO_SHOW = value;
    }

    @Override
    public int getMinArgsToShow() {
        return myOptions.MIN_PARAMS_TO_SHOW;
    }

    @Override
    public void setMinArgsToShow(int minParamsToShow) {
        myOptions.MIN_PARAMS_TO_SHOW = minParamsToShow;
    }

    @Override
    public boolean isKeepTrailingSpacesOnCaretLine() {
        return myOptions.KEEP_TRAILING_SPACE_ON_CARET_LINE;
    }

    @Override
    public void setKeepTrailingSpacesOnCaretLine(boolean keep) {
        myOptions.KEEP_TRAILING_SPACE_ON_CARET_LINE = keep;
    }

    @Override
    public boolean isShowInspectionWidget() {
        return myOptions.SHOW_INSPECTION_WIDGET;
    }

    @Override
    public void setShowInspectionWidget(boolean show) {
        myOptions.SHOW_INSPECTION_WIDGET = show;
    }

    @Override
    public boolean isFoldingEndingsShown() {
        return myOptions.IS_FOLDING_ENDINGS_SHOWN;
    }

    @Override
    public boolean isHighlightSelectionOccurrences() {
        return myOptions.IS_HIGHLIGHT_SELECTION_OCCURRENCES;
    }

    @Override
    public void setHighlightSelectionOccurrences(boolean val) {
        myOptions.IS_HIGHLIGHT_SELECTION_OCCURRENCES = val;
    }

    @Override
    public boolean isStickyLineShown() {
        return myOptions.SHOW_STICKY_LINES;
    }

    @Override
    public void setStickyLinesShown(boolean value) {
        myOptions.SHOW_STICKY_LINES = value;
    }

    @Override
    public int getStickyLinesLimit() {
        return myOptions.STICKY_LINES_LIMIT;
    }

    @Override
    public void setStickyLinesLimit(int value) {
        myOptions.STICKY_LINES_LIMIT = value;
    }

    @Override
    public boolean isUseEditorFontInInlays() {
        return myOptions.USE_EDITOR_FONT_IN_INLAYS;
    }
}