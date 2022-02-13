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

import consulo.component.extension.AbstractExtensionPointBean;
import com.intellij.openapi.util.LazyInstance;
import consulo.component.extension.KeyedLazyInstance;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.xml.serializer.annotation.Attribute;

import java.util.function.Supplier;

public class FileTypeExtensionPoint<T> extends AbstractExtensionPointBean implements KeyedLazyInstance<T> {

  // these must be public for scrambling compatibility
  @Attribute("filetype")
  public String filetype;

  @Attribute("implementationClass")
  public String implementationClass;

  private final Supplier<T> myHandler = LazyValue.<T>notNull(() -> findClass(implementationClass));
  @Override
  public T getInstance() {
    return myHandler.get();
  }

  @Override
  public String getKey() {
    return filetype;
  }
}