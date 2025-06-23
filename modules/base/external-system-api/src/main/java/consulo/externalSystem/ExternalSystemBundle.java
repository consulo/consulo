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
package consulo.externalSystem;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.component.util.localize.AbstractBundle;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import org.jetbrains.annotations.PropertyKey;

import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 2011-08-01
 */
@Deprecated(forRemoval = true)
@DeprecationInfo("Use ExternalSystemLocalize")
@MigratedExtensionsTo(ExternalSystemLocalize.class)
public class ExternalSystemBundle extends AbstractBundle {

  public static String message(@Nonnull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, @Nonnull Object... params) {
    return BUNDLE.getMessage(key, params);
  }

  public static final String PATH_TO_BUNDLE = "consulo.externalSystem.ExternalSystemBundle";
  private static final ExternalSystemBundle BUNDLE = new ExternalSystemBundle();

  public ExternalSystemBundle() {
    super(PATH_TO_BUNDLE);
  }
}
