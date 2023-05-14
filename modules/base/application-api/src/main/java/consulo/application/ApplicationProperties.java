/*
 * Copyright 2013-2016 consulo.io
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
package consulo.application;

import consulo.annotation.DeprecationInfo;
import consulo.util.lang.lazy.LazyValue;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 04.04.2016
 */
public final class ApplicationProperties {
  /**
   * @type boolean
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use CONSULO_IN_SANDBOX")
  private static final String IDEA_IS_INTERNAL = "idea.is.internal";

  public static boolean isInternal() {
    return Boolean.getBoolean(IDEA_IS_INTERNAL);
  }

  /**
   * @type boolean
   */
  @Nonnull
  public static final String CONSULO_IN_SANDBOX = "consulo.in.sandbox";

  private static final Supplier<Boolean> ourInSandboxValue = LazyValue.notNull(() -> Boolean.getBoolean(CONSULO_IN_SANDBOX));

  public static boolean isInSandbox() {
    return ourInSandboxValue.get();
  }
}
