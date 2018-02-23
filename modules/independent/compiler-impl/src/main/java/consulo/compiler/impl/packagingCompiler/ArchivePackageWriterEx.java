/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.impl.packagingCompiler;

import com.intellij.compiler.impl.packagingCompiler.ArchivePackageInfo;
import consulo.packaging.elements.ArchivePackageWriter;
import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 13.04.2016
 */
public abstract class ArchivePackageWriterEx<T> implements ArchivePackageWriter<T> {
  @Nonnull
  @Override
  public T createArchiveObject(@Nonnull File tempFile) throws IOException {
    throw new UnsupportedOperationException("Use createArchiveObject(@NotNull File tempFile, @NotNull ArchivePackageInfo archivePackageInfo)");
  }

  @Nonnull
  public abstract T createArchiveObject(@Nonnull File tempFile, @Nonnull ArchivePackageInfo archivePackageInfo) throws IOException;
}
