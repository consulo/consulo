/*-
 * #%L
 * XTerm Console Addon
 * %%
 * Copyright (C) 2020 - 2023 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.flowingcode.vaadin.addons.xterm;

import com.vaadin.flow.component.HasElement;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Add clipboard support to XTerm. This provides handling of Ctrl-C (copy) and Ctrl-V (paste)
 * shortcuts, and optional click-to-copy and click-to-paste. The clipboard target is either the
 * system clipboard or an internal buffer.
 */
public interface ITerminalClipboard extends HasElement {

  public enum UseSystemClipboard {
    /** Copy and paste from the internal buffer */
    FALSE,
    /** Copy to system clipboard, paste from internal buffer */
    WRITE,
    /** Copy and paste from the system clipboard */
    READWRITE;
  }

  /** Configure the clipboard to write (and optionally read from) the system clipboard */
  default void setUseSystemClipboard(UseSystemClipboard value) {
    getElement().setProperty("useSystemClipboard", value.toString().toLowerCase(Locale.ENGLISH));
  }

  /** Handle mouse right click as a paste action */
  default void setPasteWithRightClick(boolean value) {
    getElement().setProperty("pasteWithRightClick", value);
  }

  /** Handle mouse middle click as a paste action */
  default void setPasteWithMiddleClick(boolean value) {
    getElement().setProperty("pasteWithMiddleClick", value);
  }

  /** Automatically copy to the clipboard the select text. */
  default void setCopySelection(boolean value) {
    getElement().setProperty("copySelection", value);
  }

  /** Return whether copy and paste actions use the system clipboard */
  default UseSystemClipboard getUseSystemClipboard() {
    String value = getElement().getProperty("useSystemClipboard", "");
    return Stream.of(UseSystemClipboard.values())
        .filter(e -> e.toString().toLowerCase(Locale.ENGLISH).equals(value))
        .findAny()
        .orElse(UseSystemClipboard.FALSE);
  }

  /** Return whether mouse right click is handled as a paste action */
  default boolean isPasteWithRightClick() {
    return getElement().getProperty("pasteWithRightClick", false);
  }

  /** Return whether mouse middle click is handled as a paste action */
  default boolean isPasteWithMiddleClick() {
    return getElement().getProperty("pasteWithMiddleClick", false);
  }

  /** Return whether text is copied to the clipboard when selected. */
  default boolean isCopySelection() {
    return getElement().getProperty("copySelection", false);
  }
}
