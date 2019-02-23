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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.intellij.util.containers.ContainerUtil;

import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 06-May-17
 * <p>
 * from kotlin platform\built-in-server\src\org\jetbrains\builtInWebServer\BuiltInWebServer.kt
 */
public class BuiltInWebServerKt {
  public static final String TOKEN_PARAM_NAME = "_ijt";
  public static final String TOKEN_HEADER_NAME = "x-ijt";

  private static Cache<String, Boolean> tokens = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

  public static String acquireToken() {
    String token = ContainerUtil.getFirstItem(tokens.asMap().keySet());
    if (token == null) {
      token = TokenGenerator.generate();
      tokens.put(token, java.lang.Boolean.TRUE);
    }
    return token;
  }
}
