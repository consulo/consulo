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
import consulo.extension.impl.ModuleExtensionImpl;
import consulo.extension.impl.ModuleExtensionWithSdkImpl;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author VISTALL
 * @since 25-Nov-16.
 */
public class ModuleExtensionIndexCache {
  private static Map<Class<?>, int[]> ourClassCache = new THashMap<>();

  @NotNull
  public static int[] get(@NotNull Class<?> clazz) {
    int[] ints = ourClassCache.get(clazz);
    return ints == null ? ArrayUtil.EMPTY_INT_ARRAY : ints;
  }

  public static synchronized void put(@NotNull Class<?> clazz, int index) {
    put0(clazz, index);
  }

  private static void put0(Class<?> clazz, int index) {
    Class temp = clazz;

    do {
      if (temp == ModuleExtensionWithSdkImpl.class ||
          temp == ModuleExtensionImpl.class ||
          temp == ModuleExtension.class ||
          temp == ModuleExtensionWithSdk.class) {
        break;
      }

      putImpl(temp, index);

      Class[] interfaces = temp.getInterfaces();
      for (Class intef : interfaces) {
        put0(intef, index);
      }
    }
    while ((temp = temp.getSuperclass()) != null);
  }

  private static void putImpl(Class<?> temp, int index) {
    if (temp == ModuleExtensionWithSdkImpl.class ||
        temp == ModuleExtensionImpl.class ||
        temp == ModuleExtension.class ||
        temp == ModuleExtensionWithSdk.class) {
      return;
    }

    int[] ints = ourClassCache.get(temp);
    if (ints == null) {
      ourClassCache.put(temp, new int[]{index});
    }
    else {
      ourClassCache.put(temp, ArrayUtil.append(ints, index));
    }
  }
}
