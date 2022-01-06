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
package consulo.externalService.impl;

import javax.annotation.Nonnull;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 24-Sep-16
 */
public enum WebServiceApi {
  MAIN("https://consulo.io"),
  HELP("https://consulo.help/"),
  ERROR_REPORTER_API("https://hub.consulo.io/api/errorReporter/"),
  ERROR_REPORT("https://hub.consulo.io/errorReport"),
  STATISTICS_API("https://hub.consulo.io/api/statistics/"),
  DEVELOPER_API("https://hub.consulo.io/api/developer/"),
  STORAGE_API("https://hub.consulo.io/api/storage/"),
  OAUTH_API("https://hub.consulo.io/api/oauth/"),
  LINK_CONSULO("https://hub.consulo.io/#!linkConsulo"),
  REPOSITORY_API("https://hub.consulo.io/api/repository/");

  private String myDefaultUrl;
  private String myOverrideProperty;

  WebServiceApi(@Nonnull String defaultUrl) {
    myDefaultUrl = defaultUrl;
    myOverrideProperty = "consulo." + name().toLowerCase(Locale.US).replace("_", ".");
  }

  @Nonnull
  public String buildUrl(@Nonnull String urlPart) {
    return buildUrl() + urlPart;
  }

  @Nonnull
  public String buildUrl() {
    return System.getProperty(myOverrideProperty, myDefaultUrl);
  }
}
