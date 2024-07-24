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
import consulo.application.util.SystemInfo;
import consulo.application.util.UserHomeFileUtil;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.ui.ex.awt.FontUtil.spaceAndThinSpace;

/**
 * @author yole
 */
public class ChangesBrowserFilePathNode extends ChangesBrowserNode<FilePath> {
  public ChangesBrowserFilePathNode(FilePath userObject) {
    super(userObject);
  }

  @Override
  protected boolean isFile() {
    return !getUserObject().isDirectory();
  }

  @Override
  protected boolean isDirectory() {
    return getUserObject().isDirectory() && isLeaf();
  }

  @Override
  public void render(
    @Nonnull final ChangesBrowserNodeRenderer renderer,
    final boolean selected,
    final boolean expanded,
    final boolean hasFocus
  ) {
    final FilePath path = (FilePath)userObject;
    if (path.isDirectory() || !isLeaf()) {
      renderer.append(getRelativePath(path), SimpleTextAttributes.REGULAR_ATTRIBUTES);
      if (!isLeaf()) {
        appendCount(renderer);
      }
      renderer.setIcon(AllIcons.Nodes.TreeClosed);
    }
    else {
      if (renderer.isShowFlatten()) {
        renderer.append(path.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        FilePath parentPath = path.getParentPath();
        renderer.append(
          spaceAndThinSpace() + UserHomeFileUtil.getLocationRelativeToUserHome(parentPath.getPresentableUrl()),
          SimpleTextAttributes.GRAYED_ATTRIBUTES
        );
      }
      else {
        renderer.append(getRelativePath(path), SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
      renderer.setIcon(path.getFileType().getIcon());
    }
  }

  @Nonnull
  protected String getRelativePath(FilePath path) {
    return getRelativePath(safeCastToFilePath(((ChangesBrowserNode)getParent()).getUserObject()), path);
  }

  @Override
  public String getTextPresentation() {
    return getRelativePath(getUserObject());
  }

  @Override
  public String toString() {
    return FileUtil.toSystemDependentName(getUserObject().getPath());
  }

  @Nullable
  public static FilePath safeCastToFilePath(Object o) {
    return o instanceof FilePath filePath ? filePath : o instanceof Change change ? ChangesUtil.getAfterPath(change) : null;
  }

  @Nonnull
  public static String getRelativePath(@Nullable FilePath parent, @Nonnull FilePath child) {
    boolean isLocal = !child.isNonLocal();
    boolean caseSensitive = isLocal && SystemInfo.isFileSystemCaseSensitive;
    String result = parent != null ? FileUtil.getRelativePath(parent.getPath(), child.getPath(), '/', caseSensitive) : null;

    result = result == null ? child.getPath() : result;

    return isLocal ? FileUtil.toSystemDependentName(result) : result;
  }

  @Override
  public int getSortWeight() {
    if (((FilePath)userObject).isDirectory()) return DIRECTORY_PATH_SORT_WEIGHT;
    return FILE_PATH_SORT_WEIGHT;
  }

  @Override
  public int compareUserObjects(final Object o2) {
    return o2 instanceof FilePath filePath ? getUserObject().getPath().compareToIgnoreCase(filePath.getPath()) : 0;
  }
}
