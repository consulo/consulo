// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding;

import consulo.ide.ServiceManager;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.fileEditor.text.CodeFoldingState;
import consulo.project.Project;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class CodeFoldingManager {
  public static CodeFoldingManager getInstance(Project project) {
    return ServiceManager.getService(project, CodeFoldingManager.class);
  }

  public abstract void updateFoldRegions(@Nonnull Editor editor);

  @Nullable
  public abstract Runnable updateFoldRegionsAsync(@Nonnull Editor editor, boolean firstTime);

  @Nullable
  public abstract FoldRegion findFoldRegion(@Nonnull Editor editor, int startOffset, int endOffset);

  public abstract FoldRegion[] getFoldRegionsAtOffset(@Nonnull Editor editor, int offset);

  public abstract CodeFoldingState saveFoldingState(@Nonnull Editor editor);

  public abstract void restoreFoldingState(@Nonnull Editor editor, @Nonnull CodeFoldingState state);

  public abstract void writeFoldingState(@Nonnull CodeFoldingState state, @Nonnull Element element);

  public abstract CodeFoldingState readFoldingState(@Nonnull Element element, @Nonnull Document document);

  public abstract void releaseFoldings(@Nonnull Editor editor);

  public abstract void buildInitialFoldings(@Nonnull Editor editor);

  @Nullable
  public abstract CodeFoldingState buildInitialFoldings(@Nonnull Document document);

  /**
   * For auto-generated regions (created by {@link com.intellij.lang.folding.FoldingBuilder}s), returns their 'collapsed by default'
   * status, for other regions returns {@code null}.
   */
  @Nullable
  public abstract Boolean isCollapsedByDefault(@Nonnull FoldRegion region);

  /**
   * Schedules recalculation of foldings in editor ({@link com.intellij.codeInsight.daemon.impl.CodeFoldingPass CodeFoldingPass}), which
   * will happen even if document (and other dependencies declared by {@link com.intellij.lang.folding.FoldingBuilder FoldingBuilder})
   * haven't changed.
   */
  public abstract void scheduleAsyncFoldingUpdate(@Nonnull Editor editor);
}
