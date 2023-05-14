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
package consulo.ide.impl.idea.openapi.roots.impl.libraries;

import consulo.annotation.component.ServiceImpl;
import consulo.content.library.*;
import consulo.content.library.ui.LibraryEditor;
import consulo.content.library.ui.LibraryRootsComponentDescriptor;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.ide.impl.idea.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class LibraryTypeServiceImpl extends LibraryTypeService {
  private static final String DEFAULT_LIBRARY_NAME = "Unnamed";

  @Override
  public NewLibraryConfiguration createLibraryFromFiles(@Nonnull LibraryRootsComponentDescriptor descriptor,
                                                        @Nonnull JComponent parentComponent,
                                                        @Nullable VirtualFile contextDirectory,
                                                        LibraryType<?> type,
                                                        final Project project) {
    final FileChooserDescriptor chooserDescriptor = descriptor.createAttachFilesChooserDescriptor(null);
    chooserDescriptor.setTitle("Select Library Files");
    final VirtualFile[] rootCandidates = IdeaFileChooser.chooseFiles(chooserDescriptor, parentComponent, project, contextDirectory);
    if (rootCandidates.length == 0) {
      return null;
    }

    final List<OrderRoot> roots = RootDetectionUtil.detectRoots(Arrays.asList(rootCandidates), parentComponent, project, descriptor);
    if (roots.isEmpty()) return null;
    String name = suggestLibraryName(roots);
    return doCreate(type, name, roots);
  }

  private static <P extends LibraryProperties<?>> NewLibraryConfiguration doCreate(final LibraryType<P> type, final String name, final List<OrderRoot> roots) {
    return new NewLibraryConfiguration(name, type, type != null ? type.getKind().createDefaultProperties() : null) {
      @Override
      public void addRoots(@Nonnull LibraryEditor editor) {
        editor.addRoots(roots);
      }
    };
  }

  public static String suggestLibraryName(@Nonnull VirtualFile[] classesRoots) {
    if (classesRoots.length >= 1) {
      return FileUtil.getNameWithoutExtension(PathUtil.getFileName(classesRoots[0].getPath()));
    }
    return DEFAULT_LIBRARY_NAME;
  }

  public static String suggestLibraryName(@Nonnull List<OrderRoot> roots) {
    if (roots.size() >= 1) {
      return FileUtil.getNameWithoutExtension(PathUtil.getFileName(roots.get(0).getFile().getPath()));
    }
    return DEFAULT_LIBRARY_NAME;
  }
}
