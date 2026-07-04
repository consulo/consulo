// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkUtil;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public class OrderEntryNavBarItem extends DefaultNavBarItem<OrderEntry> {
    public OrderEntryNavBarItem(OrderEntry data) {
        super(data);
    }

    @Override
    public @Nullable Image getIcon() {
        if (getData() instanceof ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
            Sdk sdk = sdkOrderEntry.getSdk();
            return sdk == null ? null : SdkUtil.getIcon(sdk);
        }
        if (getData() instanceof LibraryOrderEntry) {
            return PlatformIconGroup.nodesPplibfolder();
        }
        if (getData() instanceof ModuleOrderEntry) {
            return PlatformIconGroup.nodesModule();
        }
        return super.getIcon();
    }
}
