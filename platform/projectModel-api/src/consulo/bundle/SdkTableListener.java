/*
 * Copyright 2013-2015 must-be.org
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
package consulo.bundle;

import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 22.05.2015
 */
public interface SdkTableListener {
  class Adapter implements SdkTableListener {
    @Override
    public void beforeSdkAdded(@NotNull Sdk sdk) {

    }

    @Override
    public void sdkAdded(@NotNull Sdk sdk) {

    }

    @Override
    public void beforeSdkRemoved(@NotNull Sdk sdk) {

    }

    @Override
    public void sdkRemoved(@NotNull Sdk sdk) {

    }

    @Override
    public void beforeSdkNameChanged(@NotNull Sdk sdk, @NotNull String previousName) {

    }

    @Override
    public void sdkNameChanged(@NotNull Sdk sdk, @NotNull String previousName) {

    }
  }

  void beforeSdkAdded(@NotNull Sdk sdk);

  void sdkAdded(@NotNull Sdk sdk);

  void beforeSdkRemoved(@NotNull Sdk sdk);

  void sdkRemoved(@NotNull Sdk sdk);

  void beforeSdkNameChanged(@NotNull Sdk sdk, @NotNull String previousName);

  void sdkNameChanged(@NotNull Sdk sdk, @NotNull String previousName);
}
