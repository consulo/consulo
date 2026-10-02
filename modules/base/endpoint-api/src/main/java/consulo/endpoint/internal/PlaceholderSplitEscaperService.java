// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.endpoint.url.UrlSpecialSegmentMarker;
import consulo.language.psi.util.SplitEscaper;

import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public interface PlaceholderSplitEscaperService {
    static PlaceholderSplitEscaperService getInstance() {
        return Application.get().getInstance(PlaceholderSplitEscaperService.class);
    }

    SplitEscaper create(List<UrlSpecialSegmentMarker> braces, CharSequence input, String pattern);
}
