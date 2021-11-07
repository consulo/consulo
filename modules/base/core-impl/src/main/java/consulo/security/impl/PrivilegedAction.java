/*
 * Copyright 2013-2021 consulo.io
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
package consulo.security.impl;

import consulo.container.impl.securityManager.impl.ConsuloSecurityManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 07/11/2021
 *
 * This class & package will be exported only to impl modules of Consulo. Never for plugins!
 */
public final class PrivilegedAction {
  public static void runPrivilegedAction(@Nonnull Runnable runnable) {
    ConsuloSecurityManager.runPrivilegedAction(runnable);
  }

  @Nullable
  public static <T> T runPrivilegedAction(@Nonnull Supplier<T> getter) {
    return ConsuloSecurityManager.runPrivilegedAction(getter::get);
  }
}
