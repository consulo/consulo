// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list.search;

public interface ReviewListQuickFilter<S extends ReviewListSearchValue> {
    S getFilter();
}
