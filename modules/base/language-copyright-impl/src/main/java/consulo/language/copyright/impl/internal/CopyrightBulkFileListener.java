/*
 * Copyright 2013-2021 consulo.io
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
package consulo.language.copyright.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.copyright.UpdateCopyrightsProvider;
import consulo.language.copyright.config.CopyrightManager;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.copyright.impl.internal.action.UpdateCopyrightProcessor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ModalityState;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 05/03/2021
 */
@TopicImpl(ComponentScope.APPLICATION)
public class CopyrightBulkFileListener implements BulkFileListener {
  private final Set<String> myNewFilePaths = ConcurrentHashMap.newKeySet();

  private AtomicBoolean myDocumentListenerRegistrator = new AtomicBoolean();

  @Nonnull
  private final Application myApplication;
  @Nonnull
  private Provider<EditorFactory> myEditorFactoryProvider;
  @Nonnull
  private Provider<FileDocumentManager> myFileDocumentManagerProvider;
  @Nonnull
  private Provider<ProjectManager> myProjectManagerProvider;

  @Inject
  public CopyrightBulkFileListener(@Nonnull Application application,
                                   @Nonnull Provider<EditorFactory> editorFactoryProvider,
                                   @Nonnull Provider<FileDocumentManager> fileDocumentManagerProvider,
                                   @Nonnull Provider<ProjectManager> projectManagerProvider) {
    myApplication = application;
    myEditorFactoryProvider = editorFactoryProvider;
    myFileDocumentManagerProvider = fileDocumentManagerProvider;
    myProjectManagerProvider = projectManagerProvider;
  }

  @Override
  public void after(@Nonnull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      if (event.isFromRefresh()) {
        continue;
      }

      if (event instanceof VFileCreateEvent) {
        myNewFilePaths.add(event.getPath());
      }
    }

    if (!myNewFilePaths.isEmpty()) {
      registerDocumentListener();
    }
  }

  private void registerDocumentListener() {
    if (myDocumentListenerRegistrator.compareAndSet(false, true)) {
      EditorFactory editorFactory = myEditorFactoryProvider.get();

      editorFactory.getEventMulticaster().addDocumentListener(new DocumentListener() {
        @Override
        public void documentChanged(DocumentEvent e) {
          final Document document = e.getDocument();
          final VirtualFile virtualFile = myFileDocumentManagerProvider.get().getFile(document);
          if (virtualFile == null) {
            return;
          }

          if (!myNewFilePaths.remove(virtualFile.getPath())) {
            return;
          }

          if (!UpdateCopyrightsProvider.hasExtension(virtualFile)) {
            return;
          }

          Project[] openProjects = myProjectManagerProvider.get().getOpenProjects();
          for (Project openProject : openProjects) {
            if (openProject.getDisposeState().get() != ThreeState.NO) {
              continue;
            }

            runEventOnEachProject(openProject, virtualFile);
          }
        }
      }, myApplication);
    }
  }

  @RequiredReadAction
  private void runEventOnEachProject(Project project, VirtualFile virtualFile) {
    final Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
    if (module == null) {
      return;
    }
    final PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
    if (file == null) {
      return;
    }

    myApplication.invokeLater(() -> {
      if (project.isDisposed()) return;
      if (file.isValid() && file.isWritable()) {
        CopyrightManager copyrightManager = CopyrightManager.getInstance(project);

        final CopyrightProfile opts = copyrightManager.getCopyrightOptions(file);
        if (opts != null) {
          new UpdateCopyrightProcessor(project, module, file).run();
        }
      }
    }, ModalityState.nonModal(), project.getDisposed());
  }
}
