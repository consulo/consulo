/*
 * Copyright 2013-2020 consulo.io
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
package com.intellij.openapi.editor.markup;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.util.ui.GridBag;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;

/**
 * <code>UIController</code> contains methods for filling inspection widget popup and
 * reacting to changes in the popup.
 * Created lazily only when needed and once for every <code>AnalyzerStatus</code> instance.
 * <p>
 * from kotlin
 */
public interface UIController {
  /**
   * Returns <code>true</code> if the inspection widget can be visible as a toolbar or
   * <code>false</code> if it can be visible as an icon above the scrollbar only.
   */
  boolean enableToolbar();

  /**
   * Contains all possible actions in the settings menu. The <code>List</code> is wrapped
   * in ActionGroup at the UI creation level in <code>EditorMarkupModelImpl</code>
   */
  @Nonnull
  List<AnAction> getActions();

  /**
   * Lists possible <code>InspectionLevel</code>s for the particular file.
   */
  @Nonnull
  List<InspectionsLevel> getAvailableLevels();

  /**
   * Lists highlight levels for the particular file per language if the file
   * contains several languages.
   */
  @Nonnull
  List<LanguageHighlightLevel> getHighlightLevels();

  /**
   * Saves the <code>LanguageHighlightLevel</code> for the file.
   */
  void setHighLightLevel(@Nonnull LanguageHighlightLevel newLevels);

  /**
   * Adds panels coming from <code>com.intellij.hectorComponentProvider</code> EP providers to
   * the inspection widget popup.
   */
  void fillHectorPanels(@Nonnull Container container, @Nonnull GridBag bag);

  /**
   * Can the inspection widget popup be closed. Might be necessary to complete some
   * settings in hector panels before closing the popup.
   * If a panel can be closed and is modified then the settings are applied for the panel.
   */
  boolean canClosePopup();

  /**
   * Called after the popup has been closed. Usually used to dispose resources held by
   * hector panels.
   */
  void onClosePopup();

  void openProblemsView();
}
