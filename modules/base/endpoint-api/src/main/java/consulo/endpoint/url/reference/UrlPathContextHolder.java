// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.reference;

import org.jspecify.annotations.Nullable;

public interface UrlPathContextHolder {
    @Nullable UrlPathContext getUrlPathContext();
}
