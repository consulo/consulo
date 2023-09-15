/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.PowerSaveModeListener;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.util.UserHomeFileUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.component.messagebus.MessageBusConnection;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.TextEditorPsiDataProvider;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ui.wm.dock.DockManager;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Provider;

/**
 * @author yole
 */
public abstract class PsiAwareFileEditorManagerImpl extends FileEditorManagerImpl {
  private static final Logger LOG = Logger.getInstance(PsiAwareFileEditorManagerImpl.class);

  protected final ApplicationConcurrency myApplicationConcurrency;
  private final PsiManager myPsiManager;
  private final Provider<WolfTheProblemSolver> myProblemSolver;

  /**
   * Updates icons for open files when project roots change
   */
  private final MyPsiTreeChangeListener myPsiTreeChangeListener;
  private final ProblemListener myProblemListener;

  public PsiAwareFileEditorManagerImpl(Application application,
                                       ApplicationConcurrency applicationConcurrency,
                                       Project project,
                                       PsiManager psiManager,
                                       Provider<WolfTheProblemSolver> problemSolver,
                                       DockManager dockManager) {
    super(application, project, dockManager);
    myApplicationConcurrency = applicationConcurrency;

    myPsiManager = psiManager;
    myProblemSolver = problemSolver;
    myPsiTreeChangeListener = new MyPsiTreeChangeListener();
    myProblemListener = new MyProblemListener();
    registerExtraEditorDataProvider(new TextEditorPsiDataProvider(), null);

    // reinit syntax highlighter for Groovy. In power save mode keywords are highlighted by GroovySyntaxHighlighter insteadof
    // GrKeywordAndDeclarationHighlighter. So we need to drop caches for token types attributes in LayeredLexerEditorHighlighter
    project.getMessageBus().connect().subscribe(PowerSaveModeListener.class, new PowerSaveModeListener() {
      @Override
      public void powerSaveStateChanged() {
        project.getUIAccess().giveIfNeed(() -> {
          for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            ((EditorEx)editor).reinitSettings();
          }
        });
      }
    });
  }

  @Override
  protected void projectOpened(@Nonnull MessageBusConnection connection) {
    super.projectOpened(connection);

    myPsiManager.addPsiTreeChangeListener(myPsiTreeChangeListener);
    connection.subscribe(ProblemListener.class, myProblemListener);
  }

  @Override
  public boolean isProblem(@Nonnull final VirtualFile file) {
    return myProblemSolver.get().isProblemFile(file);
  }

  @Nonnull
  @Override
  public String getFileTooltipText(@Nonnull final VirtualFile file) {
    final StringBuilder tooltipText = new StringBuilder();
    final Module module = ModuleUtilCore.findModuleForFile(file, getProject());
    if (module != null) {
      tooltipText.append("[");
      tooltipText.append(module.getName());
      tooltipText.append("] ");
    }
    tooltipText.append(UserHomeFileUtil.getLocationRelativeToUserHome(file.getPresentableUrl()));
    return tooltipText.toString();
  }

  @Override
  protected Editor getOpenedEditor(@Nonnull final Editor editor, final boolean focusEditor) {
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getProject());
    Document document = editor.getDocument();
    PsiFile psiFile = documentManager.getPsiFile(document);
    if (!focusEditor || documentManager.isUncommited(document)) {
      return editor;
    }

    return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, psiFile);
  }

  /**
   * Updates attribute of open files when roots change
   */
  private final class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {
    @Override
    public void propertyChanged(@Nonnull final PsiTreeChangeEvent e) {
      if (PsiTreeChangeEvent.PROP_ROOTS.equals(e.getPropertyName())) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        final VirtualFile[] openFiles = getOpenFiles();
        for (int i = openFiles.length - 1; i >= 0; i--) {
          final VirtualFile file = openFiles[i];
          LOG.assertTrue(file != null);
          updateFileIconAsync(file);
        }
      }
    }

    @Override
    public void childAdded(@Nonnull PsiTreeChangeEvent event) {
      doChange(event);
    }

    @Override
    public void childRemoved(@Nonnull PsiTreeChangeEvent event) {
      doChange(event);
    }

    @Override
    public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
      doChange(event);
    }

    @Override
    public void childMoved(@Nonnull PsiTreeChangeEvent event) {
      doChange(event);
    }

    @Override
    public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
      doChange(event);
    }

    private void doChange(final PsiTreeChangeEvent event) {
      if(UIAccess.isUIThread()) {
        doChangeInUI(event);
      }
      else {
        Application.get().getLastUIAccess().giveAndWait(() -> doChangeInUI(event));
      }
    }

    private void doChangeInUI(final PsiTreeChangeEvent event) {
      final PsiFile psiFile = event.getFile();
      if (psiFile == null) return;
      VirtualFile file = psiFile.getVirtualFile();
      if (file == null) return;
      FileEditor[] editors = getAllEditors(file);
      if (editors.length == 0) return;

      final VirtualFile currentFile = getCurrentFile();
      if (currentFile != null && Comparing.equal(psiFile.getVirtualFile(), currentFile)) {
        updateFileIconAsync(currentFile);
      }
    }
  }

  private class MyProblemListener implements ProblemListener {
    @Override
    public void problemsAppeared(@Nonnull final VirtualFile file) {
      updateFile(file);
    }

    @Override
    public void problemsDisappeared(@Nonnull VirtualFile file) {
      updateFile(file);
    }

    @Override
    public void problemsChanged(@Nonnull VirtualFile file) {
      updateFile(file);
    }

    private void updateFile(@Nonnull VirtualFile file) {
      queueUpdateFile(file);
    }
  }
}
