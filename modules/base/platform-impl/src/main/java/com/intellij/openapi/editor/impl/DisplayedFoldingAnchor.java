/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.FoldRegion;
import javax.annotation.Nonnull;

class DisplayedFoldingAnchor {
  enum Type {
    COLLAPSED(false),
    COLLAPSED_SINGLE_LINE(true),
    EXPANDED_TOP(false),
    EXPANDED_BOTTOM(false),
    EXPANDED_SINGLE_LINE(true);

    public final boolean singleLine;

    Type(boolean singleLine) {
      this.singleLine = singleLine;
    }
  }

  @Nonnull
  final FoldRegion foldRegion;
  final int visualLine;
  final int foldRegionVisualLines;
  @Nonnull
  final Type type;

  DisplayedFoldingAnchor(@Nonnull FoldRegion foldRegion, int visualLine, int foldRegionVisualLines, @Nonnull Type type) {
    this.foldRegion = foldRegion;
    this.visualLine = visualLine;
    this.foldRegionVisualLines = foldRegionVisualLines;
    this.type = type;
  }
}
