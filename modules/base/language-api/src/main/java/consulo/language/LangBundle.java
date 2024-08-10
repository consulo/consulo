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

package consulo.language;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.component.util.localize.AbstractBundle;
import consulo.language.localize.LanguageLocalize;
import org.jetbrains.annotations.PropertyKey;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author yole
 */
@Deprecated
@DeprecationInfo("Use LanguageLocalize")
@MigratedExtensionsTo(LanguageLocalize.class)
public class LangBundle extends AbstractBundle{
  private static final LangBundle ourInstance = new LangBundle();

  private LangBundle() {
    super("consulo.language.LangBundle");
  }

  public static String message(@PropertyKey(resourceBundle = "consulo.language.LangBundle") String key) {
    return ourInstance.getMessage(key);
  }

  public static String message(@PropertyKey(resourceBundle = "consulo.language.LangBundle") String key, Object... params) {
    return ourInstance.getMessage(key, params);
  }

  @Nonnull
  public static Supplier<String> messagePointer(@Nonnull String key) {
    return () -> message(key);
  }
}
