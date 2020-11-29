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
package com.intellij.openapi.fileChooser.ex;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import consulo.awt.TargetAWT;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;

public class FileNodeDescriptor extends NodeDescriptor {

  private FileElement myFileElement;
  private final Image myOriginalIcon;
  private final String myComment;

  public FileNodeDescriptor(Project project, @Nonnull FileElement element, NodeDescriptor parentDescriptor, Image icon, String name, String comment) {
    super(project, parentDescriptor);
    myOriginalIcon = icon;
    myComment = comment;
    myFileElement = element;
    myName = name;
  }

  public String getName() {
    return myName;
  }

  @Override
  @RequiredUIAccess
  public boolean update() {
    boolean changed = false;

    // special handling for roots with names (e.g. web roots)
    if (myName == null || myComment == null) {
      final String newName = myFileElement.toString();
      if (!newName.equals(myName)) changed = true;
      myName = newName;
    }

    VirtualFile file = myFileElement.getFile();

    if (file == null) return true;

    setIcon(myOriginalIcon);

    if (myFileElement.isHidden()) {
      setIcon(ImageEffects.transparent(getIcon()));
    }

    myColor = myFileElement.isHidden() ? TargetAWT.from(SimpleTextAttributes.DARK_TEXT.getFgColor()) : null;
    return changed;
  }

  @Override
  @Nonnull
  public final FileElement getElement() {
    return myFileElement;
  }

  protected final void setElement(FileElement descriptor) {
    myFileElement = descriptor;
  }

  public String getComment() {
    return myComment;
  }
}
