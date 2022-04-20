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
package consulo.desktop.awt.fileEditor.impl;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.DesktopFileEditorWithProviderComposite;
import com.intellij.openapi.fileEditor.impl.DesktopFileEditorsSplitters;
import com.intellij.openapi.fileEditor.impl.PsiAwareFileEditorManagerImpl;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.project.ui.wm.dock.DockManager;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.ex.awt.JBUI;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
@Singleton
public class DesktopPsiAwareFileEditorManagerImpl extends PsiAwareFileEditorManagerImpl {
  private volatile JPanel myPanels;
  private final Object myInitLock = new Object();

  @Inject
  public DesktopPsiAwareFileEditorManagerImpl(Project project, PsiManager psiManager, Provider<WolfTheProblemSolver> problemSolver, DockManager dockManager) {
    super(project, psiManager, problemSolver, dockManager);
  }

  @Nonnull
  @Override
  protected FileEditorWithProviderComposite createEditorWithProviderComposite(@Nonnull VirtualFile file,
                                                                              @Nonnull FileEditor[] editors,
                                                                              @Nonnull FileEditorProvider[] providers,
                                                                              @Nonnull FileEditorManagerEx fileEditorManager) {
    return new DesktopFileEditorWithProviderComposite(file, editors, providers, fileEditorManager);
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    initUI();
    return myPanels;
  }

  @Override
  protected void initUI() {
    if (myPanels == null) {
      synchronized (myInitLock) {
        if (myPanels == null) {
          final JPanel panel = new JPanel(new BorderLayout());
          panel.setOpaque(false);
          panel.setBorder(JBUI.Borders.empty());
          DesktopFileEditorsSplitters splitters = new DesktopFileEditorsSplitters(myProject, this, myDockManager, true);
          mySplitters = splitters;
          Disposer.register(myProject, splitters);
          panel.add(splitters.getComponent(), BorderLayout.CENTER);
          myPanels = panel;
        }
      }
    }
  }
}
