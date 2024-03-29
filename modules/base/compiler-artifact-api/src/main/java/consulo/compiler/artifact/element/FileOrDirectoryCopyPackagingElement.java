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
package consulo.compiler.artifact.element;

import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public abstract class FileOrDirectoryCopyPackagingElement<T extends FileOrDirectoryCopyPackagingElement> extends PackagingElement<T> {
  @NonNls public static final String PATH_ATTRIBUTE = "path";
  protected String myFilePath;

  public FileOrDirectoryCopyPackagingElement(PackagingElementType type) {
    super(type);
  }

  protected FileOrDirectoryCopyPackagingElement(PackagingElementType type, String filePath) {
    super(type);
    myFilePath = filePath;
  }

  @Nullable
  public VirtualFile findFile() {
    return LocalFileSystem.getInstance().findFileByPath(myFilePath);
  }

  @Override
  public boolean isEqualTo(@Nonnull PackagingElement<?> element) {
    return element instanceof FileOrDirectoryCopyPackagingElement &&
           myFilePath != null &&
           myFilePath.equals(((FileOrDirectoryCopyPackagingElement)element).getFilePath());
  }

  @Attribute(PATH_ATTRIBUTE)
  public String getFilePath() {
    return myFilePath;
  }

  public void setFilePath(String filePath) {
    myFilePath = filePath;
  }
}
