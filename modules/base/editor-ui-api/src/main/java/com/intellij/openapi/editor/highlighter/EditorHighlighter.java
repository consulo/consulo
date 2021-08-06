/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.openapi.editor.highlighter;

import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface EditorHighlighter extends DocumentListener {
  @Nonnull
  HighlighterIterator createIterator(int startOffset);

  void setText(@Nonnull CharSequence text);

  default void setText(@Nonnull CharSequence text, @Nullable String debugInfo) {
    setText(text);
  }

  void setEditor(@Nonnull HighlighterClient editor);

  void setColorScheme(@Nonnull EditorColorsScheme scheme);
}
