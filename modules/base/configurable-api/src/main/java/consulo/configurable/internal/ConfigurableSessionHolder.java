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
package consulo.configurable.internal;

import consulo.configurable.ConfigurableSession;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2022-01-20
 */
public class ConfigurableSessionHolder {
  public static @Nullable ConfigurableSession ourCurrentSession = null;

  @RequiredUIAccess
  public static ConfigurableSession get() {
    UIAccess.assertIsUIThread();
    return Objects.requireNonNull(ourCurrentSession, "Session is not initialized");
  }
}
