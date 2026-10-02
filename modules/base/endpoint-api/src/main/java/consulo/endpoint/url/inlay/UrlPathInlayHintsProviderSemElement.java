// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.inlay;

import consulo.language.sem.SemElement;
import consulo.language.sem.SemKey;

import java.util.List;

public interface UrlPathInlayHintsProviderSemElement extends SemElement {
    SemKey<UrlPathInlayHintsProviderSemElement> INLAY_HINT_SEM_KEY = SemKey.createKey("UrlInlayHintSemProvider");

    List<UrlPathInlayHint> getInlayHints();

    default ProviderGroupInfo getGroupInfo() {
        return ProviderGroupInfo.DEFAULT_PROVIDER_GROUP_INFO;
    }
}
