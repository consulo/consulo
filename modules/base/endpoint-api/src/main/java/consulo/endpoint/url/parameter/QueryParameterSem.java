// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.endpoint.url.reference.UrlPathContext;
import consulo.language.sem.SemKey;

public interface QueryParameterSem extends RenameableSemElement {
    SemKey<QueryParameterSem> QUERY_PARAMETER_SEM_KEY = SemKey.createKey("QueryParameter", RenameableSemElement.RENAMEABLE_SEM_KEY);

    @Override
    String getName();

    UrlPathContext getUrlPathContext();
}
