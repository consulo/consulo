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
import java.util.concurrent.CompletableFuture;

/** The API that represents an xterm.js terminal. */
public interface ITerminal extends Serializable {

  /** Unfocus the terminal. */
  void blur();

  /** Focus the terminal. */
  void focus();

  /** Gets whether the terminal has an active selection. */
  CompletableFuture<Boolean> hasSelection();

  /**
   * Gets the terminal's current selection, this is useful for implementing copy behavior outside of
   * xterm.js.
   */
  CompletableFuture<String> getSelection();

  // Gets the selection position or undefined if there is no selection./
  // getSelectionPosition(): ISelectionPosition | undefined;

  /** Clears the current terminal selection. */
  void clearSelection();

  /**
   * Selects text within the terminal.
   *
   * @param column The column the selection starts at.
   * @param row The row the selection starts at.
   * @param length The length of the selection.
   */
  void select(int column, int row, int length);

  /** Selects all text within the terminal. */
  void selectAll();

  /**
   * Selects text in the buffer between 2 lines.
   *
   * @param start The 0-based line index to select from (inclusive).
   * @param end The 0-based line index to select to (inclusive).
   */
  void selectLines(int start, int end);

  /**
   * Scroll the display of the terminal
   *
   * @param amount The number of lines to scroll down (negative scroll up).
   */
  void scrollLines(int amount);

  /**
   * Scroll the display of the terminal by a number of pages.
   *
   * @param pageCount The number of pages to scroll (negative scrolls up).
   */
  void scrollPages(int pageCount);

  /** Scrolls the display of the terminal to the top. */
  void scrollToTop();

  /** Scrolls the display of the terminal to the bottom. */
  void scrollToBottom();

  /**
   * Scrolls to a line within the buffer.
   *
   * @param line The 0-based line index to scroll to.
   */
  void scrollToLine(int line);

  /** Clear the entire buffer, making the prompt line the new first line. */
  void clear();

  /**
   * Write data to the terminal.
   *
   * @param data The data to write to the terminal.
   */
  void write(String data);

  /**
   * Writes data to the terminal, followed by a break line character (\n).
   *
   * @param data The data to write to the terminal. This can either be raw bytes given as Uint8Array
   *     from the pty or a string. Raw bytes will always be treated as UTF-8 encoded, string data as
   *     UTF-16.
   */
  void writeln(String data);

  /**
   * Writes text to the terminal, performing the necessary transformations for pasted text.
   *
   * @param data The text to write to the terminal.
   */
  void paste(String data);

  /**
   * Tells the renderer to refresh terminal content between two rows (inclusive) at the next
   * opportunity.
   *
   * @param start The row to start from (between 0 and this.rows - 1).
   * @param end The row to end at (between start and this.rows - 1).
   */
  void refresh(int start, int end);

  /** Perform a full reset (RIS, aka '\x1bc'). */
  void reset();

  /** Resizes the terminal. Set the number of columns and rows in the terminal. */
  void resize(int columns, int rows);
}
