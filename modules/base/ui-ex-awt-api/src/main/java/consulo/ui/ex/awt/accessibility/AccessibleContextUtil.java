/*
 * Copyright (C) 2015 The Android Open Source Project
 *
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
 */
package consulo.ui.ex.awt.accessibility;

import consulo.util.lang.StringUtil;

import javax.accessibility.Accessible;
import org.jspecify.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

public class AccessibleContextUtil {
  //@VisibleForTesting
  static final String PUNCTUATION_CHARACTER = ".";
  //@VisibleForTesting
  static final String PUNCTUATION_SEPARATOR = "  ";

  public static void setName(JComponent component, String name) {
    component.getAccessibleContext().setAccessibleName(name);
  }

  public static void setName(JComponent component, JComponent source) {
    String name = source.getAccessibleContext().getAccessibleName();
    if (name != null) {
      component.getAccessibleContext().setAccessibleName(name);
    }
  }

  public static void setCombinedName(JComponent component,
                                     JComponent j1, String separator, JComponent j2) {
    component.getAccessibleContext().setAccessibleName(
            combineAccessibleStrings(
                    j1.getAccessibleContext().getAccessibleName(),
                    separator,
                    j2.getAccessibleContext().getAccessibleName()));
  }

  public static void setCombinedName(JComponent component,
                                     JComponent j1, String separator1,
                                     JComponent j2, String separator2, JComponent j3) {
    component.getAccessibleContext().setAccessibleName(
            combineAccessibleStrings(
                    j1.getAccessibleContext().getAccessibleName(),
                    separator1,
                    j2.getAccessibleContext().getAccessibleName(),
                    separator2,
                    j3.getAccessibleContext().getAccessibleName()));
  }

  public static void setDescription(JComponent component, JComponent source) {
    String description = source.getAccessibleContext().getAccessibleDescription();
    if (description != null) {
      component.getAccessibleContext().setAccessibleDescription(description);
    }
  }

  public static void setCombinedDescription(JComponent component, JComponent j1,
                                            String separator, JComponent j2) {
    component.getAccessibleContext().setAccessibleDescription(
            combineAccessibleStrings(
                    j1.getAccessibleContext().getAccessibleDescription(),
                    separator,
                    j2.getAccessibleContext().getAccessibleDescription()));
  }

  public static void setCombinedDescription(JComponent component, JComponent j1, String separator1,
                                            JComponent j2, String separator2, JComponent j3) {
    component.getAccessibleContext().setAccessibleDescription(
            combineAccessibleStrings(
                    j1.getAccessibleContext().getAccessibleDescription(),
                    separator1,
                    j2.getAccessibleContext().getAccessibleDescription(),
                    separator2,
                    j3.getAccessibleContext().getAccessibleDescription()));
  }

  public static void setParent(Component component, @Nullable Component newParent) {
    if (newParent instanceof Accessible) {
      component.getAccessibleContext().setAccessibleParent((Accessible)newParent);
      return;
    }
    component.getAccessibleContext().setAccessibleParent(null);
  }

  /**
   * @deprecated use {@link #setParent(Component, Component)} instead
   */
  @Deprecated
  public static void setParent(JComponent component, @Nullable Component newParent) {
    setParent((Component)component, newParent);
  }

  public static @Nullable
  String combineAccessibleStrings(@Nullable String s1, String separator, @Nullable String s2) {
    if (StringUtil.isEmpty(s1))
      return s2;
    if (StringUtil.isEmpty(s2))
      return s1;
    return String.format("%s%s%s", s1, separator, s2);
  }

  public static @Nullable
  String combineAccessibleStrings(@Nullable String s1, String separator1, @Nullable  String s2,
                                  String separator2, @Nullable  String s3) {
    return combineAccessibleStrings(combineAccessibleStrings(s1, separator1, s2), separator2, s3);
  }

  /**
   * Given a multi-line string, return an single line string where new line separators
   * are replaced with a punctuation character. This is useful for returning text to
   * screen readers, as they tend to ignore new line separators during speech, but
   * they do pause at punctuation characters.
   */
  public static 
  String replaceLineSeparatorsWithPunctuation(@Nullable String text) {
    if (StringUtil.isEmpty(text))
      return "";

    // Split by new line, removing empty lines and white-spaces at end of lines.
    String[] lines = StringUtil.splitByLines(text);

    // Join lines, ensuring each line end with a punctuation.
    StringBuilder result = new StringBuilder();
    boolean first = true;
    for (String line : lines) {
      line = line.trim();
      if (!StringUtil.isEmpty(line)) {
        if (first)
          first = false;
        else
          result.append(PUNCTUATION_SEPARATOR);
        result.append(line);
        if (!line.endsWith(PUNCTUATION_CHARACTER)) {
          result.append(PUNCTUATION_CHARACTER);
        }
      }
    }
    return result.toString();
  }
}
