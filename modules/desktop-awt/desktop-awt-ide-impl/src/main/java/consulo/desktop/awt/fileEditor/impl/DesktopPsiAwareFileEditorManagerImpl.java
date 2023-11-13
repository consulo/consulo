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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposer;
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
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
@Singleton
@ServiceImpl
public class DesktopPsiAwareFileEditorManagerImpl extends PsiAwareFileEditorManagerImpl {
  private volatile JPanel myPanels;
  private final Object myInitLock = new Object();

  @Inject
  public DesktopPsiAwareFileEditorManagerImpl(Application application,
                                              ApplicationConcurrency applicationConcurrency,
                                              Project project,
                                              PsiManager psiManager,
                                              Provider<WolfTheProblemSolver> problemSolver, DockManager dockManager) {
    super(application, applicationConcurrency, project, psiManager, problemSolver, dockManager);
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
  protected DockableEditorContainerFactory createDockContainerFactory() {
    return new DesktopAWTDockableEditorContainerFactory(myApplicationConcurrency, myProject, this, myDockManager);
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
          DesktopFileEditorsSplitters splitters =
            new DesktopFileEditorsSplitters(myProject, myApplicationConcurrency, this, myDockManager, true);
          mySplitters = splitters;
          Disposer.register(myProject, splitters);
          panel.add(splitters.getComponent(), BorderLayout.CENTER);
          myPanels = panel;
        }
      }
    }
  }

  @Override
  protected boolean isOpenInNewWindow() {
    AWTEvent event = IdeEventQueue.getInstance().getTrueCurrentEvent();

    // Shift was used while clicking
    if (event instanceof MouseEvent &&
      ((MouseEvent)event).isShiftDown() &&
      (event.getID() == MouseEvent.MOUSE_CLICKED || event.getID() == MouseEvent.MOUSE_PRESSED || event.getID() == MouseEvent.MOUSE_RELEASED)) {
      return true;
    }

    if (event instanceof KeyEvent) {
      KeyEvent ke = (KeyEvent)event;
      Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
      String[] ids = keymap.getActionIds(KeyStroke.getKeyStroke(ke.getKeyCode(), ke.getModifiers()));
      return Arrays.asList(ids).contains("OpenElementInNewWindow");
    }

    return false;
  }

  @Override
  public void dispose() {
  }
}
