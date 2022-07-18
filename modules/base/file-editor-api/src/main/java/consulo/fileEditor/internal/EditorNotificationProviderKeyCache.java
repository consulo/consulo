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
package consulo.fileEditor.internal;

import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-05-08
 */
public class EditorNotificationProviderKeyCache {
  private static final Map<Class, Key> ourKeys = Maps.newConcurrentWeakKeySoftValueHashMap();

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T> Key<T> getOrCreate(Class clazz) {
    return ourKeys.computeIfAbsent(clazz, aClass -> Key.create(aClass.getName() + "$EditorNotificationProvider"));
  }
}
