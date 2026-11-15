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
package consulo.util.xml.serializer;

import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.internal.DefaultValue;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class SkipDefaultValuesSerializationFilters extends SerializationFilterBase {
  private static final Set<SkipDefaultValuesSerializationFilters> ourInstances =
    Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

  private final Map<Class<?>, Object> myDefaultBeans = new ConcurrentHashMap<>();

  {
    ourInstances.add(this);
  }

  /**
   * Forgets default beans of classes from the class loader of an unloaded plugin.
   */
  static void dropCachedClasses(ClassLoader classLoader) {
    synchronized (ourInstances) {
      for (SkipDefaultValuesSerializationFilters filters : ourInstances) {
        filters.myDefaultBeans.keySet().removeIf(aClass -> aClass.getClassLoader() == classLoader);
      }
    }
  }

  @Override
  protected boolean accepts(Accessor accessor, Object bean, @Nullable Object beanValue) {
    Object defValue = accessor.read(getDefaultBean(bean));
    if (defValue instanceof Element && beanValue instanceof Element) {
      return !JDOMUtil.areElementsEqual((Element)beanValue, (Element)defValue);
    }
    else {
      return !Objects.equals(beanValue, defValue);
    }
  }

  Object getDefaultBean(Object bean) {
    Class<?> c = bean.getClass();
    Object o = myDefaultBeans.get(c);
    if (o == null) {
      o = DefaultValue.createDefaultInstance(c);
      configure(o);

      myDefaultBeans.put(c, o);
    }
    return o;
  }

  /**
   * Override to put your own default object configuration
   */
  protected void configure(Object o) {
  }
}
