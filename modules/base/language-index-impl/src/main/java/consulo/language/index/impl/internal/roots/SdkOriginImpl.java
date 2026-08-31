// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.content.bundle.Sdk;
import consulo.language.index.impl.internal.roots.kind.SdkOrigin;

class SdkOriginImpl implements SdkOrigin {
    private final Sdk mySdk;

    SdkOriginImpl(Sdk sdk) {
        mySdk = sdk;
    }

    @Override
    public Sdk getSdk() {
        return mySdk;
    }
}
