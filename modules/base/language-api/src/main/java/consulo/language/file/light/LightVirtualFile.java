/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.file.light;

import consulo.language.Language;
import consulo.language.file.CharsetUtil;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.light.TextLightVirtualFileBase;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;

/**
 * In-memory implementation of {@link VirtualFile}.
 */
public class LightVirtualFile extends TextLightVirtualFileBase {
  private Language myLanguage;

  public LightVirtualFile() {
    this("");
  }

  public LightVirtualFile(@NonNls @Nonnull String name) {
    this(name, "");
  }

  public LightVirtualFile(@NonNls @Nonnull String name, @Nonnull CharSequence content) {
    this(name, null, content, LocalTimeCounter.currentTime());
  }

  public LightVirtualFile(@Nonnull String name, final FileType fileType, @Nonnull CharSequence text) {
    this(name, fileType, text, LocalTimeCounter.currentTime());
  }

  public LightVirtualFile(VirtualFile original, @Nonnull CharSequence text, long modificationStamp) {
    this(original.getName(), original.getFileType(), text, modificationStamp);
    setCharset(original.getCharset());
  }

  public LightVirtualFile(@Nonnull String name, final FileType fileType, @Nonnull CharSequence text, final long modificationStamp) {
    this(name, fileType, text, CharsetUtil.extractCharsetFromFileContent(null, null, fileType, text), modificationStamp);
  }

  public LightVirtualFile(@Nonnull String name, final FileType fileType, @Nonnull CharSequence text, Charset charset, final long modificationStamp) {
    super(name, fileType, modificationStamp);
    setContent(text);
    setCharset(charset);
  }

  public LightVirtualFile(@Nonnull String name, final Language language, @Nonnull CharSequence text) {
    super(name, null, LocalTimeCounter.currentTime());
    setContent(text);
    setLanguage(language);
  }

  public Language getLanguage() {
    return myLanguage;
  }

  public void setLanguage(@Nonnull Language language) {
    myLanguage = language;
    FileType type = language.getAssociatedFileType();
    if (type == null) {
      type = FileTypeRegistry.getInstance().getFileTypeByFileName(getName());
    }
    setFileType(type);
  }

  @Override
  public String toString() {
    return "LightVirtualFile: " + getPresentableUrl();
  }
}
