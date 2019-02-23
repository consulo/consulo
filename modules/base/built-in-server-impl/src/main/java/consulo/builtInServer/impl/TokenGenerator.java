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
package consulo.builtInServer.impl;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * @author VISTALL
 * @since 06-May-17
 * <p>
 * from kotlin platform\built-in-server\src\org\jetbrains\builtInWebServer\BuiltInWebServer.kt
 */
class TokenGenerator {
  private static SecureRandom random = new SecureRandom();

  static String generate() {
    return new BigInteger(130, random).toString(32);
  }
}
