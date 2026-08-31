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

import java.io.Serializable;

/** Start up options for the terminal. */
public interface ITerminalOptions extends Serializable {

  /** A data uri of the sound to use for the bell when `bellStyle = 'sound'`. */
  void setBellSound(String value);

  /** The type of the bell notification the terminal will use. */
  void setBellStyle(BellStyle value);

  enum BellStyle {
    NONE,
    SOUND
  }

  /** Whether the cursor blinks. */
  void setCursorBlink(boolean value);

  /** The style of the cursor. */
  void setCursorStyle(CursorStyle value);

  enum CursorStyle {
    BLOCK,
    UNDERLINE,
    BAR
  }

  /** The width of the cursor in CSS pixels when `cursorStyle` is set to 'bar'. */
  void setCursorWidth(int value);

  /** Whether to draw bold text in bright colors. The default is true. */
  void setDrawBoldTextInBrightColors(boolean value);

  /** The modifier key hold to multiply scroll speed. */
  void setFastScrollModifier(FastScrollModifier value);

  enum FastScrollModifier {
    ALT,
    CTRL,
    SHIFT,
    UNDEFINED
  }

  /** The scroll speed multiplier used for fast scrolling. */
  void setFastScrollSensitivity(int number);

  /** The font size used to render text. */
  void setFontSize(int number);

  /** The font family used to render text. */
  void setFontFamily(String fontFamily);

  /** The font weight used to render non-bold text. */
  void setFontWeight(int value);

  /** The font weight used to render bold text. */
  void setFontWeightBold(int value);

  /** The spacing in whole pixels between characters.. */
  void setLetterSpacing(int value);

  /** The line height used to render text. */
  void setLineHeight(int value);

  /** Whether to treat option as the meta key. */
  void setMacOptionIsMeta(boolean value);

  /**
   * Whether holding a modifier key will force normal selection behavior, regardless of whether the
   * terminal is in mouse events mode. This will also prevent mouse events from being emitted by the
   * terminal. For example, this allows you to use xterm.js' regular selection inside tmux with
   * mouse mode enabled.
   */
  void setMacOptionClickForcesSelection(boolean value);

  /**
   * The minimum contrast ratio for text in the terminal, setting this will change the foreground
   * color dynamically depending on whether the contrast ratio is met. Example values:
   *
   * <ul>
   *   <li>1: The default, do nothing.
   *   <li>4.5: Minimum for WCAG AA compliance.
   *   <li>7: Minimum for WCAG AAA compliance.
   *   <li>21: White on black or black on white.
   * </ul>
   */
  void setMinimumContrastRatio(int value);

  /** The color theme of the terminal. */
  void setTheme(TerminalTheme theme);

  /**
   * The type of renderer to use, this allows using the fallback DOM renderer when canvas is too
   * slow for the environment. The following features do not work when the DOM renderer is used:
   *
   * <p>- Letter spacing - Cursor blink
   */
  void setRendererType(RendererType value);

  enum RendererType {
    DOM,
    CANVAS
  }

  /**
   * Whether to select the word under the cursor on right click, this is standard behavior in a lot
   * of macOS applications.
   */
  void setRightClickSelectsWord(boolean value);

  /**
   * Whether screen reader support is enabled. When on this will expose supporting elements in the
   * DOM to support NVDA on Windows and VoiceOver on macOS.
   */
  void setScreenReaderMode(boolean value);

  /**
   * The amount of scrollback in the terminal. Scrollback is the amount of rows that are retained
   * when lines are scrolled beyond the initial viewport.
   */
  void setScrollback(int value);

  /** The scrolling speed multiplier used for adjusting normal scrolling speed. */
  void setScrollSensitivity(int value);

  /** The size of tab stops in the terminal. */
  void setTabStopWidth(int value);

  /**
   * A string containing all characters that are considered word separated by the double click to
   * select work logic.
   */
  void setWordSeparator(String value);
}
