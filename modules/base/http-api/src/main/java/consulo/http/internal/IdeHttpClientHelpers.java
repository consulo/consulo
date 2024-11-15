/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.http.internal;

import consulo.http.HttpProxyManager;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

/**
 * @author Mikhail Golubev
 */
public class IdeHttpClientHelpers {
    private IdeHttpClientHelpers() {
    }

    @Nonnull
    public static HttpProxyManager getHttpProxyManager() {
        return HttpProxyManager.getInstance();
    }

    public static boolean isHttpProxyEnabled() {
        return getHttpProxyManager().isHttpProxyEnabled();
    }

    public static boolean isProxyAuthenticationEnabled() {
        return getHttpProxyManager().isProxyAuthenticationEnabled();
    }

    @Nonnull
    public static String getProxyHost() {
        return StringUtil.notNullize(getHttpProxyManager().getProxyHost());
    }

    public static int getProxyPort() {
        return getHttpProxyManager().getProxyPort();
    }

    @Nonnull
    public static String getProxyLogin() {
        return StringUtil.notNullize(getHttpProxyManager().getProxyLogin());
    }

    @Nonnull
    public static String getProxyPassword() {
        return StringUtil.notNullize(getHttpProxyManager().getPlainProxyPassword());
    }
}
