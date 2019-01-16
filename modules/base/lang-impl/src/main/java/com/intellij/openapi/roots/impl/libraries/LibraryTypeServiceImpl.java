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
package com.intellij.openapi.roots.impl.libraries;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryProperties;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.LibraryTypeService;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
@Singleton
public class LibraryTypeServiceImpl extends LibraryTypeService {
  private static final String DEFAULT_LIBRARY_NAME = "Unnamed";

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<NewLibraryConfiguration> createLibraryFromFiles(@Nonnull LibraryRootsComponentDescriptor descriptor,
                                                                     @Nonnull JComponent parentComponent,
                                                                     @Nullable VirtualFile contextDirectory,
                                                                     LibraryType<?> type,
                                                                     final Project project) {
    final FileChooserDescriptor chooserDescriptor = descriptor.createAttachFilesChooserDescriptor(null);
    chooserDescriptor.setTitle("Select Library Files");

    AsyncResult<NewLibraryConfiguration> result = AsyncResult.undefined();
    AsyncResult<VirtualFile[]> filesAsync = FileChooser.chooseFilesAsync(chooserDescriptor, parentComponent, project, contextDirectory);
    filesAsync.doWhenDone((rootCandidates) -> {
      final List<OrderRoot> roots = RootDetectionUtil.detectRoots(Arrays.asList(rootCandidates), parentComponent, project, descriptor);
      if (roots.isEmpty()) {
        result.setRejected();
        return;
      }

      String name = suggestLibraryName(roots);
      result.setDone(doCreate(type, name, roots));
    });
    filesAsync.doWhenRejected((Runnable)result::setRejected);
    return result;
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
