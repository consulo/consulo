/*
 * Copyright 2013-2022 consulo.io
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
package consulo.project.ui.view.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.util.function.ThrowableComputable;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.util.lang.function.ThrowableRunnable;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProjectViewInternalHelper {
  public static final KeyWithDefaultValue<Boolean> SHOW_EXCLUDED_FILES_KEY = KeyWithDefaultValue.create("show-excluded-files", Boolean.FALSE);

  static ProjectViewInternalHelper getInstance() {
    return Application.get().getInstance(ProjectViewInternalHelper.class);
  }

  default <E extends Throwable> void disallowTreeLoading(@Nonnull ThrowableRunnable<E> runnable) throws E {
    disallowTreeLoading(toComputable(runnable));
  }

  default <T, E extends Throwable> T disallowTreeLoading(@Nonnull ThrowableComputable<? extends T, E> computable) throws E {
    return disallowTreeLoading(computable, () -> null);
  }

  <T, E extends Throwable> T disallowTreeLoading(@Nonnull ThrowableComputable<? extends T, E> computable, @Nonnull Supplier<String> debugInfo) throws E;

  private static <E extends Throwable> ThrowableComputable<?, E> toComputable(ThrowableRunnable<? extends E> runnable) {
    return () -> {
      runnable.run();
      return null;
    };
  }
}