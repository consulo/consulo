/*
 * Copyright 2013-2023 consulo.io
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
package consulo.codeEditor;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14/04/2023
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PersistentEditorSettings {
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use injection")
  static PersistentEditorSettings getInstance() {
    return Application.get().getInstance(PersistentEditorSettings.class);
  }

  boolean isRightMarginShown();

  void setRightMarginShown(boolean val);

  boolean isLineNumbersShown();

  void setLineNumbersShown(boolean val);

  boolean areGutterIconsShown();

  void setGutterIconsShown(boolean val);

  int getAdditionalLinesCount();

  void setAdditionalLinesCount(int additionalLinesCount);

  int getAdditinalColumnsCount();

  void setAdditionalColumnsCount(int value);

  boolean isLineMarkerAreaShown();

  void setLineMarkerAreaShown(boolean lineMarkerAreaShown);

  boolean isFoldingOutlineShown();

  void setFoldingOutlineShown(boolean val);

  /**
   * @return {@code true} if breadcrumbs should be shown above the editor, {@code false} otherwise
   */
  boolean isBreadcrumbsAbove();

  /**
   * @param value {@code true} if breadcrumbs should be shown above the editor, {@code false} otherwise
   * @return {@code true} if an option was modified, {@code false} otherwise
   */
  boolean setBreadcrumbsAbove(boolean value);

  /**
   * @return {@code true} if breadcrumbs should be shown, {@code false} otherwise
   */
  boolean isBreadcrumbsShown();

  /**
   * @param value {@code true} if breadcrumbs should be shown, {@code false} otherwise
   * @return {@code true} if an option was modified, {@code false} otherwise
   */
  boolean setBreadcrumbsShown(boolean value);

  /**
   * @param languageID the language identifier to configure
   * @return {@code true} if breadcrumbs should be shown for the specified language, {@code false} otherwise
   */
  boolean isBreadcrumbsShownFor(String languageID);

  boolean hasBreadcrumbSettings(String languageID);

  /**
   * @param languageID the language identifier to configure
   * @param value      {@code true} if breadcrumbs should be shown for the specified language, {@code false} otherwise
   * @return {@code true} if an option was modified, {@code false} otherwise
   */
  boolean setBreadcrumbsShownFor(String languageID, boolean value);

  boolean isBlockCursor();

  void setBlockCursor(boolean val);

  boolean isCaretRowShown();

  int getBlockIndent();

  void setBlockIndent(int blockIndent);

  boolean isSmartHome();

  void setSmartHome(boolean val);

  default boolean isUseSoftWraps() {
    return isUseSoftWraps(SoftWrapAppliancePlaces.MAIN_EDITOR);
  }

  boolean isUseSoftWraps(@Nonnull SoftWrapAppliancePlaces place);

  default void setUseSoftWraps(boolean use) {
    setUseSoftWraps(use, SoftWrapAppliancePlaces.MAIN_EDITOR);
  }

  void setUseSoftWraps(boolean use, @Nonnull SoftWrapAppliancePlaces place);

  boolean isUseCustomSoftWrapIndent();

  void setUseCustomSoftWrapIndent(boolean use);

  int getCustomSoftWrapIndent();

  void setCustomSoftWrapIndent(int indent);

  boolean isVirtualSpace();

  void setVirtualSpace(boolean val);

  boolean isCaretInsideTabs();

  void setCaretInsideTabs(boolean val);

  boolean isBlinkCaret();

  void setBlinkCaret(boolean blinkCaret);

  int getBlinkPeriod();

  void setBlinkPeriod(int blinkInterval);


  boolean isEnsureNewLineAtEOF();

  void setEnsureNewLineAtEOF(boolean ensure);

  String getStripTrailingSpaces(); // TODO: move to CodeEditorManager or something else

  void setStripTrailingSpaces(String stripTrailingSpaces);

  boolean isShowQuickDocOnMouseOverElement();

  void setShowQuickDocOnMouseOverElement(boolean show);

  @Deprecated
  default int getQuickDocOnMouseOverElementDelayMillis() {
    return getTooltipsDelay();
  }

  int getTooltipsDelay();

  void setTooltipsDelay(int delay);

  @Deprecated
  default void setQuickDocOnMouseOverElementDelayMillis(int delay) {
    setTooltipsDelay(delay);
  }

  boolean isShowIntentionBulb();

  void setShowIntentionBulb(boolean show);

  boolean isRefrainFromScrolling();

  void setRefrainFromScrolling(boolean b);

  boolean isWhitespacesShown();

  void setWhitespacesShown(boolean val);

  boolean isLeadingWhitespacesShown();

  void setLeadingWhitespacesShown(boolean val);

  boolean isInnerWhitespacesShown();

  void setInnerWhitespacesShown(boolean val);

  boolean isTrailingWhitespacesShown();

  void setTrailingWhitespacesShown(boolean val);

  boolean isAllSoftWrapsShown();

  void setAllSoftwrapsShown(boolean val);

  boolean isIndentGuidesShown();

  void setIndentGuidesShown(boolean val);

  boolean isSmoothScrolling();

  void setSmoothScrolling(boolean val);

  boolean isCamelWords();

  void setCamelWords(boolean val);

  boolean isAdditionalPageAtBottom();

  void setAdditionalPageAtBottom(boolean val);

  boolean isDndEnabled();

  void setDndEnabled(boolean val);

  boolean isWheelFontChangeEnabled();

  void setWheelFontChangeEnabled(boolean val);

  boolean isMouseClickSelectionHonorsCamelWords();

  void setMouseClickSelectionHonorsCamelWords(boolean val);

  boolean isVariableInplaceRenameEnabled();

  void setVariableInplaceRenameEnabled(final boolean val);

  boolean isPreselectRename();

  void setPreselectRename(final boolean val);

  boolean isShowInlineLocalDialog();

  void setShowInlineLocalDialog(final boolean val);

  boolean addCaretsOnDoubleCtrl();

  void setAddCaretsOnDoubleCtrl(boolean val);

  BidiTextDirection getBidiTextDirection();

  void setBidiTextDirection(BidiTextDirection direction);

  boolean isShowParameterNameHints();

  void setShowParameterNameHints(boolean value);

  int getMinParamNameLengthToShow();

  void setMinParamNameLengthToShow(int value);

  int getMinArgsToShow();

  void setMinArgsToShow(int minParamsToShow);

  boolean isKeepTrailingSpacesOnCaretLine();

  void setKeepTrailingSpacesOnCaretLine(boolean keep);

  boolean isShowInspectionWidget();

  void setShowInspectionWidget(boolean show);

  boolean isFoldingEndingsShown();
}
