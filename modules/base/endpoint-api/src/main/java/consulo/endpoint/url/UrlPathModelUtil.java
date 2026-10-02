// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import consulo.language.psi.util.PartiallyKnownString;
import consulo.language.psi.util.StringEntry;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

public final class UrlPathModelUtil {
    private UrlPathModelUtil() {
    }

    public static Set<String> getDeclaredHttpMethods(@Nullable String methodString) {
        return methodString != null ? Set.of(methodString.toUpperCase(Locale.ROOT)) : Set.of();
    }

    public static boolean compatibleSchemes(Collection<String> aSchemes, Collection<String> bSchemes) {
        if (aSchemes.isEmpty() || bSchemes.isEmpty()) {
            return true;
        }
        for (String scheme : aSchemes) {
            if (bSchemes.contains(scheme)) {
                return true;
            }
        }
        for (List<String> group : UrlConstants.KNOWN_SCHEMES_GROUPS) {
            if (containsAny(group, aSchemes) && containsAny(group, bSchemes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(List<String> group, Collection<String> schemes) {
        for (String scheme : schemes) {
            if (group.contains(scheme)) {
                return true;
            }
        }
        return false;
    }

    public static Set<UrlTargetInfo> filterBestUrlPathMatches(Iterable<? extends UrlTargetInfo> all) {
        return filterBestUrlPathMatches(all, null);
    }

    public static Set<UrlTargetInfo> filterBestUrlPathMatches(Iterable<? extends UrlTargetInfo> all, @Nullable UrlPath original) {
        return new MultiPathBestMatcher().addBestMatching(all, original).getResult();
    }

    public static String getEndpointUrlPresentation(PartiallyKnownString path) {
        StringJoiner joiner = new StringJoiner(", ");
        for (StringEntry segment : path.getSegments()) {
            if (segment instanceof StringEntry.Known known) {
                joiner.add(known.getValue());
            }
            else {
                joiner.add(UrlPath.UNKNOWN_URL_PATH_SEGMENT_PRESENTATION);
            }
        }
        return joiner.toString();
    }
}
