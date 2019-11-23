/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.util.lang.UrlClassLoader;

import javax.annotation.Nonnull;

public class ClassLoaderUtilRt {

  public static void addPlatformLoaderParentIfOnJdk9(@Nonnull UrlClassLoader.Builder builder) {
    if (SystemInfoRt.IS_AT_LEAST_JAVA9) {
      // on Java 8, 'tools.jar' is on a classpath; on Java 9, its classes are available via the platform loader
      try {
        ClassLoader platformCl = (ClassLoader)ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
        builder.parent(platformCl);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
