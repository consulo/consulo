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
package consulo.util.xml.serializer.internal;

import consulo.util.xml.serializer.SerializationFilter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Text;

class TextBinding extends Binding {
  private final Class<?> valueClass;

  public TextBinding(@Nonnull MutableAccessor accessor) {
    super(accessor);

    valueClass = XmlSerializerImpl.typeToClass(accessor.getGenericType());
  }

  @Nullable
  @Override
  public Object serialize(@Nonnull Object o, @Nullable Object context, @Nonnull SerializationFilter filter) {
    Object value = myAccessor.read(o);
    return value == null ? null : new Text(XmlSerializerImpl.convertToString(value));
  }

  void set(@Nonnull Object context, @Nonnull String value) {
    XmlSerializerImpl.doSet(context, value, myAccessor, valueClass);
  }
}
