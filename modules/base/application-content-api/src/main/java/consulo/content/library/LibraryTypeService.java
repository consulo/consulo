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
package consulo.content.library;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.content.OrderRootType;
import consulo.content.library.ui.LibraryRootsComponentDescriptor;
import consulo.content.library.ui.LibraryRootsDetector;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class LibraryTypeService {
  public static LibraryTypeService getInstance() {
    return Application.get().getInstance(LibraryTypeService.class);
  }

  @RequiredUIAccess
  public abstract CompletableFuture<NewLibraryConfiguration> createLibraryFromFiles(LibraryRootsComponentDescriptor descriptor,
                                                                 @Nullable Component parentComponent,
                                                                 @Nullable VirtualFile contextDirectory,
                                                                 @Nullable LibraryType<?> type,
                                                                 @Nullable Project project);

  /**
   * Scans the candidates for library roots under a modal progress. The roots the detector is not sure about are
   * offered to the user to choose from, and when nothing is detected at all the user picks the kinds of the
   * candidates among the given root types.
   *
   * @param rootTypesAllowedIfNothingDetected the root types offered when nothing is detected, empty to offer none
   * @return the roots which were detected or chosen, completed exceptionally when the user cancels
   */
  @RequiredUIAccess
  public abstract CompletableFuture<List<OrderRoot>> detectRootsAsync(Collection<VirtualFile> candidates,
                                                                      @Nullable Component parentComponent,
                                                                      @Nullable Project project,
                                                                      LibraryRootsDetector detector,
                                                                      List<OrderRootType> rootTypesAllowedIfNothingDetected);
}
