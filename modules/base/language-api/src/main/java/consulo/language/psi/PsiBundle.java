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
package consulo.language.psi;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class PsiBundle extends AbstractBundle{
  private static final PsiBundle ourInstance = new PsiBundle();

  private PsiBundle() {
    super("consulo.language.psi.PsiBundle");
  }

  public static String message(@PropertyKey(resourceBundle = "consulo.language.psi.PsiBundle") String key) {
    return ourInstance.getMessage(key);
  }

  public static String message(@PropertyKey(resourceBundle = "consulo.language.psi.PsiBundle") String key, Object... params) {
    return ourInstance.getMessage(key, params);
  }

  @Nonnull
  public static String visibilityPresentation(@Nonnull String modifier) {
    return message(modifier + ".visibility.presentation");
  }
}
