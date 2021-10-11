/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.newProject.ui;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposer;
import consulo.start.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class NewProjectDialog extends DialogWrapper {
  private NewProjectPanel myProjectPanel;

  private Runnable myOkAction;
  private Runnable myCancelAction;

  @RequiredUIAccess
  public NewProjectDialog(@Nullable Project project, @Nullable VirtualFile moduleHome) {
    super(project, true);
    setResizable(false);

    myProjectPanel = new NewProjectPanel(getDisposable(), project, moduleHome) {
      @RequiredUIAccess
      @Nonnull
      @Override
      protected JComponent createSouthPanel() {
        return NewProjectDialog.this.createSouthPanel();
      }

      @Override
      public void setOKActionEnabled(boolean enabled) {
        NewProjectDialog.this.setOKActionEnabled(enabled);
      }

      @Override
      public void setOKActionText(@Nonnull String text) {
        NewProjectDialog.this.setOKButtonText(text);
      }

      @Override
      public void setCancelText(@Nonnull String text) {
        NewProjectDialog.this.setCancelButtonText(text);
      }

      @Override
      public void setOKAction(@Nullable Runnable action) {
        myOkAction = action;
      }

      @Override
      public void setCancelAction(@Nullable Runnable action) {
        myCancelAction = action;
      }
    };

    setTitle(moduleHome != null ? IdeBundle.message("title.add.module") : IdeBundle.message("title.new.project"));

    setOKActionEnabled(false);
    init();
  }

  public NewProjectPanel getProjectPanel() {
    return myProjectPanel;
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{getCancelAction(), getOKAction()};
  }

  @Override
  public void doCancelAction(AWTEvent source) {
    if (source instanceof WindowEvent) {
      // if it's window event - close it via X
      super.doCancelAction();
      return;
    }
    super.doCancelAction(source);
  }

  @Override
  public void doCancelAction() {
    if(myCancelAction != null) {
      myCancelAction.run();
    }
    else {
      super.doCancelAction();
    }
  }

  @Override
  protected void doOKAction() {
    if (myOkAction != null) {
      myOkAction.run();
    }
    else {
      super.doOKAction();
    }
  }

  @Override
  protected void dispose() {
    myProjectPanel.finish();
    Disposer.dispose(myProjectPanel);

    super.dispose();
  }

  @Override
  protected void initRootPanel(@Nonnull JPanel root) {
    root.add(myProjectPanel, BorderLayout.CENTER);
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    Dimension defaultWindowSize = TargetAWT.to(WelcomeFrameManager.getDefaultWindowSize());
    setSize(defaultWindowSize.width, defaultWindowSize.height);
    return "NewProjectDialog";
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myProjectPanel.getLeftComponent();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    throw new IllegalArgumentException();
  }
}
