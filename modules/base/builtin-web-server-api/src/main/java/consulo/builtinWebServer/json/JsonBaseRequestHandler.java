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
package consulo.builtinWebServer.json;

import consulo.annotation.DeprecationInfo;
import consulo.application.json.JsonService;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.http.OriginCheckResult;
import consulo.builtinWebServer.http.util.HttpRequestUtil;
import consulo.builtinWebServer.localize.BuiltInServerLocalize;
import consulo.http.HttpMethod;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.util.collection.Maps;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonBaseRequestHandler extends HttpRequestHandler {
    public static final class JsonResponse {
        public boolean success;
        public String message;
        public Object data;

        @Deprecated
        @DeprecationInfo("don't use it, used for serialize")
        public JsonResponse() {
        }

        public static JsonResponse asSuccess(@Nullable Object data) {
            JsonResponse response = new JsonResponse();
            response.success = true;
            response.message = null;
            response.data = data;
            return response;
        }

        public static JsonResponse asError(String message) {
            JsonResponse response = new JsonResponse();
            response.success = false;
            response.message = message;
            return response;
        }
    }

    private static final Logger LOG = Logger.getInstance(JsonBaseRequestHandler.class);

    private static final String API_PREFIX = "/api/";

    private static final String REFERER_HEADER_NAME = "Referer";

    private static final int TRUSTED_ORIGINS_MAXIMUM_SIZE = 1024;

    private static final long TRUSTED_ORIGINS_EXPIRE_AFTER_WRITE = TimeUnit.DAYS.toMillis(1);

    private final String myApiUrl;

    private final Map<Pair<String, String>, Pair<Boolean, Long>> myTrustedOrigins = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Pair<String, String>, Pair<Boolean, Long>> eldest) {
            return size() > TRUSTED_ORIGINS_MAXIMUM_SIZE;
        }
    };

    private final ConcurrentMap<String, Object> myHostLocks = Maps.newConcurrentWeakKeyWeakValueHashMap();

    private volatile boolean myBlockUnknownHosts;

    protected JsonBaseRequestHandler(String apiUrl) {
        myApiUrl = API_PREFIX + apiUrl;
    }

    /**
     * Use a human-readable name or UUID if it is an internal service.
     */
    protected String getServiceName() {
        return myApiUrl.substring(API_PREFIX.length());
    }

    @Override
    public boolean isSupported(HttpRequest request) {
        return getMethod() == request.method() && myApiUrl.equals(request.path());
    }

    protected HttpResponse writeResponse(Object responseObject, HttpRequest request) throws IOException {
        String jsonResponse = JsonService.getInstance().toJson(responseObject);

        return HttpResponse.ok("application/json; charset=utf-8", jsonResponse.getBytes(StandardCharsets.UTF_8));
    }

    public String getApiUrl() {
        return myApiUrl;
    }

    protected abstract HttpMethod getMethod();

    protected boolean isHostTrusted(HttpRequest request) {
        if (BuiltInServerManager.getInstance().isRequestSigned(request) || isOriginAllowed(request) == OriginCheckResult.ALLOW) {
            return true;
        }

        String origin = HttpRequestUtil.getOrigin(request);
        String referrer = origin != null ? origin : request.getHeaderValue(REFERER_HEADER_NAME);
        String host;
        String scheme;
        if (StringUtil.isEmptyOrSpaces(referrer)) {
            host = null;
            scheme = null;
        }
        else {
            try {
                URI uri = new URI(referrer);
                host = StringUtil.nullize(uri.getHost(), true);
                scheme = StringUtil.nullize(uri.getScheme(), true);
            }
            catch (URISyntaxException ignored) {
                return false;
            }
        }

        Object lock = myHostLocks.computeIfAbsent(host == null ? "" : host, it -> new Object());
        synchronized (lock) {
            if (host == null || scheme == null) {
                if (myBlockUnknownHosts) {
                    return false;
                }
            }
            else if (isLocalhost(host)) {
                return true;
            }

            Pair<String, String> key = host == null || scheme == null ? null : Pair.create(host, scheme);
            if (key != null) {
                Boolean trusted = getTrustedOrigin(key);
                if (trusted != null) {
                    return trusted;
                }
            }

            Boolean isTrusted = askIsHostTrusted(host, key == null);
            if (isTrusted == null) {
                return false;
            }

            if (key != null) {
                putTrustedOrigin(key, isTrusted);
            }
            return isTrusted;
        }
    }

    public static boolean isHostInPredefinedHosts(HttpRequest request, Set<String> trustedPredefinedHosts, String systemPropertyKey) {
        String origin = HttpRequestUtil.getOrigin(request);
        String originHost;
        if (origin == null) {
            originHost = null;
        }
        else {
            try {
                URI uri = new URI(origin);
                originHost = "https".equals(uri.getScheme()) ? StringUtil.nullize(uri.getHost(), true) : null;
            }
            catch (URISyntaxException ignored) {
                return false;
            }
        }

        String hostName = getHostName(request);
        if (hostName != null && !isLocalhost(hostName)) {
            LOG.error("Expected 'request.hostName' to be localhost. hostName='" + hostName + "', origin='" + origin + "'");
        }

        if (originHost == null) {
            return false;
        }
        return trustedPredefinedHosts.contains(originHost)
            || Arrays.asList(System.getProperty(systemPropertyKey, "").split(",")).contains(originHost)
            || isLocalhost(originHost);
    }

    private @Nullable Boolean askIsHostTrusted(@Nullable String host, boolean isUnknownHost) {
        if (Platform.current().isInBrowser()) {
            return null;
        }

        UIAccess uiAccess = findUIAccess();
        if (uiAccess == null) {
            return null;
        }

        LocalizeValue message = host == null
            ? BuiltInServerLocalize.warningUseRestApi0AndTrustHostUnknown(getServiceName())
            : BuiltInServerLocalize.warningUseRestApi0AndTrustHost1(getServiceName(), host);
        try {
            return showYesNoDialog(uiAccess, message)
                .thenCompose(isTrusted -> {
                    if (isTrusted || !isUnknownHost) {
                        return CompletableFuture.completedFuture(isTrusted);
                    }

                    return showYesNoDialog(uiAccess, BuiltInServerLocalize.warningUseRestApiBlockUnknownHosts())
                        .thenApply(isBlockUnknownHosts -> {
                            myBlockUnknownHosts = isBlockUnknownHosts;
                            return Boolean.FALSE;
                        });
                })
                .get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        catch (CancellationException e) {
            return null;
        }
        catch (ExecutionException e) {
            LOG.error(e);
            return null;
        }
    }

    private @Nullable Boolean getTrustedOrigin(Pair<String, String> key) {
        synchronized (myTrustedOrigins) {
            Pair<Boolean, Long> value = myTrustedOrigins.get(key);
            if (value == null) {
                return null;
            }

            if (System.currentTimeMillis() - value.getSecond() >= TRUSTED_ORIGINS_EXPIRE_AFTER_WRITE) {
                myTrustedOrigins.remove(key);
                return null;
            }
            return value.getFirst();
        }
    }

    private void putTrustedOrigin(Pair<String, String> key, boolean isTrusted) {
        synchronized (myTrustedOrigins) {
            myTrustedOrigins.put(key, Pair.create(isTrusted, System.currentTimeMillis()));
        }
    }

    private static CompletableFuture<Boolean> showYesNoDialog(UIAccess uiAccess, LocalizeValue message) {
        return uiAccess.giveAsync(() -> MessageBoxes.yesNo()
                .asWarning()
                .title(BuiltInServerLocalize.titleUseRestApi())
                .text(message)
                .showAsync())
            .thenCompose(Function.identity())
            .thenApply(Boolean.TRUE::equals);
    }

    private static @Nullable UIAccess findUIAccess() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }

            UIAccess uiAccess = project.getUIAccess();
            if (uiAccess.isValid()) {
                return uiAccess;
            }
        }
        return null;
    }

    private static @Nullable String getHostName(HttpRequest request) {
        String hostAndPort = StringUtil.nullize(HttpRequestUtil.getHost(request), true);
        if (hostAndPort == null) {
            return null;
        }

        int portIndex = hostAndPort.lastIndexOf(':');
        return portIndex > 0 ? StringUtil.nullize(hostAndPort.substring(0, portIndex), true) : hostAndPort;
    }

    private static boolean isLocalhost(String hostName) {
        return hostName.equalsIgnoreCase("localhost") || hostName.equals("127.0.0.1") || hostName.equals("::1");
    }
}
