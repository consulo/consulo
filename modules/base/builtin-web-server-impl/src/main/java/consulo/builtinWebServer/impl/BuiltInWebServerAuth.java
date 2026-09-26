// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.container.boot.ContainerPathManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.lazy.LazyValue;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class BuiltInWebServerAuth {
    private static final String COOKIE_HEADER_NAME = "Cookie";
    private static final String SET_COOKIE_HEADER_NAME = "Set-Cookie";
    private static final String REFERER_HEADER_NAME = "Referer";

    private static final Object TOKEN_KEY = new Object();

    private final BuiltInServerOptions myOptions;

    private final LazyValue<Cookie> myStandardCookie = LazyValue.atomicNotNull(BuiltInWebServerAuth::createStandardCookie);

    private final LoadingCache<Object, String> myTokens =
        CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build(CacheLoader.from(TokenGenerator::generate));

    @Inject
    public BuiltInWebServerAuth(BuiltInServerOptions options) {
        myOptions = options;
    }

    private static Cookie createStandardCookie() {
        String token = UUID.randomUUID().toString();
        String settingsDirHash = Integer.toHexString(ContainerPathManager.get().getConfigPath().hashCode());
        DefaultCookie cookie = new DefaultCookie("consulo-" + settingsDirHash, token);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(TimeUnit.DAYS.toSeconds(365 * 10));
        cookie.setPath("/");
        return cookie;
    }

    public String acquireToken() {
        return myTokens.getUnchecked(TOKEN_KEY);
    }

    public @Nullable Map<String, String> validateToken(HttpRequest request) {
        if (myOptions.isAllowUnsignedRequests()) {
            return Map.of();
        }

        String cookieHeader = request.getHeaderValue(COOKIE_HEADER_NAME);
        if (cookieHeader != null) {
            Cookie standardCookie = myStandardCookie.get();
            for (Cookie cookie : ServerCookieDecoder.STRICT.decode(cookieHeader)) {
                if (cookie.name().equals(standardCookie.name())) {
                    if (cookie.value().equals(standardCookie.value())) {
                        return Map.of();
                    }
                    break;
                }
            }
        }

        if (isRequestSigned(request)) {
            return Map.of(SET_COOKIE_HEADER_NAME, ServerCookieEncoder.STRICT.encode(myStandardCookie.get()) + "; SameSite=strict");
        }

        return null;
    }

    public boolean isRequestSigned(HttpRequest request) {
        if (myOptions.isAllowUnsignedRequests()) {
            return true;
        }

        String token = request.getHeaderValue(BuiltInWebServerKt.TOKEN_HEADER_NAME);
        if (token == null) {
            token = request.getParameterValue(BuiltInWebServerKt.TOKEN_PARAM_NAME);
        }
        if (token == null) {
            String referrer = request.getHeaderValue(REFERER_HEADER_NAME);
            if (referrer != null) {
                token = ContainerUtil.getFirstItem(new QueryStringDecoder(referrer).parameters().get(BuiltInWebServerKt.TOKEN_PARAM_NAME));
            }
        }

        if (token == null) {
            return false;
        }

        String expected = myTokens.getIfPresent(TOKEN_KEY);
        return expected != null
            && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
