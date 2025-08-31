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

package consulo.language.pattern;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
public class PsiFilePattern<T extends PsiFile, Self extends PsiFilePattern<T, Self>> extends PsiElementPattern<T, Self> {

  protected PsiFilePattern(@Nonnull InitialPatternCondition<T> condition) {
    super(condition);
  }

  protected PsiFilePattern(Class<T> aClass) {
    super(aClass);
  }

  public Self withParentDirectoryName(final StringPattern namePattern) {
    return with(new PatternCondition<T>("withParentDirectoryName") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        PsiDirectory directory = t.getContainingDirectory();
        return directory != null && namePattern.getCondition().accepts(directory.getName(), context);
      }
    });
  }

  public Self withOriginalFile(final ElementPattern<? extends T> filePattern) {
    return with(new PatternCondition<T>("withOriginalFile") {
      @Override
      public boolean accepts(@Nonnull T file, ProcessingContext context) {
        return filePattern.accepts(file.getOriginalFile());
      }
    });
  }

  public Self withVirtualFile(final ElementPattern<? extends VirtualFile> vFilePattern) {
    return with(new PatternCondition<T>("withVirtualFile") {
      @Override
      public boolean accepts(@Nonnull T file, ProcessingContext context) {
        return vFilePattern.accepts(file.getVirtualFile(), context);
      }
    });
  }

  public Self withFileType(final ElementPattern<? extends FileType> fileTypePattern) {
    return with(new PatternCondition<T>("withFileType") {
      @Override
      public boolean accepts(@Nonnull T file, ProcessingContext context) {
        return fileTypePattern.accepts(file.getFileType(), context);
      }
    });
  }

  public static class Capture<T extends PsiFile> extends PsiFilePattern<T,Capture<T>> {

    protected Capture(Class<T> aClass) {
      super(aClass);
    }

    public Capture(@Nonnull InitialPatternCondition<T> condition) {
      super(condition);
    }
  }
}
