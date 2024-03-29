/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.util.xml.serializer;

import consulo.util.lang.ThreeState;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public class SkipEmptySerializationFilter extends SerializationFilterBase {
  @Override
  protected boolean accepts(@Nonnull Accessor accessor, @Nonnull Object bean, @Nullable Object beanValue) {
    if (beanValue == null) {
      return false;
    }

    ThreeState accepts = accepts(accessor.getName(), beanValue);
    if (accepts != ThreeState.UNSURE) {
      return accepts.toBoolean();
    }

    if (Boolean.FALSE.equals(beanValue) ||
        (beanValue instanceof String && ((String)beanValue).isEmpty()) ||
        beanValue instanceof Collection && ((Collection)beanValue).isEmpty() ||
        (beanValue instanceof Map && ((Map)beanValue).isEmpty())) {
      return false;
    }

    return true;
  }

  protected ThreeState accepts(@Nonnull String name, @Nonnull Object beanValue) {
    return ThreeState.UNSURE;
  }
}