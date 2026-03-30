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
package consulo.fileChooser.node;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.StubVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

public class FileNodeDescriptor extends NodeDescriptor {
  private FileElement myFileElement;
  private final @Nullable Image myOriginalIcon;
  private final @Nullable String myComment;

  public FileNodeDescriptor(
    FileElement element,
    NodeDescriptor parentDescriptor,
    @Nullable Image icon,
    @Nullable String name,
    @Nullable String comment
  ) {
    super(parentDescriptor);
    myOriginalIcon = icon;
    myComment = comment;
    myFileElement = element;
    myName = name;
  }

  public @Nullable String getName() {
    return myName;
  }

  @Override
  @RequiredUIAccess
  public boolean update() {
    boolean changed = false;

    // special handling for roots with names (e.g. web roots)
    if (myName == null || myComment == null) {
      String newName = myFileElement.toString();
      if (!newName.equals(myName)) changed = true;
      myName = newName;
    }

    VirtualFile file = myFileElement.getFile();

    if (file instanceof StubVirtualFile) {
      return true;
    }

    setIcon(myOriginalIcon);

    if (myFileElement.isHidden()) {
      Image icon = getIcon();
      if (icon != null) {
        setIcon(ImageEffects.transparent(icon));
      }
    }

    myColor = myFileElement.isHidden() ? TargetAWT.from(SimpleTextAttributes.DARK_TEXT.getFgColor()) : null;
    return changed;
  }

  @Override
  
  public final FileElement getElement() {
    return myFileElement;
  }

  protected final void setElement(FileElement descriptor) {
    myFileElement = descriptor;
  }

  public @Nullable String getComment() {
    return myComment;
  }
}
