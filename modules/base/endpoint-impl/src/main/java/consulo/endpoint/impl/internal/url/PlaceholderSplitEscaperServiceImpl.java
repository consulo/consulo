// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url;

import consulo.annotation.component.ServiceImpl;
import consulo.endpoint.internal.PlaceholderSplitEscaperService;
import consulo.endpoint.url.UrlSpecialSegmentMarker;
import consulo.language.psi.util.SplitEscaper;
import jakarta.inject.Singleton;

import java.util.List;

@ServiceImpl
@Singleton
public final class PlaceholderSplitEscaperServiceImpl implements PlaceholderSplitEscaperService {
    @Override
    public SplitEscaper create(List<UrlSpecialSegmentMarker> braces, CharSequence input, String pattern) {
        return new PlaceholderSplitEscaperImpl(braces, input, pattern);
    }
}
