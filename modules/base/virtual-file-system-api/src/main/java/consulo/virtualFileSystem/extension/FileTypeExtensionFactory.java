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

/*
 * @author max
 */
package consulo.virtualFileSystem.extension;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointName;
import consulo.component.extension.KeyedFactoryEPBean;
import consulo.application.extension.KeyedExtensionFactory;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

public class FileTypeExtensionFactory<T> extends KeyedExtensionFactory<T, FileType> {
  public FileTypeExtensionFactory(@Nonnull final Class<T> interfaceClass, @NonNls @Nonnull final ExtensionPointName<KeyedFactoryEPBean> epName) {
    super(interfaceClass, epName, Application.get());
  }

  @Override
  public String getKey(@Nonnull final FileType key) {
    return key.getId();
  }
}