/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.psi.include;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionPointName;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.stub.FileContent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
@Extension(ComponentScope.APPLICATION)
public abstract class FileIncludeProvider {
  public static final ExtensionPointName<FileIncludeProvider> EP_NAME = ExtensionPointName.create(FileIncludeProvider.class);

  @Nonnull
  public abstract String getId();

  public abstract boolean acceptFile(VirtualFile file);

  public abstract void registerFileTypesUsedForIndexing(@Nonnull Consumer<FileType> fileTypeSink);

  @Nonnull
  public abstract FileIncludeInfo[] getIncludeInfos(FileContent content);

  /**
   * If all providers return {@code null} then {@code FileIncludeInfo} is resolved in a standard way using {@code FileReferenceSet}
   */
  @Nullable
  public PsiFileSystemItem resolveIncludedFile(@Nonnull final FileIncludeInfo info, @Nonnull final PsiFile context) {
    return null;
  }

  /**
   * Override this method and increment returned value each time when you change the logic of your provider.
   */
  public int getVersion() {
    return 0;
  }

  /**
   * @return Possible name in included paths. For example if a provider returns FileIncludeInfos without file extensions
   */
  @Nonnull
  public String getIncludeName(@Nonnull PsiFile file, @Nonnull String originalName) {
    return originalName;
  }
}
