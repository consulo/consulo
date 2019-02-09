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

package com.intellij.codeInsight.folding;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.fileEditor.impl.text.CodeFoldingState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.WriteExternalException;
import consulo.ui.RequiredUIAccess;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class CodeFoldingManager {
  public static CodeFoldingManager getInstance(Project project){
    return project.getComponent(CodeFoldingManager.class);
  }

  public abstract void updateFoldRegions(@Nonnull Editor editor);

  public abstract void forceDefaultState(@Nonnull Editor editor);

  @Nullable
  public abstract Runnable updateFoldRegionsAsync(@Nonnull Editor editor, boolean firstTime);

  @Nullable
  public abstract FoldRegion findFoldRegion(@Nonnull Editor editor, int startOffset, int endOffset);
  public abstract FoldRegion[] getFoldRegionsAtOffset(@Nonnull Editor editor, int offset);

  @RequiredUIAccess
  public abstract CodeFoldingState saveFoldingState(@Nonnull Editor editor);
  @RequiredUIAccess
  public abstract void restoreFoldingState(@Nonnull Editor editor, @Nonnull CodeFoldingState state);

  public abstract void writeFoldingState(@Nonnull CodeFoldingState state, @Nonnull Element element) throws WriteExternalException;
  public abstract CodeFoldingState readFoldingState(@Nonnull Element element, @Nonnull Document document);

  @RequiredUIAccess
  public abstract void releaseFoldings(@Nonnull Editor editor);
  @RequiredUIAccess
  public abstract void buildInitialFoldings(@Nonnull Editor editor);
  @Nullable
  public abstract CodeFoldingState buildInitialFoldings(@Nonnull Document document);
}
