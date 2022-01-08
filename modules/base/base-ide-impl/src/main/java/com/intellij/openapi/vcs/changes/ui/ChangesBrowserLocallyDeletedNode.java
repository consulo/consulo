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
package com.intellij.openapi.vcs.changes.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.LocallyDeletedChange;
import com.intellij.openapi.vcs.changes.issueLinks.TreeLinkMouseListener;
import com.intellij.ui.SimpleTextAttributes;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.intellij.util.FontUtil.spaceAndThinSpace;

public class ChangesBrowserLocallyDeletedNode extends ChangesBrowserNode<LocallyDeletedChange>
        implements TreeLinkMouseListener.HaveTooltip {
  public ChangesBrowserLocallyDeletedNode(@Nonnull LocallyDeletedChange userObject) {
    super(userObject);
  }

  @Override
  protected boolean isFile() {
    return !isDirectory();
  }

  @Override
  protected boolean isDirectory() {
    return getUserObject().getPath().isDirectory();
  }

  @Override
  public void render(@Nonnull ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
    // todo would be good to have render code in one place
    FilePath filePath = getUserObject().getPath();
    renderer.appendFileName(filePath.getVirtualFile(), filePath.getName(), TargetAWT.to(FileStatus.NOT_CHANGED.getColor()));

    if (renderer.isShowFlatten()) {
      FilePath parentPath = filePath.getParentPath();
      if (parentPath != null) {
        renderer.append(spaceAndThinSpace() + parentPath.getPresentableUrl(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
    }
    else if (getFileCount() != 1 || getDirectoryCount() != 0) {
      appendCount(renderer);
    }

    renderer.setIcon(getIcon());
  }

  @Override
  @Nullable
  public String getTooltip() {
    return getUserObject().getDescription();
  }

  @Nullable
  private Image getIcon() {
    Image result = getUserObject().getAddIcon();

    if (result == null) {
      FilePath filePath = getUserObject().getPath();
      result = filePath.isDirectory() || !isLeaf() ? AllIcons.Nodes.TreeClosed : filePath.getFileType().getIcon();
    }

    return result;
  }
}
