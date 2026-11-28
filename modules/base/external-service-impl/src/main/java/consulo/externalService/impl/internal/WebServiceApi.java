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
package consulo.externalService.impl.internal;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * @author VISTALL
 * @since 24-Sep-16
 */
public enum WebServiceApi {
    MAIN("https://consulo.io"),
    HELP("https://consulo.help/"),
    ERROR_REPORTER_API("https://api.consulo.io/errorReporter/", "http://api.ide.rocks/errorReporter/"),
    ERROR_REPORT("https://hub.consulo.io/public/errorReport/"),
    STATISTICS_API("https://api.consulo.io/statistics/", "http://api.ide.rocks/statistics/"),
    DEVELOPER_API("https://api.consulo.io/developer/", "http://api.ide.rocks/developer/"),
    STORAGE_API("https://api.consulo.io/storage/", "http://api.ide.rocks/storage/"),
    OAUTH_API("https://api.consulo.io/oauth/", "http://api.ide.rocks/oauth/"),
    LINK_CONSULO("https://hub.consulo.io/link"),
    REPOSITORY_API("https://api.consulo.io/repository/", "http://api.ide.rocks/repository/"),
    DISCUSSION("https://github.com/orgs/consulo/discussions");

    private final String myDefaultUrl;
    private final @Nullable String myFallbackUrl;
    private final String myOverrideProperty;

    WebServiceApi(String defaultUrl) {
        this(defaultUrl, null);
    }

    /**
     * @param fallbackUrl the same api on another host, for where the default one can not be reached - http only
     */
    WebServiceApi(String defaultUrl, @Nullable String fallbackUrl) {
        myDefaultUrl = defaultUrl;
        myFallbackUrl = fallbackUrl;
        myOverrideProperty = "consulo." + name().toLowerCase(Locale.US).replace("_", ".");
    }

    public String buildUrl(String urlPart) {
        return buildUrl() + urlPart;
    }

    public String buildUrl() {
        return System.getProperty(myOverrideProperty, myDefaultUrl);
    }

    /**
     * The url on the other host of this api - null where it has no other host, or the url does not come from the
     * default one, so an api pointed somewhere else by its property keeps the host it was given.
     *
     * @param url a url built by {@link #buildUrl(String)}
     * @see WebServiceApiFallback
     */
    public @Nullable String buildFallbackUrl(String url) {
        if (myFallbackUrl == null || !url.startsWith(myDefaultUrl)) {
            return null;
        }

        return myFallbackUrl + url.substring(myDefaultUrl.length());
    }
}
