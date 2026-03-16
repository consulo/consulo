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
package consulo.language.editor.impl.internal.markup;

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.language.editor.impl.internal.hint.TooltipAction;
import consulo.language.editor.impl.internal.hint.TooltipRenderer;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

/**
 * @author max
 */
public interface ErrorStripTooltipRendererProvider {
  @Nullable
  TooltipRenderer calcTooltipRenderer(Collection<? extends RangeHighlighter> highlighters);

  
  TooltipRenderer calcTooltipRenderer(String text);

  
  TooltipRenderer calcTooltipRenderer(String text, int width);

  
  default TooltipRenderer calcTooltipRenderer(String text, @Nullable TooltipAction action, int width) {
    return calcTooltipRenderer(text, width);
  }

  
  TrafficTooltipRenderer createTrafficTooltipRenderer(Runnable onHide, Editor editor);
}