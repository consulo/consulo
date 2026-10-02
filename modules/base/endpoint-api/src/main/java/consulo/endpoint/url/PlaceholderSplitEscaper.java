// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url;

import consulo.endpoint.internal.PlaceholderSplitEscaperService;
import consulo.language.psi.util.SplitEscaper;

import java.util.List;

public final class PlaceholderSplitEscaper {
    private PlaceholderSplitEscaper() {
    }

    public static SplitEscaper create(String begin, String end, CharSequence input, String pattern) {
        return create(List.of(new UrlSpecialSegmentMarker(begin, end)), input, pattern);
    }

    public static SplitEscaper create(List<UrlSpecialSegmentMarker> braces, CharSequence input, String pattern) {
        return PlaceholderSplitEscaperService.getInstance().create(braces, input, pattern);
    }
}
