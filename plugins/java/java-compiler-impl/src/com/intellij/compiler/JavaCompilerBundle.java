/*
 * Copyright 2013 Consulo.org
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
package com.intellij.compiler;

import com.intellij.AbstractBundle;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author VISTALL
 * @since 18:11/20.10.13
 */
public class JavaCompilerBundle extends AbstractBundle {
  private static final JavaCompilerBundle INSTANCE = new JavaCompilerBundle();

  private static final String BUNDLE = "messages.JavaCompilerBundle";

  private JavaCompilerBundle() {
    super(BUNDLE);
  }

  @NotNull
  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... arg) {
    return INSTANCE.getMessage(key, arg);
  }

  @NotNull
  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key) {
    return INSTANCE.getMessage(key, ArrayUtil.EMPTY_OBJECT_ARRAY);
  }
}
