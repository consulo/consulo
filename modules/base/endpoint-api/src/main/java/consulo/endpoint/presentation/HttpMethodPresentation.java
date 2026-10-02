// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.presentation;

import consulo.colorScheme.TextAttributesKey;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public final class HttpMethodPresentation extends PresentationData implements EndpointMethodPresentation {
    private final int myEndpointMethodOrder;
    private final @Nullable String myEndpointMethodPresentation;
    private final List<String> myEndpointMethods;

    public HttpMethodPresentation(
        @Nullable String httpUrl,
        @Nullable String httpMethod,
        @Nullable String definitionSource,
        @Nullable Image icon
    ) {
        this(httpUrl, httpMethod, definitionSource, icon, null);
    }

    public HttpMethodPresentation(
        @Nullable String httpUrl,
        @Nullable String httpMethod,
        @Nullable String definitionSource,
        @Nullable Image icon,
        @Nullable TextAttributesKey textAttributesKey
    ) {
        super(httpUrl, definitionSource, icon, textAttributesKey);
        String methodName = httpMethod != null ? httpMethod.toUpperCase(Locale.ROOT) : null;

        myEndpointMethodOrder = getHttpMethodOrder(methodName);
        myEndpointMethodPresentation = getHttpMethodPresentation(httpMethod);
        myEndpointMethods = methodName != null ? List.of(methodName) : List.of();
    }

    public HttpMethodPresentation(
        @Nullable String httpUrl,
        List<String> httpMethods,
        @Nullable String definitionSource,
        @Nullable Image icon
    ) {
        this(httpUrl, httpMethods, definitionSource, icon, null);
    }

    public HttpMethodPresentation(
        @Nullable String httpUrl,
        List<String> httpMethods,
        @Nullable String definitionSource,
        @Nullable Image icon,
        @Nullable TextAttributesKey textAttributesKey
    ) {
        super(httpUrl, definitionSource, icon, textAttributesKey);
        List<String> methods = new ArrayList<>(httpMethods.size());
        for (String method : httpMethods) {
            methods.add(method.toUpperCase(Locale.ROOT));
        }

        myEndpointMethodOrder = getHttpMethodOrder(String.join(" ", methods));
        myEndpointMethodPresentation = getHttpMethodsPresentation(httpMethods);
        myEndpointMethods = methods;
    }

    @Override
    public int getEndpointMethodOrder() {
        return myEndpointMethodOrder;
    }

    @Override
    public @Nullable String getEndpointMethodPresentation() {
        return myEndpointMethodPresentation;
    }

    @Override
    public List<String> getEndpointMethods() {
        return myEndpointMethods;
    }

    public static String getHttpMethodPresentation(@Nullable String httpMethod) {
        return httpMethod != null ? "[" + httpMethod.toUpperCase(Locale.ROOT) + "]" : "";
    }

    public static String getHttpMethodsPresentation(Collection<String> httpMethods) {
        if (httpMethods.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("|", "[", "]");
        for (String method : httpMethods) {
            joiner.add(method.toUpperCase(Locale.ROOT));
        }
        return joiner.toString();
    }

    public static int getHttpMethodOrder(@Nullable String method) {
        if (method == null) {
            return 100;
        }

        return switch (method) {
            case "HEAD" -> 0;
            case "GET" -> 1;
            case "PUT" -> 2;
            case "POST" -> 3;
            case "PATCH" -> 4;
            case "DELETE" -> 5;
            case "OPTIONS" -> 6;
            case "TRACE" -> 7;
            default -> 100;
        };
    }
}
