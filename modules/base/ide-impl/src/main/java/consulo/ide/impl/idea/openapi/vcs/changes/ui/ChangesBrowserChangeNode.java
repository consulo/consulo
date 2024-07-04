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

package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.AllIcons;
import consulo.application.util.UserHomeFileUtil;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.TreeLinkMouseListener;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.ui.ex.awt.FontUtil.spaceAndThinSpace;

public class ChangesBrowserChangeNode extends ChangesBrowserNode<Change> implements TreeLinkMouseListener.HaveTooltip {

  @Nonnull
  private final Project myProject;
  @Nullable
  private final ChangeNodeDecorator myDecorator;

  protected ChangesBrowserChangeNode(@Nonnull Project project, @Nonnull Change userObject, @Nullable ChangeNodeDecorator decorator) {
    super(userObject);
    myProject = project;
    myDecorator = decorator;
  }

  @Override
  protected boolean isFile() {
    return !isDirectory();
  }

  @Override
  protected boolean isDirectory() {
    return ChangesUtil.getFilePath(getUserObject()).isDirectory();
  }

  @Override
  public void render(@Nonnull ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
    Change change = getUserObject();
    FilePath filePath = ChangesUtil.getFilePath(change);
    VirtualFile file = filePath.getVirtualFile();

    if (myDecorator != null) {
      myDecorator.preDecorate(change, renderer, renderer.isShowFlatten());
    }

    renderer.appendFileName(file, filePath.getName(), TargetAWT.to(change.getFileStatus().getColor()));

    String originText = change.getOriginText(myProject);
    if (originText != null) {
      renderer.append(spaceAndThinSpace() + originText, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    if (renderer.isShowFlatten()) {
      FilePath parentPath = filePath.getParentPath();
      if (parentPath != null) {
        renderer.append(spaceAndThinSpace() + UserHomeFileUtil.getLocationRelativeToUserHome(parentPath.getPath()),
                        SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
      appendSwitched(renderer, file);
    }
    else if (getFileCount() != 1 || getDirectoryCount() != 0) {
      appendSwitched(renderer, file);
      appendCount(renderer);
    }
    else {
      appendSwitched(renderer, file);
    }

    renderer.setIcon(getIcon(change, filePath));

    if (myDecorator != null) {
      myDecorator.decorate(change, renderer, renderer.isShowFlatten());
    }
  }

  @Nullable
  private Image getIcon(@Nonnull Change change, @Nonnull FilePath filePath) {
    Image result = change.getAdditionalIcon();

    if (result == null) {
      result = filePath.isDirectory() || !isLeaf() ? AllIcons.Nodes.TreeClosed : filePath.getFileType().getIcon();
    }

    return result;
  }

  private void appendSwitched(@Nonnull ChangesBrowserNodeRenderer renderer, @Nullable VirtualFile file) {
    if (file != null && !myProject.isDefault()) {
      String branch = ChangeListManager.getInstance(myProject).getSwitchedBranch(file);
      if (branch != null) {
        renderer.append(spaceAndThinSpace() + "[switched to " + branch + "]", SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
    }
  }

  @Override
  public String getTooltip() {
    return getUserObject().getDescription();
  }

  @Override
  public String getTextPresentation() {
    return ChangesUtil.getFilePath(getUserObject()).getName();
  }

  @Override
  public String toString() {
    return FileUtil.toSystemDependentName(ChangesUtil.getFilePath(getUserObject()).getPath());
  }

  @Override
  public int getSortWeight() {
    return CHANGE_SORT_WEIGHT;
  }

  @Override
  public int compareUserObjects(final Object o2) {
    return o2 instanceof Change change
      ? ChangesUtil.getFilePath(getUserObject()).getName().compareToIgnoreCase(ChangesUtil.getFilePath(change).getName()) : 0;
  }
}
