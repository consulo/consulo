/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.editor.ex;

import com.intellij.codeInsight.hint.TooltipRenderer;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.TrafficTooltipRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

public interface ErrorStripTooltipRendererProvider {
  @Nullable
  TooltipRenderer calcTooltipRenderer(@Nonnull Collection<? extends RangeHighlighter> highlighters);

  @Nonnull
  TooltipRenderer calcTooltipRenderer(@Nonnull String text);

  @Nonnull
  TooltipRenderer calcTooltipRenderer(@Nonnull String text, int width);

  @Nonnull
  default TooltipRenderer calcTooltipRenderer(@Nonnull String text, @Nullable TooltipAction action, int width) {
    return calcTooltipRenderer(text, width);
  }

  @Nonnull
  TrafficTooltipRenderer createTrafficTooltipRenderer(@Nonnull Runnable onHide, @Nonnull Editor editor);
}