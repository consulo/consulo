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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.AllIcons;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.impl.internal.change.ui.awt.TreeModelBuilder;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.versionControlSystem.change.Change;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.io.File;

/**
 * @author irengrig
 *         Date: 7/8/11
 *         Time: 12:21 PM
 */
public class VirtualFileListCellRenderer extends ColoredListCellRenderer {
  private final FileStatusManager myFileStatusManager;
  private final boolean myIgnoreFileStatus;

  public VirtualFileListCellRenderer(final Project project) {
    this(project, false);
  }

  public VirtualFileListCellRenderer(final Project project, final boolean ignoreFileStatus) {
    myIgnoreFileStatus = ignoreFileStatus;
    myFileStatusManager = FileStatusManager.getInstance(project);
  }

  @Override
  protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
    final FilePath path = TreeModelBuilder.getPathForObject(value);
    renderIcon(path);
    final FileStatus fileStatus = myIgnoreFileStatus ? FileStatus.NOT_CHANGED : getStatus(value, path);
    append(getName(path), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, TargetAWT.to(fileStatus.getColor()), null));
    putParentPath(value, path, path);
  }

  protected String getName(FilePath path) {
    return path.getName();
  }

  protected FileStatus getStatus(Object value, FilePath path) {
    final FileStatus fileStatus;
    if (value instanceof Change) {
      fileStatus = ((Change) value).getFileStatus();
    }
    else {
      final VirtualFile virtualFile = path.getVirtualFile();
      if (virtualFile != null) {
        fileStatus = myFileStatusManager.getStatus(virtualFile);
      }
      else {
        fileStatus = FileStatus.NOT_CHANGED;
      }
    }
    return fileStatus;
  }

  protected void renderIcon(FilePath path) {
    if (path.isDirectory()) {
      setIcon(AllIcons.Nodes.TreeClosed);
    } else {
      setIcon(path.getFileType().getIcon());
    }
  }

  protected void putParentPath(Object value, FilePath path, FilePath self) {
    final File parentFile = path.getIOFile().getParentFile();
    if (parentFile != null) {
      final String parentPath = parentFile.getPath();
      append(" (", SimpleTextAttributes.GRAYED_ATTRIBUTES);
      putParentPathImpl(value, parentPath, self);
      append(")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
  }

  protected void putParentPathImpl(Object value, String parentPath, FilePath self) {
    append(parentPath, SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }
}
