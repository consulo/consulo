/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.application.AllIcons;
import consulo.application.util.UserHomeFileUtil;
import consulo.util.lang.StringUtil;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;

import jakarta.annotation.Nonnull;

import static consulo.ui.ex.awt.FontUtil.spaceAndThinSpace;

/**
 * @author yole
 */
public class ChangesBrowserFileNode extends ChangesBrowserNode<VirtualFile> implements Comparable<ChangesBrowserFileNode> {
  private final Project myProject;
  private final String myName;

  public ChangesBrowserFileNode(Project project, @Nonnull VirtualFile userObject) {
    super(userObject);
    myName = StringUtil.toLowerCase(userObject.getName());
    myProject = project;
  }

  @Override
  protected boolean isFile() {
    return !getUserObject().isDirectory();
  }

  @Override
  protected boolean isDirectory() {
    return getUserObject().isDirectory() &&
           (isLeaf() || FileStatusManager.getInstance(myProject).getStatus(getUserObject()) != FileStatus.NOT_CHANGED);
  }

  @Override
  public void render(final ChangesBrowserNodeRenderer renderer, final boolean selected, final boolean expanded, final boolean hasFocus) {
    final VirtualFile file = getUserObject();
    renderer.appendFileName(file, file.getName(), TargetAWT.to(ChangeListManager.getInstance(myProject).getStatus(file).getColor()));
    if (renderer.isShowFlatten() && file.isValid()) {
      final VirtualFile parentFile = file.getParent();
      assert parentFile != null;
      renderer.append(spaceAndThinSpace() + UserHomeFileUtil.getLocationRelativeToUserHome(parentFile.getPresentableUrl()), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    else if (getFileCount() != 1 || getDirectoryCount() != 0) {
      appendCount(renderer);
    }
    if (file.isDirectory()) {
      renderer.setIcon(AllIcons.Nodes.TreeClosed);
    }
    else {
      renderer.setIcon(file.getFileType().getIcon());
    }
  }

  @Override
  public String getTextPresentation() {
    return getUserObject().getName();
  }

  @Override
  public String toString() {
    return getUserObject().getPresentableUrl();
  }

  public int getSortWeight() {
    return VIRTUAL_FILE_SORT_WEIGHT;
  }

  @Override
  public int compareTo(ChangesBrowserFileNode o) {
    return myName.compareTo(o.myName);
  }

  public int compareUserObjects(final Object o2) {
    if (o2 instanceof VirtualFile) {
      return getUserObject().getName().compareToIgnoreCase(((VirtualFile)o2).getName());
    }
    return 0;
  }
}
