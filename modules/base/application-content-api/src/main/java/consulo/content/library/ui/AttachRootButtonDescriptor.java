/*
 * Copyright 2000-2010 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.content.library.ui;

import consulo.content.OrderRootType;
import consulo.dataContext.DataContext;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * Describes an 'attach' button in the library roots component.
 *
 * @author nik
 * @see ChooserBasedAttachRootButtonDescriptor
 */
public abstract class AttachRootButtonDescriptor {
  private final OrderRootType myOrderRootType;
  protected final String myButtonText;
  private final Image myToolbarIcon;

  /**
   * Creates a descriptor for 'attach' button shown in popup when user click on '+' button.
   * Consider using {@link #AttachRootButtonDescriptor(OrderRootType, Image, String)} instead.
   */
  protected AttachRootButtonDescriptor(@Nonnull OrderRootType orderRootType, @Nonnull String buttonText) {
    myOrderRootType = orderRootType;
    myButtonText = buttonText;
    myToolbarIcon = null;
  }

  /**
   * Creates a descriptor for 'attach' button shown in toolbar of a library editor
   */
  protected AttachRootButtonDescriptor(@Nonnull OrderRootType orderRootType, @Nonnull Image toolbarIcon, @Nonnull String description) {
    myOrderRootType = orderRootType;
    myButtonText = description;
    myToolbarIcon = toolbarIcon;
  }

  public abstract VirtualFile[] selectFiles(@Nonnull JComponent parent, @Nullable VirtualFile initialSelection, @Nonnull DataContext dataContext, @Nonnull LibraryEditor libraryEditor);

  public String getButtonText() {
    return myButtonText;
  }

  public OrderRootType getRootType() {
    return myOrderRootType;
  }

  public boolean addAsJarDirectories() {
    return false;
  }

  @Nonnull
  public VirtualFile[] scanForActualRoots(@Nonnull VirtualFile[] rootCandidates, JComponent parent) {
    return rootCandidates;
  }

  @Nullable
  public Image getToolbarIcon() {
    return myToolbarIcon;
  }
}
