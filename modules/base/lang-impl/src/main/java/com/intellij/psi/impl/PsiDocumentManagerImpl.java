/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.psi.impl;

import com.intellij.AppTopics;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.DocumentWindowImpl;
import com.intellij.injected.editor.EditorWindowImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.components.SettingsSavingComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.DocumentBulkUpdateListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.core.impl.PomModelImpl;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.impl.source.tree.injected.MultiHostRegistrarImpl;
import com.intellij.util.FileContentUtil;
import com.intellij.util.Processor;
import com.intellij.util.messages.MessageBusConnection;
import consulo.application.AccessRule;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;

//todo listen & notifyListeners readonly events?
@Singleton
public class PsiDocumentManagerImpl extends PsiDocumentManagerBase implements SettingsSavingComponent {
  private final DocumentCommitProcessor myDocumentCommitThread;
  private final boolean myUnitTestMode = ApplicationManager.getApplication().isUnitTestMode();

  @Inject
  public PsiDocumentManagerImpl(@Nonnull final Project project,
                                @Nonnull PsiManager psiManager,
                                @Nonnull EditorFactory editorFactory,
                                @NonNls @Nonnull final DocumentCommitProcessor documentCommitThread) {
    super(project, psiManager, documentCommitThread);
    myDocumentCommitThread = documentCommitThread;
    editorFactory.getEventMulticaster().addDocumentListener(this, project);
    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerAdapter() {
      @Override
      public void fileContentLoaded(@Nonnull final VirtualFile virtualFile, @Nonnull Document document) {
        ThrowableComputable<PsiFile, RuntimeException> action = () -> myProject.isDisposed() || !virtualFile.isValid() ? null : getCachedPsiFile(virtualFile);
        PsiFile psiFile = AccessRule.read(action);
        fireDocumentCreated(document, psiFile);
      }
    });
    connection.subscribe(DocumentBulkUpdateListener.TOPIC, new DocumentBulkUpdateListener.Adapter() {
      @Override
      public void updateFinished(@Nonnull Document doc) {
        documentCommitThread.commitAsynchronously(project, doc, "Bulk update finished", TransactionGuard.getInstance().getContextTransaction());
      }
    });
    Disposer.register(project, () -> ((DocumentCommitThread)myDocumentCommitThread).cancelTasksOnProjectDispose(project));
  }

  @Nullable
  @Override
  public PsiFile getPsiFile(@Nonnull Document document) {
    final PsiFile psiFile = super.getPsiFile(document);
    if (myUnitTestMode) {
      final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
      if (virtualFile != null && virtualFile.isValid()) {
        Collection<Project> projects = ProjectLocator.getInstance().getProjectsForFile(virtualFile);
        if (!projects.isEmpty() && !projects.contains(myProject)) {
          LOG.error("Trying to get PSI for an alien project. VirtualFile=" + virtualFile + ";\n myProject=" + myProject + ";\n projects returned: " + projects);
        }
      }
    }
    return psiFile;
  }

  @Override
  public void documentChanged(DocumentEvent event) {
    super.documentChanged(event);
    // optimisation: avoid documents piling up during batch processing
    if (FileDocumentManagerImpl.areTooManyDocumentsInTheQueue(myUncommittedDocuments)) {
      if (myUnitTestMode) {
        myStopTrackingDocuments = true;
        try {
          LOG.error("Too many uncommitted documents for " +
                    myProject +
                    "(" +
                    myUncommittedDocuments.size() +
                    ")" +
                    ":\n" +
                    StringUtil.join(myUncommittedDocuments, "\n") +
                    "\n\n Project creation trace: " +
                    myProject.getUserData(ProjectImpl.CREATION_TRACE));
        }
        finally {
          //noinspection TestOnlyProblems
          clearUncommittedDocuments();
        }
      }
      // must not commit during document save
      if (PomModelImpl.isAllowPsiModification()) {
        commitAllDocuments();
      }
    }
  }

  @Override
  protected void beforeDocumentChangeOnUnlockedDocument(@Nonnull final FileViewProvider viewProvider) {
    PostprocessReformattingAspect.getInstance(myProject).beforeDocumentChanged(viewProvider);
    super.beforeDocumentChangeOnUnlockedDocument(viewProvider);
  }

  @Override
  protected boolean finishCommitInWriteAction(@Nonnull Document document, @Nonnull List<Processor<Document>> finishProcessors, boolean synchronously) {
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) { // can be false for non-physical PSI
      EditorWindowImpl.disposeInvalidEditors();
    }
    return super.finishCommitInWriteAction(document, finishProcessors, synchronously);
  }

  @Override
  public boolean isDocumentBlockedByPsi(@Nonnull Document doc) {
    final FileViewProvider viewProvider = getCachedViewProvider(doc);
    return viewProvider != null && PostprocessReformattingAspect.getInstance(myProject).isViewProviderLocked(viewProvider);
  }

  @Override
  public void doPostponedOperationsAndUnblockDocument(@Nonnull Document doc) {
    if (doc instanceof DocumentWindow) doc = ((DocumentWindow)doc).getDelegate();
    final PostprocessReformattingAspect component = myProject.getComponent(PostprocessReformattingAspect.class);
    final FileViewProvider viewProvider = getCachedViewProvider(doc);
    if (viewProvider != null && component != null) component.doPostponedFormatting(viewProvider);
  }

  @Override
  public void save() {
    // Ensure all documents are committed on save so file content dependent indices, that use PSI to build have consistent content.
    try {
      commitAllDocuments();
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  @Override
  @TestOnly
  public void clearUncommittedDocuments() {
    super.clearUncommittedDocuments();
    ((DocumentCommitThread)myDocumentCommitThread).clearQueue();
  }

  @NonNls
  @Override
  public String toString() {
    return super.toString() + " for the project " + myProject + ".";
  }

  @Override
  public void reparseFiles(@Nonnull Collection<VirtualFile> files, boolean includeOpenFiles) {
    FileContentUtil.reparseFiles(myProject, files, includeOpenFiles);
  }

  @Nonnull
  @Override
  protected DocumentWindow freezeWindow(@Nonnull DocumentWindow document) {
    return MultiHostRegistrarImpl.freezeWindow((DocumentWindowImpl)document);
  }
}
