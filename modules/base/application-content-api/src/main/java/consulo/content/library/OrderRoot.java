/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.content.OrderRootType;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class OrderRoot {
  private final VirtualFile myFile;
  private final OrderRootType myType;
  private final boolean myJarDirectory;

  public OrderRoot(@Nonnull VirtualFile file, @Nonnull OrderRootType type) {
    this(file, type, false);
  }

  public OrderRoot(@Nonnull VirtualFile file, @Nonnull OrderRootType type, boolean jarDirectory) {
    myFile = file;
    myType = type;
    myJarDirectory = jarDirectory;
  }

  @Nonnull
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  public OrderRootType getType() {
    return myType;
  }

  public boolean isJarDirectory() {
    return myJarDirectory;
  }
}
