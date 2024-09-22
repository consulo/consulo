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

import consulo.util.xml.serializer.internal.AbstractCollectionBinding;
import consulo.util.xml.serializer.internal.MutableAccessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;

class ArrayBinding extends AbstractCollectionBinding {
  public ArrayBinding(@Nonnull Class<?> valueClass, @Nullable MutableAccessor accessor) {
    super(valueClass.getComponentType(), accessor);
  }

  @Override
  protected String getCollectionTagName(@Nullable Object target) {
    return "array";
  }

  @Override
  @SuppressWarnings({"unchecked"})
  Object processResult(Collection result, Object target) {
    return result.toArray((Object[])Array.newInstance(itemType, result.size()));
  }

  @Nonnull
  @Override
  Collection<Object> getIterable(@Nonnull Object o) {
    return Arrays.asList((Object[])o);
  }
}
