// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiDocumentManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;

/**
 * Handler, extending IDE behaviour on typing in editor.
 * <p>
 * Note that {@code PsiFile} passed to handler's methods isn't guaranteed to be in sync with the document at the time of invocation
 * (due to performance considerations). {@link PsiDocumentManager#commitDocument(Document)} should be invoked explicitly,
 * if an up-to-date PSI is required.
 *
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TypedHandlerDelegate {
  public static final ExtensionPointName<TypedHandlerDelegate> EP_NAME = ExtensionPointName.create(TypedHandlerDelegate.class);

  /**
   * If the specified character triggers auto-popup, schedules the auto-popup appearance. This method is called even
   * in overwrite mode, when the rest of typed handler delegate methods are not called. It is invoked only for the primary caret.
   */
  @Nonnull
  public Result checkAutoPopup(char charTyped, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    return Result.CONTINUE;
  }

  /**
   * Called before selected text is deleted.
   * This method is supposed to be overridden by handlers having custom behaviour with respect to selection.
   */
  @Nonnull
  public Result beforeSelectionRemoved(char c, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    return Result.CONTINUE;
  }

  /**
   * Called before the specified character typed by the user is inserted in the editor.
   */
  @Nonnull
  public Result beforeCharTyped(char c, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull FileType fileType) {
    return Result.CONTINUE;
  }

  /**
   * Called after the specified character typed by the user has been inserted in the editor.
   */
  @Nonnull
  public Result charTyped(char c, @Nonnull Project project, @Nonnull final Editor editor, @Nonnull final PsiFile file) {
    return Result.CONTINUE;
  }

  public boolean isImmediatePaintingEnabled(@Nonnull Editor editor, char c, @Nonnull DataContext context) {
    return true;
  }

  public enum Result {
    STOP,
    CONTINUE,
    DEFAULT
  }
}
