/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.diff.impl.dir;

import consulo.application.HelpManager;
import consulo.application.ui.WindowState;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.ex.awt.FrameWrapper;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class DirDiffFrame extends FrameWrapper {
  private DirDiffPanel myPanel;

  public DirDiffFrame(Project project, DirDiffTableModel model) {
    super(project, "DirDiffDialog");
    setSize(new Dimension(800, 600));
    setTitle(model.getTitle());
    myPanel = new DirDiffPanel(model, new DirDiffWindow(this));
    Disposer.register(this, myPanel);
    setComponent(myPanel.getPanel());
    if (project != null) {
      setProject(project);
    }
    closeOnEsc();
    DataManager.registerDataProvider(myPanel.getPanel(), dataId -> HelpManager.HELP_ID == dataId ? "reference.dialogs.diff.folder" : null);
  }


  @Override
  protected void loadFrameState(@Nullable WindowState state) {
    super.loadFrameState(state);
    myPanel.setupSplitter();
  }
}
