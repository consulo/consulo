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
package com.intellij.diff.tools.util.base;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Condition;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum HighlightingLevel {
  INSPECTIONS("Inspections", AllIcons.Ide.HectorOn, rangeHighlighter -> true),
  ADVANCED("Syntax", AllIcons.Ide.HectorSyntax, rangeHighlighter -> rangeHighlighter.getLayer() <= HighlighterLayer.ADDITIONAL_SYNTAX),
  SIMPLE("None", AllIcons.Ide.HectorOff, rangeHighlighter -> rangeHighlighter.getLayer() <= HighlighterLayer.SYNTAX);

  @Nonnull
  private final String myText;
  @Nullable
  private final Image myIcon;
  @Nonnull
  private final Condition<RangeHighlighter> myCondition;

  HighlightingLevel(@Nonnull String text, @Nullable Image icon, @Nonnull Condition<RangeHighlighter> condition) {
    myText = text;
    myIcon = icon;
    myCondition = condition;
  }

  @Nonnull
  public String getText() {
    return myText;
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  @Nonnull
  public Condition<RangeHighlighter> getCondition() {
    return myCondition;
  }
}