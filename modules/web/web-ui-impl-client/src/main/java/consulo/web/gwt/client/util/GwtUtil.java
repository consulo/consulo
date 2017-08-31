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
package consulo.web.gwt.client.util;

import com.google.gwt.core.shared.GWT;
import consulo.annotations.DeprecationInfo;
import consulo.web.gwt.client.service.FetchService;
import consulo.web.gwt.shared.GwtTransportService;
import consulo.web.gwt.shared.GwtTransportServiceAsync;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-May-16
 */
@Deprecated
@DeprecationInfo("This is part of research 'consulo as web app'. Code was written in hacky style. Must be dropped, or replaced by Consulo UI API")
public class GwtUtil {
  private static final GwtTransportServiceAsync ourAsyncService = GWT.create(GwtTransportService.class);

  private static final Map<String, FetchService> ourServices = new HashMap<String, FetchService>();

  public static GwtTransportServiceAsync rpc() {
    return ourAsyncService;
  }

  public static void put(String key, FetchService fetchService) {
    ourServices.put(key, fetchService);
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(String key) {
    return (T)ourServices.get(key);
  }
}
