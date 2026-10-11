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
import consulo.content.OrderRootType;
import consulo.content.library.*;
import consulo.content.library.ui.LibraryEditor;
import consulo.content.library.ui.LibraryRootsComponentDescriptor;
import consulo.content.library.ui.LibraryRootsDetector;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooser;
import consulo.ide.impl.idea.openapi.roots.libraries.ui.impl.RootDetectionUtil;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class LibraryTypeServiceImpl extends LibraryTypeService {
  private static final String DEFAULT_LIBRARY_NAME = "Unnamed";

  @Override
  @RequiredUIAccess
  public CompletableFuture<NewLibraryConfiguration> createLibraryFromFiles(LibraryRootsComponentDescriptor descriptor,
                                                                          @Nullable Component parentComponent,
                                                                          @Nullable VirtualFile contextDirectory,
                                                                          @Nullable LibraryType<?> type,
                                                                          @Nullable Project project) {
    FileChooserDescriptor chooserDescriptor = descriptor.createAttachFilesChooserDescriptor(null);
    chooserDescriptor.withTitle(ProjectLocalize.libraryChooseFilesTitle());
    UIAccess uiAccess = UIAccess.current();
    return FileChooser.chooseFiles(chooserDescriptor, project, contextDirectory)
      .thenComposeAsync(
        rootCandidates -> RootDetectionUtil.detectRootsAsync(Arrays.asList(rootCandidates), parentComponent, project, descriptor),
        uiAccess
      )
      .thenApply(roots -> {
        if (roots.isEmpty()) {
          throw new CancellationException();
        }
        return doCreate(type, suggestLibraryName(roots), roots);
      });
  }

  @Override
  @RequiredUIAccess
  public CompletableFuture<List<OrderRoot>> detectRootsAsync(Collection<VirtualFile> candidates,
                                                             @Nullable Component parentComponent,
                                                             @Nullable Project project,
                                                             LibraryRootsDetector detector,
                                                             List<OrderRootType> rootTypesAllowedIfNothingDetected) {
    return RootDetectionUtil.detectRootsAsync(candidates, parentComponent, project, detector, rootTypesAllowedIfNothingDetected);
  }

  private static <P extends LibraryProperties<?>> NewLibraryConfiguration doCreate(final LibraryType<P> type, final String name, final List<OrderRoot> roots) {
    return new NewLibraryConfiguration(name, type, type != null ? type.getKind().createDefaultProperties() : null) {
      @Override
      public void addRoots(LibraryEditor editor) {
        editor.addRoots(roots);
      }
    };
  }

  public static String suggestLibraryName(VirtualFile[] classesRoots) {
    if (classesRoots.length >= 1) {
      return FileUtil.getNameWithoutExtension(PathUtil.getFileName(classesRoots[0].getPath()));
    }
    return DEFAULT_LIBRARY_NAME;
  }

  public static String suggestLibraryName(List<OrderRoot> roots) {
    if (roots.size() >= 1) {
      return FileUtil.getNameWithoutExtension(PathUtil.getFileName(roots.get(0).getFile().getPath()));
    }
    return DEFAULT_LIBRARY_NAME;
  }
}
