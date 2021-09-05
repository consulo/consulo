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
package consulo.externalService;

import consulo.ui.image.Image;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 05/09/2021
 */
public interface ExternalServiceConfiguration {
  @Nullable
  String getEmail();

  boolean isAuthorized();

  @Nullable
  Image getUserIcon();

  void updateIcon();

  /**
   * @see ThreeState documentation
   * @return state or default state
   */
  @Nonnull
  ThreeState getState(@Nonnull ExternalService externalService);

  void setState(@Nonnull ExternalService externalService, @Nonnull ThreeState state);
}
