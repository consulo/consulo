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
package consulo.http.impl.internal.proxy;

/**
 * @author VISTALL
 * @since 23/01/2023
 */
public class HttpProxyManagerState {
  public boolean PROXY_TYPE_IS_SOCKS;
  public boolean USE_HTTP_PROXY;
  public boolean USE_PROXY_PAC;

  public volatile boolean PROXY_AUTHENTICATION;
  public boolean KEEP_PROXY_PASSWORD;
  public transient String LAST_ERROR;

  public String PROXY_HOST;
  public int PROXY_PORT = 80;
  public String PROXY_EXCEPTIONS;
  public boolean USE_PAC_URL;
  public String PAC_URL;
}
