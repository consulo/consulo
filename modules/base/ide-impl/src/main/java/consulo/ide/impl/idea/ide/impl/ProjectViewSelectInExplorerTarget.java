/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.ide.SelectInContext;
import consulo.ide.impl.idea.ide.SelectInTarget;
import consulo.ide.impl.idea.ide.StandardTargetWeights;
import consulo.ide.impl.idea.ide.actions.RevealFileAction;
import consulo.ide.impl.idea.ide.actions.ShowFilePathAction;
import consulo.application.dumb.DumbAware;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;

/**
 * @author Roman.Chernyatchik
 */
@ExtensionImpl
public class ProjectViewSelectInExplorerTarget implements SelectInTarget, DumbAware {
  @Override
  public boolean canSelect(SelectInContext context) {
    VirtualFile file = ShowFilePathAction.findLocalFile(context.getVirtualFile());
    return file != null;
  }

  @Override
  public void selectIn(SelectInContext context, boolean requestFocus) {
    VirtualFile file = ShowFilePathAction.findLocalFile(context.getVirtualFile());
    if (file != null) {
      ShowFilePathAction.openFile(new File(file.getPresentableUrl()));
    }
  }

  @Override
  public String getToolWindowId() {
    return null;
  }

  @Override
  public String getMinorViewId() {
    return null;
  }

  @Override
  public String toString() {
    return RevealFileAction.getActionName();
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.OS_FILE_MANAGER;
  }
}
