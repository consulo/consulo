/*
 * Copyright 2013-2016 consulo.io
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
package consulo.module.extension.impl;

import com.intellij.util.ArrayUtil;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25-Nov-16.
 */
public class ModuleExtensionIndexCache {
  private static volatile Map<Class<?>, int[]> ourClassCache;

  @Nonnull
  public static int[] get(@Nonnull Class<?> clazz) {
    if (ourClassCache == null) {
      throw new IllegalArgumentException("Calling #get() without initializing.");
    }

    int[] ints = ourClassCache.get(clazz);
    return ints == null ? ArrayUtil.EMPTY_INT_ARRAY : ints;
  }

  public static void putMap(@Nonnull Map<Class<?>, int[]> map) {
    ourClassCache = map;
  }

  public static void putToMap(@Nonnull Map<Class<?>, int[]> map, @Nonnull Class<?> clazz, int index) {
    putToMap0(map, clazz, index);
  }

  private static void putToMap0(@Nonnull Map<Class<?>, int[]> map, Class<?> clazz, int index) {
    Class temp = clazz;

    do {
      if (temp == ModuleExtensionWithSdkImpl.class || temp == ModuleExtensionImpl.class || temp == ModuleExtension.class || temp == ModuleExtensionWithSdk.class) {
        break;
      }

      putToMapImpl(map, temp, index);

      Class[] interfaces = temp.getInterfaces();
      for (Class intef : interfaces) {
        putToMap0(map, intef, index);
      }
    }
    while ((temp = temp.getSuperclass()) != null);
  }

  private static void putToMapImpl(@Nonnull Map<Class<?>, int[]> map, Class<?> temp, int index) {
    if (temp == ModuleExtensionWithSdkImpl.class || temp == ModuleExtensionImpl.class || temp == ModuleExtension.class || temp == ModuleExtensionWithSdk.class) {
      return;
    }

    int[] ints = map.get(temp);
    if (ints == null) {
      map.put(temp, new int[]{index});
    }
    else {
      map.put(temp, ArrayUtil.append(ints, index));
    }
  }
}
