/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.io;

import com.intellij.Patches;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FileResponses {
  private static Method getContentTypeMethod;
  private static Object ourFileTypeMap;

  static {
    assert Patches.USE_REFLECTION_TO_ACCESS_JDK11;

    try {
      Class<?> clazz = Class.forName("javax.activation.MimetypesFileTypeMap");
      getContentTypeMethod = clazz.getDeclaredMethod("getContentType", String.class);
      getContentTypeMethod.setAccessible(true);

      ourFileTypeMap = clazz.newInstance();
    }
    catch (Exception e) {
      // class not found at jdk 11
    }
  }

  @Nullable
  public static String getContentType(String path) {
    if (ourFileTypeMap == null) {
      return null;
    }
    try {
      return (String)getContentTypeMethod.invoke(ourFileTypeMap, path);
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
