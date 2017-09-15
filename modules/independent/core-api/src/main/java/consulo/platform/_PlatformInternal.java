/*
 * Copyright 2013-2017 consulo.io
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
package consulo.platform;

import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
abstract class _PlatformInternal {
  private static Platform ourPlatform;

  @NotNull
  static Platform current() {
    if (ourPlatform == null) {
      Platform platform = initialize();
      ourPlatform = platform;
      return platform;
    }
    return ourPlatform;
  }

  @NotNull
  static Platform initialize() {
    try {
      Class<?> clazz = Class.forName(_PlatformInternal.class.getName() + "Impl");
      _PlatformInternal o = (_PlatformInternal)ReflectionUtil.newInstance(clazz);
      return o.build();
    }
    catch (Exception e) {
      throw new Error(e);
    }
  }

  @NotNull
  abstract Platform build();
}
