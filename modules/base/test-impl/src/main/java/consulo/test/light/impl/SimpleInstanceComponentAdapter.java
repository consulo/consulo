/*
 * Copyright 2013-2019 consulo.io
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
package consulo.test.light.impl;

import com.intellij.openapi.extensions.impl.ExtensionComponentAdapter;
import org.jdom.Element;

/**
 * @author VISTALL
 * @since 2019-02-25
 */
class SimpleInstanceComponentAdapter<T> extends ExtensionComponentAdapter<T> {
  @SuppressWarnings("unchecked")
  SimpleInstanceComponentAdapter(T value) {
    super(value.getClass().getName(), new Element("instance"), null, false);
    myComponentInstance = value;
    myImplementationClass = (Class<T>)value.getClass();
  }
}
