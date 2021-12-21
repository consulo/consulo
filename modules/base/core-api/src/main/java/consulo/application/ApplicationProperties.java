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

import com.intellij.openapi.util.NotNullLazyValue;
import consulo.annotation.DeprecationInfo;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04.04.2016
 */
public class ApplicationProperties {
  /**
   * @type boolean
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use CONSULO_IN_SANDBOX")
  public static final String IDEA_IS_INTERNAL = "idea.is.internal";

  public static boolean isInternal() {
    return Boolean.getBoolean(IDEA_IS_INTERNAL);
  }

  /**
   * @type boolean
   */
  @Nonnull
  public static final String CONSULO_IN_SANDBOX = "consulo.in.sandbox";

  private static final NotNullLazyValue<Boolean> ourInSandboxValue = NotNullLazyValue.createValue(() -> Boolean.getBoolean(CONSULO_IN_SANDBOX));

  public static boolean isInSandbox() {
    return ourInSandboxValue.getValue();
  }

  /**
   * @type boolean
   */
  @Nonnull
  @Deprecated
  public static final String CONSULO_IN_UNIT_TEST = "consulo.is.unit.test";

  /**
   * Disable using external platform directory for platform updates
   *
   * @type boolean
   */
  @Nonnull
  public static final String CONSULO_NO_EXTERNAL_PLATFORM = "consulo.no.external.platform";

  /**
   * Path to boot application home
   *
   * @type String
   */
  @Nonnull
  public static final String CONSULO_APP_HOME_PATH = "consulo.app.home.path";

  @Nonnull
  @Deprecated
  @DeprecationInfo("Old idea plugins path. See #CONSULO_PLUGINS_PATHS")
  public static final String IDEA_PLUGINS_PATH = "idea.plugins.path";

  /**
   * Path for plugin install directory. Not included in plugin path list
   * @type
   */
  @Nonnull
  public static final String CONSULO_INSTALL_PLUGINS_PATH = "consulo.install.plugins.path";

  /**
   * List of plugin directories for loading. If size more that one - CONSULO_INSTALL_PLUGINS_PATH must be installed
   * @type String[]
   */
  @Nonnull
  public static final String CONSULO_PLUGINS_PATHS = "consulo.plugins.paths";

  /**
   * Redirect log to console. Used when running from maven
   * @type boolean
   */
  public static final String CONSULO_MAVEN_CONSOLE_LOG = "consulo.maven.console.log";
}
