/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.compiler.localize.CompilerLocalize;
import consulo.component.util.localize.AbstractBundle;
import consulo.content.bundle.Sdk;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author Eugene Zhuravlev
 * @since 2005-09-09
 */
@Deprecated(forRemoval = true)
@DeprecationInfo("Use CompilerLocalize")
@MigratedExtensionsTo(CompilerLocalize.class)
public class CompilerBundle extends AbstractBundle {
  private static final CompilerBundle ourInstance = new CompilerBundle();

  private CompilerBundle() {
    super("consulo.compiler.CompilerBundle");
  }

  public static String message(@PropertyKey(resourceBundle = "consulo.compiler.CompilerBundle") String key) {
    return ourInstance.getMessage(key);
  }

  public static String message(@PropertyKey(resourceBundle = "consulo.compiler.CompilerBundle") String key, Object... params) {
    return ourInstance.getMessage(key, params);
  }

  public static String jdkHomeNotFoundMessage(final Sdk jdk) {
    return message("javac.error.jdk.home.missing", jdk.getName(), jdk.getHomePath());
  }
}
