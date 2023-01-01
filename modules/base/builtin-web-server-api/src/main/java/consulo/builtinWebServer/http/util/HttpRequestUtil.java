/*
 * Copyright 2013-2019 consulo.io
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
package consulo.builtinWebServer.http.util;

import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.http.HTTPMethod;
import consulo.util.io.Url;
import consulo.util.io.Urls;
import consulo.util.lang.StringUtil;

/**
 * @author VISTALL
 * @since 2019-02-22
 */
public class HttpRequestUtil {
  public static String getHost(HttpRequest request) {
    return request.getHeaderValue("Host");
  }

  public static boolean isLocalOrigin(HttpRequest httpRequest) {
    return isLocalOrigin(httpRequest, true, false);
  }

  public static boolean isLocalOrigin(HttpRequest httpRequest, boolean onlyAnyOrLoopback, boolean hostsOnly) {
    return parseAndCheckIsLocalHost(getOrigin(httpRequest), onlyAnyOrLoopback, hostsOnly) && parseAndCheckIsLocalHost(getReferrer(httpRequest), onlyAnyOrLoopback, hostsOnly);
  }

  public static boolean parseAndCheckIsLocalHost(String uri) {
    return parseAndCheckIsLocalHost(uri, true, false);
  }


  public static String getOrigin(HttpRequest request) {
    return request.getHeaderValue("Origin");
  }

  public static String getReferrer(HttpRequest request) {
    return request.getHeaderValue("Referrer");
  }

  public static String getUserAgent(HttpRequest request) {
    return request.getHeaderValue("User-Agent");
  }

  // forbid POST requests from browser without Origin
  public static boolean isWriteFromBrowserWithoutOrigin(HttpRequest request) {
    HTTPMethod method = request.method();

    return StringUtil.isEmpty(getOrigin(request)) && isRegularBrowser(request) && (method == HTTPMethod.POST || method == HTTPMethod.PATCH || method == HTTPMethod.PUT || method == HTTPMethod.DELETE);
  }

  public static boolean isRegularBrowser(HttpRequest request) {
    String userAgent = getUserAgent(request);
    return userAgent != null && StringUtil.startsWith(userAgent, "Mozilla/5.0");
  }

  private static String getHost(Url uri) {
    String authority = uri.getAuthority();
    if (authority != null) {
      int portIndex = authority.indexOf(':');
      if (portIndex > 0) {
        return authority.substring(0, portIndex);
      }
      else {
        return authority;
      }
    }
    return null;
  }

  public static boolean parseAndCheckIsLocalHost(String uri, boolean onlyAnyOrLoopback, boolean hostsOnly) {
    if (uri == null || uri.equals("about:blank")) {
      return true;
    }

    try {
      Url parsedUri = Urls.parse(uri, false);
      if (parsedUri == null) {
        return false;
      }

      String host = getHost(parsedUri);

      return host != null && (isTrustedChromeExtension(parsedUri) || isLocalHost(host, onlyAnyOrLoopback, hostsOnly));
    }
    catch (Exception ignored) {
    }
    return false;
  }


  public static boolean isLocalHost(String host, boolean onlyAnyOrLoopback, boolean hostsOnly) {
    return BuiltInServerManager.getInstance().isLocalHost(host, onlyAnyOrLoopback, hostsOnly);
  }

  private static boolean isTrustedChromeExtension(Url url) {
    /*  FIXME [VISTALL] this is only jetbrains plugins
    return Comparing.equal(url.getScheme(), "chrome-extension") &&  (Comparing.equal(url.getAuthority(), "hmhgeddbohgjknpmjagkdomcpobmllji") || Comparing
            .equal(url.getAuthority(), "offnedcbhjldheanlbojaefbfbllddna"));
            */
    return false;
  }
}
