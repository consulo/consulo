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
package consulo.ui.ex.internal;

import consulo.ui.ex.action.Shortcut;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 27/06/2023
 */
public interface HelpTooltip {
  /**
   * Sets tooltip title. If it's longer than 2 lines (fitting in 250 pixels each) then
   * the text is automatically stripped to the word boundary and dots are added to the end.
   *
   * @param title text for title.
   * @return {@code this}
   */
  HelpTooltip setTitle(@Nullable String title);


  /**
   * Sets text for the shortcut placeholder.
   *
   * @param shortcut text for shortcut.
   * @return {@code this}
   */
  HelpTooltip setShortcut(@Nullable String shortcut);

  HelpTooltip setShortcut(@Nullable Shortcut shortcut);

  /**
   * Sets description text.
   *
   * @param description text for description.
   * @return {@code this}
   */
  HelpTooltip setDescription(@Nullable String description);

  /**
   * Enables link in the tooltip below description and sets action for it.
   *
   * @param linkText   text to show in the link.
   * @param linkAction action to execute when link is clicked.
   * @return {@code this}
   */
  public HelpTooltip setLink(String linkText, Runnable linkAction);
}
