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
import consulo.content.library.LibraryType;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.localize.ProjectLocalize;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Allows to customize a library editor
 *
 * @see LibraryType#createLibraryRootsComponentDescriptor
 *
 * @author nik
 */
public abstract class LibraryRootsComponentDescriptor {
  /**
   * Defines presentation for root type nodes in the library roots editor
   *
   * @return custom presentation or {@code null} if default presentation should be used
   */
  @Nullable
  public OrderRootTypePresentation getRootTypePresentation(@Nonnull OrderRootType type) {
    // will use default impl
    return null;
  }

  /**
   * Provides separate detectors for root types supported by the library type.
   *
   * @return non-empty list of {@link RootDetector}'s implementations
   */
  @Nonnull
  public abstract List<? extends RootDetector> getRootDetectors();

  /**
   * Provides root detector for 'Attach Files' button. It will be used
   * to automatically assign {@link OrderRootType}s for selected files.
   * Also this detector is used when a new library is created.
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
    return new FileChooserDescriptor(true, true, true, false, true, true)
      .withTitleValue(
        StringUtil.isEmpty(libraryName)
          ? ProjectLocalize.libraryAttachFilesAction()
          : ProjectLocalize.libraryAttachFilesToLibraryAction(libraryName)
      )
      .withDescriptionValue(ProjectLocalize.libraryAttachFilesDescription());
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
    return ProjectLocalize.buttonTextAttachFiles().get();
  }
}
