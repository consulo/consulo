/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.impl.fileEditor;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.openapi.fileEditor.impl.DockableEditorContainerFactory;
import consulo.ide.impl.idea.openapi.fileEditor.impl.PsiAwareFileEditorManagerImpl;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.project.ui.wm.dock.DockManager;
import consulo.ui.Component;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedPsiAwareFileEditorManagerImpl extends PsiAwareFileEditorManagerImpl {
  @Inject
  public UnifiedPsiAwareFileEditorManagerImpl(Application application,
                                              ApplicationConcurrency applicationConcurrency,
                                              Project project,
                                              PsiManager psiManager,
                                              Provider<WolfTheProblemSolver> problemSolver, DockManager dockManager) {
    super(application, applicationConcurrency, project, psiManager, problemSolver, dockManager);
  }

  @Override
  protected void initUI() {
    if (mySplitters == null) {
      mySplitters = new UnifiedFileEditorsSplitters(myApplicationConcurrency, myProject, this, myDockManager, true);
    }
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    initUI();
    return mySplitters.getUIComponent();
  }

  @Nonnull
  @Override
  protected FileEditorWithProviderComposite createEditorWithProviderComposite(@Nonnull VirtualFile file,
                                                                              @Nonnull FileEditor[] editors,
                                                                              @Nonnull FileEditorProvider[] providers,
                                                                              @Nonnull FileEditorManagerEx fileEditorManager) {
    return new UnifiedFileEditorWithProviderComposite(file, editors, providers, fileEditorManager);
  }

  @Nonnull
  @Override
  protected DockableEditorContainerFactory createDockContainerFactory() {
    return new UnifiedDockableEditorContainerFactory();
  }
}
