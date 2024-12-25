/*
 * Copyright 2013-2023 consulo.io
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
package consulo.platform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public interface PlatformJvm {
  @Nonnull
  String version();

  @Nonnull
  String runtimeVersion();

  @Nonnull
  String vendor();

  @Nonnull
  String name();

  @Nullable
  String getRuntimeProperty(@Nonnull String key);

  @Nullable
  default String getRuntimeProperty(@Nonnull String key, @Nonnull String defaultValue) {
    String runtimeProperty = getRuntimeProperty(key);
    return runtimeProperty == null ? defaultValue : runtimeProperty;
  }

  @Nonnull
  Map<String, String> getRuntimeProperties();

  default boolean isAny64Bit() {
    return arch().width() == 64;
  }

  @Nonnull
  CpuArchitecture arch();
}
