// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.folding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.document.Document;
import consulo.fileEditor.text.CodeFoldingState;
import consulo.project.Project;
import org.jdom.Element;

import org.jspecify.annotations.Nullable;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class CodeFoldingManager {
  public static CodeFoldingManager getInstance(Project project) {
    return project.getInstance(CodeFoldingManager.class);
  }

  public abstract void updateFoldRegions(Editor editor);

  public abstract @Nullable Runnable updateFoldRegionsAsync(Editor editor, boolean firstTime);

  public abstract @Nullable FoldRegion findFoldRegion(Editor editor, int startOffset, int endOffset);

  public abstract FoldRegion[] getFoldRegionsAtOffset(Editor editor, int offset);

  public abstract CodeFoldingState saveFoldingState(Editor editor);

  public abstract void restoreFoldingState(Editor editor, CodeFoldingState state);

  public abstract void writeFoldingState(CodeFoldingState state, Element element);

  public abstract CodeFoldingState readFoldingState(Element element, Document document);

  public abstract void releaseFoldings(Editor editor);

  public abstract void buildInitialFoldings(Editor editor);

  public abstract @Nullable CodeFoldingState buildInitialFoldings(Document document);

  /**
   * For auto-generated regions (created by {@link FoldingBuilder}s), returns their 'collapsed by default'
   * status, for other regions returns {@code null}.
   */
  public abstract @Nullable Boolean isCollapsedByDefault(FoldRegion region);

  /**
   * Schedules recalculation of foldings in editor ({@link CodeFoldingPass}), which
   * will happen even if document (and other dependencies declared by {@link FoldingBuilder FoldingBuilder})
   * haven't changed.
   */
  public abstract void scheduleAsyncFoldingUpdate(Editor editor);
}
