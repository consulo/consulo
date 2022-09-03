/*
 * Copyright 2013-2022 consulo.io
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
package consulo.compiler.artifact.internal;

import consulo.compiler.artifact.element.ArchivePackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.DirectoryPackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 03-Sep-22
 */
public class ArtifactInternalUtil {

  @Nullable
  private static PackagingElement<?> findArchiveOrDirectoryByName(@Nonnull CompositePackagingElement<?> parent, @Nonnull String name) {
    for (PackagingElement<?> element : parent.getChildren()) {
      if (element instanceof ArchivePackagingElement && ((ArchivePackagingElement)element).getArchiveFileName().equals(name) ||
          element instanceof DirectoryPackagingElement && ((DirectoryPackagingElement)element).getDirectoryName().equals(name)) {
        return element;
      }
    }
    return null;
  }

  @Nonnull
  public static String suggestFileName(@Nonnull CompositePackagingElement<?> parent, @NonNls @Nonnull String prefix, @NonNls @Nonnull String suffix) {
    String name = prefix + suffix;
    int i = 2;
    while (findArchiveOrDirectoryByName(parent, name) != null) {
      name = prefix + i++ + suffix;
    }
    return name;
  }
}
