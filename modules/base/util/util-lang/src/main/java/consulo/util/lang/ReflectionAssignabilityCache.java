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
package consulo.util.lang;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author peter
 */
public class ReflectionAssignabilityCache {
  private final ConcurrentMap<Class, ConcurrentMap<Class, Boolean>> myCache = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public boolean isAssignable(Class ancestor, Class descendant) {
    if (ancestor == descendant) {
      return true;
    }

    ConcurrentMap<Class, Boolean> ancestorMap = myCache.computeIfAbsent(ancestor, it -> new ConcurrentHashMap<>());
    return ancestorMap.computeIfAbsent(descendant, ancestor::isAssignableFrom);
  }
}
