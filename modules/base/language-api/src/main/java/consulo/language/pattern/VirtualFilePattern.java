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
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class VirtualFilePattern extends TreeElementPattern<VirtualFile, VirtualFile, VirtualFilePattern> {
  public VirtualFilePattern() {
    super(VirtualFile.class);
  }

  public VirtualFilePattern ofType(final FileType type) {
    return with(new PatternCondition<VirtualFile>("ofType") {
      @Override
      public boolean accepts(@Nonnull VirtualFile virtualFile, ProcessingContext context) {
        return type.equals(virtualFile.getFileType());
      }
    });
  }

  public VirtualFilePattern withName(String name) {
    return withName(PlatformPatterns.string().equalTo(name));
  }

  public VirtualFilePattern withExtension(@Nonnull final String... alternatives) {
    return with(new PatternCondition<VirtualFile>("withExtension") {
      @Override
      public boolean accepts(@Nonnull VirtualFile virtualFile, ProcessingContext context) {
        String extension = virtualFile.getExtension();
        for (String alternative : alternatives) {
          if (alternative.equals(extension)) {
            return true;
          }
        }
        return false;
      }
    });
  }

  public VirtualFilePattern withExtension(@Nonnull final String extension) {
    return with(new PatternCondition<VirtualFile>("withExtension") {
      @Override
      public boolean accepts(@Nonnull VirtualFile virtualFile, ProcessingContext context) {
        return extension.equals(virtualFile.getExtension());
      }
    });
  }

  public VirtualFilePattern withName(final ElementPattern<String> namePattern) {
    return with(new PatternCondition<VirtualFile>("withName") {
      @Override
      public boolean accepts(@Nonnull VirtualFile virtualFile, ProcessingContext context) {
        return namePattern.getCondition().accepts(virtualFile.getName(), context);
      }
    });
  }

  public VirtualFilePattern withPath(final ElementPattern<String> pathPattern) {
    return with(new PatternCondition<VirtualFile>("withName") {
      @Override
      public boolean accepts(@Nonnull VirtualFile virtualFile, ProcessingContext context) {
        return pathPattern.accepts(virtualFile.getPath(), context);
      }
    });
  }

  @Override
  protected VirtualFile getParent(@Nonnull VirtualFile t) {
    return t.getParent();
  }

  @Override
  protected VirtualFile[] getChildren(@Nonnull VirtualFile file) {
    return file.getChildren();
  }
}
