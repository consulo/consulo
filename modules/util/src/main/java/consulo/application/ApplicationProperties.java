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

import consulo.annotations.DeprecationInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 04.04.2016
 */
public interface ApplicationProperties {
  @NotNull
  @NonNls
  @Deprecated
  @DeprecationInfo("Old idea internal mode - replaced by sandbox mode")
  String IDEA_IS_INTERNAL = "idea.is.internal";

  @NotNull
  @NonNls
  String CONSULO_IN_SANDBOX = "consulo.in.sandbox";

  @NotNull
  @NonNls
  String CONSULO_IN_UNIT_TEST = "consulo.is.unit.test";

  @NotNull
  @NonNls
  String CONSULO_AS_WEB_APP = "consulo.as.web.app";

  @NotNull
  @NonNls
  @Deprecated
  @DeprecationInfo("Old idea plugins path. See #CONSULO_PLUGINS_PATHS")
  String IDEA_PLUGINS_PATH = "idea.plugins.path";

  @NotNull
  @NonNls
  String CONSULO_PLUGINS_PATHS = "consulo.plugins.paths";

  @NotNull
  @NonNls
  String CONSULO_INSTALL_PLUGINS_PATH = "consulo.install.plugins.path";

  /**
   * Disable using external platform directory for platform updates
   *
   * @type boolean
   */
  @NotNull
  @NonNls
  String CONSULO_NO_EXTERNAL_PLATFORM = "consulo.no.external.platform";

  /**
   * Path to boot application home
   *
   * @type String
   */
  @NotNull
  @NonNls
  String CONSULO_APP_HOME_PATH = "consulo.app.home.path";
}
