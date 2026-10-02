// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.internal;

import consulo.endpoint.EndpointModuleEntity;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

public final class DefaultEndpointModule implements EndpointModuleEntity {
    private final Module myModule;

    public DefaultEndpointModule(Module module) {
        myModule = module;
    }

    public Module getModule() {
        return myModule;
    }

    @Override
    public String getName() {
        return myModule.getName();
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesModule();
    }
}
