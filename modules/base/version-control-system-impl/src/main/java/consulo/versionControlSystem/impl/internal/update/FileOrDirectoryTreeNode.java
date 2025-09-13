/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.update;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lesya
 */
public abstract class FileOrDirectoryTreeNode extends AbstractTreeNode implements VirtualFilePointerListener, Disposable {
  private static final Map<FileStatus, SimpleTextAttributes> myFileStatusToAttributeMap = new HashMap<FileStatus, SimpleTextAttributes>();
  private final SimpleTextAttributes myInvalidAttributes;
  protected final Project myProject;
  protected final File myFile;
  private final String myName;

  protected FileOrDirectoryTreeNode(@Nonnull String path,
                                    @Nonnull SimpleTextAttributes invalidAttributes,
                                    @Nonnull Project project,
                                    @Nullable String parentPath) {
    String preparedPath = path.replace(File.separatorChar, '/');
    String url = VirtualFileManager.constructUrl(LocalFileSystem.getInstance().getProtocol(), preparedPath);
    setUserObject(VirtualFilePointerManager.getInstance().create(url, this, this));
    myFile = new File(getFilePath());
    myInvalidAttributes = invalidAttributes;
    myProject = project;
    myName = parentPath == null ? myFile.getAbsolutePath() : myFile.getName();
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  protected String getFilePath() {
    return getFilePointer().getPresentableUrl();
  }

  @Override
  public void beforeValidityChanged(@Nonnull VirtualFilePointer[] pointers) {
  }

  @Override
  public void validityChanged(@Nonnull VirtualFilePointer[] pointers) {
    if (!getFilePointer().isValid()) {
      AbstractTreeNode parent = (AbstractTreeNode)getParent();
      if (parent != null && parent.getSupportsDeletion()) {
        getTreeModel().removeNodeFromParent(this);
      }
      else {
        if (getTree() != null) {
          getTree().repaint();
        }
      }
    }
  }

  @Override
  public void setUserObject(Object userObject) {
    Object oldObject = getUserObject();
    try {
      super.setUserObject(userObject);
    }
    finally {
      if (oldObject instanceof VirtualFilePointer) {
        VirtualFilePointer pointer = (VirtualFilePointer)oldObject;
        Disposer.dispose((Disposable)pointer);
      }
    }
  }

  public VirtualFilePointer getFilePointer() {
    return (VirtualFilePointer)getUserObject();
  }

  @Nonnull
  @Override
  public SimpleTextAttributes getAttributes() {
    if (!getFilePointer().isValid()) {
      return myInvalidAttributes;
    }
    VirtualFile file = getFilePointer().getFile();
    FileStatusManager fileStatusManager = FileStatusManager.getInstance(myProject);
    FileStatus status = fileStatusManager.getStatus(file);
    SimpleTextAttributes attributes = getAttributesFor(status);
    return myFilterAttributes == null ? attributes : SimpleTextAttributes.merge(myFilterAttributes, attributes);
  }

  @Nonnull
  private static SimpleTextAttributes getAttributesFor(@Nonnull FileStatus status) {
    Color color = TargetAWT.to(status.getColor());
    if (color == null) color = UIUtil.getListForeground();

    if (!myFileStatusToAttributeMap.containsKey(status)) {
      myFileStatusToAttributeMap.put(status, new SimpleTextAttributes(Font.PLAIN, color));
    }
    return myFileStatusToAttributeMap.get(status);
  }

  @Override
  public boolean getSupportsDeletion() {
    AbstractTreeNode parent = (AbstractTreeNode)getParent();
    return parent != null && parent.getSupportsDeletion();
  }

  @Override
  public void dispose() {
  }
}
