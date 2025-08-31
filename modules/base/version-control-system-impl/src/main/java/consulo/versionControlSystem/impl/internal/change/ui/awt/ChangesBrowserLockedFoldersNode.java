/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.impl.internal.change.ChangeListOwner;
import consulo.versionControlSystem.change.ChangeProvider;
import consulo.ui.ex.awt.tree.TreeLinkMouseListener;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.SimpleTextAttributes;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangesBrowserLockedFoldersNode extends ChangesBrowserNode implements TreeLinkMouseListener.HaveTooltip {
  private final Project myProject;

  public ChangesBrowserLockedFoldersNode(Project project, Object userObject) {
    super(userObject);
    myProject = project;
  }

  public boolean canAcceptDrop(ChangeListDragBean dragBean) {
    return false;
  }

  public void acceptDrop(ChangeListOwner dragOwner, ChangeListDragBean dragBean) {
  }

  public String getTooltip() {
    return VcsBundle.message("changes.nodetitle.locked.folders.tooltip");
  }

  public void render(ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
    renderer.append(userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    renderer.append(getCountText(), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    renderer.append("   ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    CleanupStarter starter = new CleanupStarter(myProject, this);
    renderer.append("do cleanup...", new SimpleTextAttributes(SimpleTextAttributes.STYLE_UNDERLINE, Color.red), starter);
  }

  private static class CleanupStarter implements Runnable {
    private final Project myProject;
    private final ChangesBrowserLockedFoldersNode myParentNode;

    private CleanupStarter(Project project, ChangesBrowserLockedFoldersNode parentNode) {
      myProject = project;
      myParentNode = parentNode;
    }

    public void run() {
      List<VirtualFile> files = myParentNode.getAllFilesUnder();
      ProjectLevelVcsManager plVcsManager = ProjectLevelVcsManager.getInstance(myProject);
      Map<String, List<VirtualFile>> byVcs = new HashMap<String, List<VirtualFile>>();
      for (VirtualFile file : files) {
        AbstractVcs vcs = plVcsManager.getVcsFor(file);
        if (vcs != null) {
          List<VirtualFile> list = byVcs.get(vcs.getName());
          if (list == null) {
            list = new ArrayList<VirtualFile>();
            byVcs.put(vcs.getName(), list);
          }
          list.add(file);
        }
      }
      for (Map.Entry<String, List<VirtualFile>> entry : byVcs.entrySet()) {
        AbstractVcs vcs = plVcsManager.findVcsByName(entry.getKey());
        if (vcs != null) {
          ChangeProvider changeProvider = vcs.getChangeProvider();
          if (changeProvider != null) {
            changeProvider.doCleanup(entry.getValue());
          }
        }
      }
    }
  }
}
