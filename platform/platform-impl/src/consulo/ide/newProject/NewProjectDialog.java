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
package consulo.ide.newProject;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 04.06.14
 */
public class NewProjectDialog extends DialogWrapper {
  private final boolean myModuleCreation;

  private NewProjectPanel myProjectPanel;

  public NewProjectDialog(@Nullable Project project, @Nullable VirtualFile virtualFile) {
    super(project, true);
    setResizable(false);

    myProjectPanel = new NewProjectPanel(getDisposable(), project, virtualFile) {
      @Override
      protected JComponent createSouthPanel() {
        return NewProjectDialog.this.createSouthPanel();
      }

      @Override
      public void setOKActionEnabled(boolean enabled) {
        NewProjectDialog.this.setOKActionEnabled(enabled);
      }
    };

    myModuleCreation = virtualFile != null;

    setTitle(myModuleCreation ? IdeBundle.message("title.add.module") : IdeBundle.message("title.new.project"));

    setOKActionEnabled(false);
    init();
  }

  public NewProjectPanel getProjectPanel() {
    return myProjectPanel;
  }

  @Override
  protected void initRootPanel(@NotNull JPanel root) {
    root.add(myProjectPanel, BorderLayout.CENTER);
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    Dimension defaultWindowSize = FlatWelcomeFrame.getDefaultWindowSize();
    setSize(defaultWindowSize.width, defaultWindowSize.height);
    return "NewProjectDialog";
  }

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

  public boolean isModuleCreation() {
    return myModuleCreation;
  }
}
