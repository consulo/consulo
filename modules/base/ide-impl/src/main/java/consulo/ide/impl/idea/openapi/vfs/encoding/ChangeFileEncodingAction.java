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
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.ide.impl.idea.openapi.command.undo.GlobalUndoableAction;
import consulo.undoRedo.UndoManager;
import consulo.undoRedo.UndoableAction;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.Editor;
import consulo.undoRedo.ApplicationUndoManager;
import consulo.undoRedo.ProjectUndoManager;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.CommonDataKeys;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author cdr
 */
public class ChangeFileEncodingAction extends AnAction implements DumbAware {
  private final boolean allowDirectories;

  public ChangeFileEncodingAction() {
    this(false);
  }

  public ChangeFileEncodingAction(boolean allowDirectories) {
    this.allowDirectories = allowDirectories;
  }

  private boolean checkEnabled(@Nonnull VirtualFile virtualFile) {
    if (allowDirectories && virtualFile.isDirectory()) return true;
    FileDocumentManager documentManager = FileDocumentManager.getInstance();
    Document document = documentManager.getDocument(virtualFile);
    if (document == null) return false;

    return EncodingUtil.checkCanConvert(virtualFile) == null || EncodingUtil.checkCanReload(virtualFile, null) == null;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    VirtualFile myFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    boolean enabled = myFile != null && checkEnabled(myFile);
    e.getPresentation().setEnabled(enabled);
    e.getPresentation().setVisible(myFile != null);
  }

  @Override
  public final void actionPerformed(@Nonnull final AnActionEvent e) {
    DataContext dataContext = e.getDataContext();

    ListPopup popup = createPopup(dataContext);
    if (popup != null) {
      popup.showInBestPositionFor(dataContext);
    }
  }

  @Nullable
  public ListPopup createPopup(@Nonnull DataContext dataContext) {
    final VirtualFile virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE);
    if (virtualFile == null) return null;
    boolean enabled = checkEnabled(virtualFile);
    if (!enabled) return null;
    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    FileDocumentManager documentManager = FileDocumentManager.getInstance();
    final Document document = documentManager.getDocument(virtualFile);
    if (!allowDirectories && virtualFile.isDirectory() || document == null && !virtualFile.isDirectory()) return null;

    final byte[] bytes;
    try {
      bytes = virtualFile.isDirectory() ? null : VfsUtilCore.loadBytes(virtualFile);
    }
    catch (IOException e) {
      return null;
    }
    DefaultActionGroup group = createActionGroup(virtualFile, editor, document, bytes, null);

    return JBPopupFactory.getInstance().createActionGroupPopup(getTemplatePresentation().getText(), group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
  }

  public DefaultActionGroup createActionGroup(@Nullable final VirtualFile myFile, final Editor editor, final Document document, final byte[] bytes, @Nullable final String clearItemText) {
    return new ChooseFileEncodingAction(myFile) {
      @Override
      public void update(@Nonnull final AnActionEvent e) {
      }

      @Nonnull
      @Override
      protected DefaultActionGroup createPopupActionGroup(JComponent button) {
        return createCharsetsActionGroup(clearItemText, null, charset -> "Change encoding to '" + charset.displayName() + "'");
        // no 'clear'
      }

      @Override
      protected void chosen(@Nullable VirtualFile virtualFile, @Nonnull Charset charset) {
        ChangeFileEncodingAction.this.chosen(document, editor, virtualFile, bytes, charset);
      }
    }.createPopupActionGroup(null);
  }

  // returns true if charset was changed, false if failed
  protected boolean chosen(final Document document, final Editor editor, @Nullable final VirtualFile virtualFile, byte[] bytes, @Nonnull final Charset charset) {
    if (virtualFile == null) return false;
    String text = document.getText();
    EncodingUtil.Magic8 isSafeToConvert = EncodingUtil.isSafeToConvertTo(virtualFile, text, bytes, charset);
    EncodingUtil.Magic8 isSafeToReload = EncodingUtil.isSafeToReloadIn(virtualFile, text, bytes, charset);

    final Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
    return changeTo(project, document, editor, virtualFile, charset, isSafeToConvert, isSafeToReload);
  }

  public static boolean changeTo(Project project, @Nonnull Document document,
                                 Editor editor,
                                 @Nonnull VirtualFile virtualFile,
                                 @Nonnull Charset charset,
                                 @Nonnull EncodingUtil.Magic8 isSafeToConvert, @Nonnull EncodingUtil.Magic8 isSafeToReload) {
    final Charset oldCharset = virtualFile.getCharset();
    final Runnable undo;
    final Runnable redo;

    if (isSafeToConvert == EncodingUtil.Magic8.ABSOLUTELY && isSafeToReload == EncodingUtil.Magic8.ABSOLUTELY) {
      //change and forget
      undo = () -> EncodingManager.getInstance().setEncoding(virtualFile, oldCharset);
      redo = () -> EncodingManager.getInstance().setEncoding(virtualFile, charset);
    }
    else {
      IncompatibleEncodingDialog dialog = new IncompatibleEncodingDialog(virtualFile, charset, isSafeToReload, isSafeToConvert);
      dialog.show();
      if (dialog.getExitCode() == IncompatibleEncodingDialog.RELOAD_EXIT_CODE) {
        undo = () -> EncodingUtil.reloadIn(virtualFile, oldCharset, project);
        redo = () -> EncodingUtil.reloadIn(virtualFile, charset, project);
      }
      else if (dialog.getExitCode() == IncompatibleEncodingDialog.CONVERT_EXIT_CODE) {
        undo = () -> EncodingUtil.saveIn(document, editor, virtualFile, oldCharset);
        redo = () -> EncodingUtil.saveIn(document, editor, virtualFile, charset);
      }
      else {
        return false;
      }
    }

    final UndoableAction action = new GlobalUndoableAction(virtualFile) {
      @Override
      public void undo() {
        // invoke later because changing document inside undo/redo is not allowed
        Application application = ApplicationManager.getApplication();
        application.invokeLater(undo, IdeaModalityState.NON_MODAL, (project == null ? application : project).getDisposed());
      }

      @Override
      public void redo() {
        // invoke later because changing document inside undo/redo is not allowed
        Application application = ApplicationManager.getApplication();
        application.invokeLater(redo, IdeaModalityState.NON_MODAL, (project == null ? application : project).getDisposed());
      }
    };

    redo.run();
    CommandProcessor.getInstance().executeCommand(project, () -> {
      UndoManager undoManager = project == null ? ApplicationUndoManager.getGlobalInstance() : ProjectUndoManager.getInstance(project);
      undoManager.undoableActionPerformed(action);
    }, "Change encoding for '" + virtualFile.getName() + "'", null, UndoConfirmationPolicy.REQUEST_CONFIRMATION);

    return true;
  }
}
