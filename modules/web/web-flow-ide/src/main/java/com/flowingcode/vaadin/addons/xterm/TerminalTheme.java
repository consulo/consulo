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

import com.vaadin.flow.internal.JacksonUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import tools.jackson.databind.node.ObjectNode;

/** The color theme of the terminal. */
public final class TerminalTheme {

  /** Set the default foreground color */
  private String foreground;

  /** Set the default background color */
  private String background;

  /** Set the cursor color */
  private String cursor;

  /** Set the accent color of the cursor (fg color for a block cursor) */
  private String cursorAccent;

  /** Set the selection background color (can be transparent) */
  private String selection;

  /** ANSI black (eg. `\x1b[30m`) */
  private String black;

  /** ANSI red (eg. `\x1b[31m`) */
  private String red;

  /** ANSI green (eg. `\x1b[32m`) */
  private String green;

  /** ANSI yellow (eg. `\x1b[33m`) */
  private String yellow;

  /** ANSI blue (eg. `\x1b[34m`) */
  private String blue;

  /** ANSI magenta (eg. `\x1b[35m`) */
  private String magenta;

  /** ANSI cyan (eg. `\x1b[36m`) */
  private String cyan;

  /** ANSI white (eg. `\x1b[37m`) */
  private String white;

  /** ANSI bright black (eg. `\x1b[1;30m`) */
  private String brightBlack;

  /** ANSI bright red (eg. `\x1b[1;31m`) */
  private String brightRed;

  /** ANSI bright green (eg. `\x1b[1;32m`) */
  private String brightGreen;

  /** ANSI bright yellow (eg. `\x1b[1;33m`) */
  private String brightYellow;

  /** ANSI bright blue (eg. `\x1b[1;34m`) */
  private String brightBlue;

  /** ANSI bright magenta (eg. `\x1b[1;35m`) */
  private String brightMagenta;

  /** ANSI bright cyan (eg. `\x1b[1;36m`) */
  private String brightCyan;

  /** ANSI bright white (eg. `\x1b[1;37m`) */
  private String brightWhite;

  public TerminalTheme() {
  }

  private TerminalTheme(TerminalTheme other) {
    foreground = other.foreground;
    background = other.background;
    cursor = other.cursor;
    cursorAccent = other.cursorAccent;
    selection = other.selection;
    black = other.black;
    red = other.red;
    green = other.green;
    yellow = other.yellow;
    blue = other.blue;
    magenta = other.magenta;
    cyan = other.cyan;
    white = other.white;
    brightBlack = other.brightBlack;
    brightRed = other.brightRed;
    brightGreen = other.brightGreen;
    brightYellow = other.brightYellow;
    brightBlue = other.brightBlue;
    brightMagenta = other.brightMagenta;
    brightCyan = other.brightCyan;
    brightWhite = other.brightWhite;
  }

  public TerminalTheme withForeground(String foreground) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.foreground = foreground;
    return theme;
  }

  public TerminalTheme withBackground(String background) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.background = background;
    return theme;
  }

  public TerminalTheme withCursor(String cursor) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.cursor = cursor;
    return theme;
  }

  public TerminalTheme withCursorAccent(String cursorAccent) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.cursorAccent = cursorAccent;
    return theme;
  }

  public TerminalTheme withSelection(String selection) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.selection = selection;
    return theme;
  }

  public TerminalTheme withBlack(String black) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.black = black;
    return theme;
  }

  public TerminalTheme withRed(String red) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.red = red;
    return theme;
  }

  public TerminalTheme withGreen(String green) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.green = green;
    return theme;
  }

  public TerminalTheme withYellow(String yellow) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.yellow = yellow;
    return theme;
  }

  public TerminalTheme withBlue(String blue) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.blue = blue;
    return theme;
  }

  public TerminalTheme withMagenta(String magenta) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.magenta = magenta;
    return theme;
  }

  public TerminalTheme withCyan(String cyan) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.cyan = cyan;
    return theme;
  }

  public TerminalTheme withWhite(String white) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.white = white;
    return theme;
  }

  public TerminalTheme withBrightBlack(String brightBlack) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.brightBlack = brightBlack;
    return theme;
  }

  public TerminalTheme withBrightRed(String brightRed) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.brightRed = brightRed;
    return theme;
  }

  public TerminalTheme withBrightGreen(String brightGreen) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.brightGreen = brightGreen;
    return theme;
  }

  public TerminalTheme withBrightYellow(String brightYellow) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.brightYellow = brightYellow;
    return theme;
  }

  public TerminalTheme withBrightBlue(String brightBlue) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.brightBlue = brightBlue;
    return theme;
  }

  public TerminalTheme withBrightMagenta(String brightMagenta) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.brightMagenta = brightMagenta;
    return theme;
  }

  public TerminalTheme withBrightCyan(String brightCyan) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.brightCyan = brightCyan;
    return theme;
  }

  public TerminalTheme withBrightWhite(String brightWhite) {
    TerminalTheme theme = new TerminalTheme(this);
    theme.brightWhite = brightWhite;
    return theme;
  }

  ObjectNode asJsonObject() {
    ObjectNode obj = JacksonUtils.createObjectNode();
    for (Field field : this.getClass().getDeclaredFields()) {
      if (field.getType() != String.class || Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      try {
        String value = (String) field.get(this);
        obj.set(field.getName(), value == null ? JacksonUtils.nullNode() : JacksonUtils.createNode(value));
      } catch (Exception e) {
        throw new UndeclaredThrowableException(e);
      }
    }
    return obj;
  }
}
