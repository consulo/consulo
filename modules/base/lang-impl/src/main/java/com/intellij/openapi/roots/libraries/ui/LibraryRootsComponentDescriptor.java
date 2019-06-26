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
package com.intellij.openapi.roots.libraries.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.ui.impl.LibraryRootsDetectorImpl;
import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * Allows to customize a library editor
 *
 * @see com.intellij.openapi.roots.libraries.LibraryType#createLibraryRootsComponentDescriptor
 *
 * @author nik
 */
public abstract class LibraryRootsComponentDescriptor {
  /**
   * Defines presentation for root type nodes in the library roots editor
   * @return custom presentation or {@code null} if default presentation should be used
   */
  @Nullable
  public abstract OrderRootTypePresentation getRootTypePresentation(@Nonnull OrderRootType type);

  /**
   * Provides separate detectors for root types supported by the library type.
   *
   * @return non-empty list of {@link RootDetector}'s implementations
   */
  @Nonnull
  public abstract List<? extends RootDetector> getRootDetectors();

  /**
   * Provides root detector for 'Attach Files' button. It will be used to automatically assign {@link OrderRootType}s for selected files.
   * Also this detector is used when a new library is created
   *
   * @return {@link LibraryRootsDetector}'s implementation
   */
  @Nonnull
  public LibraryRootsDetector getRootsDetector() {
    return new LibraryRootsDetectorImpl(getRootDetectors());
  }


  /**
   * @return descriptor for the file chooser which will be shown when 'Attach Files' button is pressed
   * @param libraryName
   */
  @Nonnull
  public FileChooserDescriptor createAttachFilesChooserDescriptor(@Nullable String libraryName) {
    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, false, true, true);
    descriptor.setTitle(StringUtil.isEmpty(libraryName) ? ProjectBundle.message("library.attach.files.action")
                                                        : ProjectBundle.message("library.attach.files.to.library.action", libraryName));
    descriptor.setDescription(ProjectBundle.message("library.attach.files.description"));
    return descriptor;
  }

  /**
   * @return descriptors for 'Attach' buttons in the library roots editor
   */
  @Nonnull
  public abstract List<? extends AttachRootButtonDescriptor> createAttachButtons();

  /**
   * @return Array of root types supported by a library type associated with the roots
   *         component descriptor. All persistent root types are returned by default. 
   */
  @Nonnull
  public List<OrderRootType> getRootTypes() {
    return OrderRootType.getAllTypes();
  }

  public String getAttachFilesActionName() {
    return ProjectBundle.message("button.text.attach.files");
  }
}
