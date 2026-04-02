// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.util;

import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderValueParser;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LinkHttpHeaderValue {
    public static final @Nonnull String HEADER_NAME = "Link";

    private final @Nullable String firstLink;
    private final @Nullable String prevLink;
    private final @Nullable String nextLink;
    private final @Nullable String lastLink;
    private final boolean isDeprecated;

    public LinkHttpHeaderValue(
        @Nullable String firstLink,
        @Nullable String prevLink,
        @Nullable String nextLink,
        @Nullable String lastLink,
        boolean isDeprecated
    ) {
        this.firstLink = firstLink;
        this.prevLink = prevLink;
        this.nextLink = nextLink;
        this.lastLink = lastLink;
        this.isDeprecated = isDeprecated;
    }

    public LinkHttpHeaderValue() {
        this(null, null, null, null, false);
    }

    public @Nullable String getFirstLink() {
        return firstLink;
    }

    public @Nullable String getPrevLink() {
        return prevLink;
    }

    public @Nullable String getNextLink() {
        return nextLink;
    }

    public @Nullable String getLastLink() {
        return lastLink;
    }

    public boolean isDeprecated() {
        return isDeprecated;
    }

    @Nonnull
    public static LinkHttpHeaderValue parse(@Nonnull String linkHeaderValue) {
        HeaderElement[] elements = BasicHeaderValueParser.parseElements(linkHeaderValue, null);
        Map<String, String> headerElements = new HashMap<>();

        for (HeaderElement element : elements) {
            NameValuePair relParam = null;
            for (NameValuePair param : element.getParameters()) {
                if ("rel".equals(param.getName())) {
                    relParam = param;
                    break;
                }
            }
            if (relParam == null) {
                throw new IllegalStateException("Missing rel-param in: '" + linkHeaderValue + "'");
            }

            String name = element.getName();
            if (name == null) {
                continue;
            }

            String urlPart = element.getValue() != null ? name + "=" + element.getValue() : name;
            if (!urlPart.startsWith("<") || !urlPart.endsWith(">")) {
                throw new IllegalStateException("Invalid URL-part '" + urlPart + "' in: '" + linkHeaderValue + "'");
            }
            String url = urlPart.substring(1, urlPart.length() - 1);

            headerElements.put(relParam.getValue(), url);
        }

        return new LinkHttpHeaderValue(
            headerElements.get("first"),
            headerElements.get("prev"),
            headerElements.get("next"),
            headerElements.get("last"),
            headerElements.containsKey("deprecation")
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LinkHttpHeaderValue that)) {
            return false;
        }
        return isDeprecated == that.isDeprecated
            && Objects.equals(firstLink, that.firstLink)
            && Objects.equals(prevLink, that.prevLink)
            && Objects.equals(nextLink, that.nextLink)
            && Objects.equals(lastLink, that.lastLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstLink, prevLink, nextLink, lastLink, isDeprecated);
    }

    @Override
    public @Nonnull String toString() {
        return "LinkHttpHeaderValue(firstLink=" + firstLink + ", prevLink=" + prevLink +
            ", nextLink=" + nextLink + ", lastLink=" + lastLink + ", isDeprecated=" + isDeprecated + ")";
    }
}
