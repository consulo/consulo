// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.util;

import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public final class DelegatePsiTargetSimpleNamePomTargetPresentationProvider
    extends DelegateSimpleNamePomTargetPresentationProviderBase<DelegatePsiTargetSimpleNamePomTarget> {
    @Override
    public Class<DelegatePsiTargetSimpleNamePomTarget> getItemClass() {
        return DelegatePsiTargetSimpleNamePomTarget.class;
    }
}
