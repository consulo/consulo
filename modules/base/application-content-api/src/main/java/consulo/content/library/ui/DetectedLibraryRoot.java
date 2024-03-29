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
package consulo.content.library.ui;

import consulo.content.OrderRootType;
import consulo.content.library.LibraryRootType;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class DetectedLibraryRoot {
  private final VirtualFile myFile;
  private final List<LibraryRootType> myTypes;

  public DetectedLibraryRoot(@Nonnull VirtualFile file, @Nonnull OrderRootType rootType, boolean jarDirectory) {
    this(file, Collections.singletonList(new LibraryRootType(rootType, jarDirectory)));
  }

  public DetectedLibraryRoot(@Nonnull VirtualFile file, @Nonnull List<LibraryRootType> types) {
    myFile = file;
    myTypes = types;
  }

  @Nonnull
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  public List<LibraryRootType> getTypes() {
    return myTypes;
  }
}
