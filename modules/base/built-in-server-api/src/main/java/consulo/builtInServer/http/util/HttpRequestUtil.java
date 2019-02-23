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
package consulo.builtInServer.http.util;

import com.google.common.net.InetAddresses;
import com.intellij.openapi.util.ThrowableNotNullFunction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import consulo.net.util.NetUtil;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.resolver.HostsFileEntriesResolver;
import io.netty.resolver.ResolvedAddressTypes;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * @author VISTALL
 * @since 2019-02-22
 */
public class HttpRequestUtil {
  public static String getHost(HttpRequest request) {
    return request.headers().getAsString(HttpHeaderNames.HOST);
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
    return request.headers().getAsString(HttpHeaderNames.ORIGIN);
  }

  public static String getReferrer(HttpRequest request) {
    return request.headers().getAsString(HttpHeaderNames.REFERER);
  }

  public static String getUserAgent(HttpRequest request) {
    return request.headers().getAsString(HttpHeaderNames.USER_AGENT);
  }

  // forbid POST requests from browser without Origin
  public static boolean isWriteFromBrowserWithoutOrigin(HttpRequest request) {
    HttpMethod method = request.method();

    return StringUtil.isEmpty(getOrigin(request)) && isRegularBrowser(request) && (method == HttpMethod.POST || method == HttpMethod.PATCH || method == HttpMethod.PUT || method == HttpMethod.DELETE);
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
    if (NetUtil.isLocalhost(host)) {
      return true;
    }

    // if IP address, it is safe to use getByName (not affected by DNS rebinding)
    if (onlyAnyOrLoopback && !InetAddresses.isInetAddress(host)) {
      return false;
    }

    ThrowableNotNullFunction<InetAddress, Boolean, SocketException> isLocal =
            inetAddress -> inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || NetworkInterface.getByInetAddress(inetAddress) != null;

    try {
      InetAddress address = InetAddress.getByName(host);
      if (!isLocal.fun(address)) {
        return false;
      }
      // be aware - on windows hosts file doesn't contain localhost
      // hosts can contain remote addresses, so, we check it
      if (hostsOnly && !InetAddresses.isInetAddress(host)) {
        InetAddress hostInetAddress = HostsFileEntriesResolver.DEFAULT.address(host, ResolvedAddressTypes.IPV4_PREFERRED);
        return hostInetAddress != null && isLocal.fun(hostInetAddress);
      }
      else {
        return true;
      }
    }
    catch (IOException ignored) {
      return false;
    }
  }

  private static boolean isTrustedChromeExtension(Url url) {
    /*  FIXME [VISTALL] this is only jetbrains plugins
    return Comparing.equal(url.getScheme(), "chrome-extension") &&  (Comparing.equal(url.getAuthority(), "hmhgeddbohgjknpmjagkdomcpobmllji") || Comparing
            .equal(url.getAuthority(), "offnedcbhjldheanlbojaefbfbllddna"));
            */
    return false;
  }
}
