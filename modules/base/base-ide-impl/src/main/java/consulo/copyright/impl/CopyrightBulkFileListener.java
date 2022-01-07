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
package consulo.copyright.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.maddyhome.idea.copyright.CopyrightManager;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.CopyrightUpdaters;
import com.maddyhome.idea.copyright.actions.UpdateCopyrightProcessor;
import consulo.annotation.access.RequiredReadAction;
import consulo.util.lang.ThreeState;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 05/03/2021
 */
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

          if (!CopyrightUpdaters.hasExtension(virtualFile)) {
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
    }, ModalityState.NON_MODAL, project.getDisposed());
  }
}
