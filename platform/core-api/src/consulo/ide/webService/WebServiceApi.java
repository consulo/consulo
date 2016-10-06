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
package consulo.ide.webService;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 24-Sep-16
 *
 * TODO [VISTALL] missed:
 * > statistics
 * > updates - think it will be dropped
 */
public enum WebServiceApi {
  MAIN("https://consulo.io"),
  ERROR_REPORTER_API("https://hub.consulo.io/api/errorReporter/"),
  PLUGINS_API("https://hub.consulo.io/api/plugins/");

  private String myDefaultUrl;
  private String myOverrideProperty;

  WebServiceApi(String defaultUrl) {
    myDefaultUrl = defaultUrl;
    myOverrideProperty = "consulo." + name().toLowerCase(Locale.US).replace("_", ".");
  }

  @NotNull
  public String buildUrl(@NotNull String urlPart) {
    return buildUrl() + urlPart;
  }

  @NotNull
  public String buildUrl() {
    return System.getProperty(myOverrideProperty, myDefaultUrl);
  }
}
