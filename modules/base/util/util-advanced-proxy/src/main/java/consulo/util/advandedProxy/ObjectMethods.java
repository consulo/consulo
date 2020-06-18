/*
 * Copyright 2013-2020 consulo.io
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
package consulo.util.advandedProxy;

import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2020-06-18
 */
public final class ObjectMethods {
  public static final Method FINALIZE_METHOD;
  public static final Method EQUALS_METHOD;
  public static final Method HASHCODE_METHOD;
  public static final Method TOSTRING_METHOD;

  static {
    try {
      FINALIZE_METHOD = Object.class.getDeclaredMethod("finalize");
      EQUALS_METHOD = Object.class.getDeclaredMethod("equals", Object.class);
      HASHCODE_METHOD = Object.class.getDeclaredMethod("hashCode");
      TOSTRING_METHOD = Object.class.getDeclaredMethod("toString");
    }
    catch (NoSuchMethodException e) {
      throw new Error(e);
    }
  }
}
