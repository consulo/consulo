/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.application.content.impl.internal.platform;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.content.platform.PlatformTarget;
import consulo.content.platform.PlatformTargetManager;
import consulo.content.platform.PlatformTargetProvider;
import consulo.platform.Platform;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
@ServiceImpl
@Singleton
public class PlatformTargetManagerImpl implements PlatformTargetManager {
    private final Application myApplication;
    private final PlatformTarget myLocalTarget = new LocalPlatformTarget();

    @Inject
    public PlatformTargetManagerImpl(Application application) {
        myApplication = application;
    }

    @Override
    public List<PlatformTarget> getTargets() {
        List<PlatformTarget> targets = new ArrayList<>();
        targets.add(myLocalTarget);
        myApplication.getExtensionPoint(PlatformTargetProvider.class).forEach(provider -> targets.addAll(provider.getTargets()));
        return targets;
    }

    @Override
    public PlatformTarget getLocalTarget() {
        return myLocalTarget;
    }

    @Override
    public @Nullable PlatformTarget findTarget(@Nullable String id) {
        if (id == null || LocalPlatformTarget.ID.equals(id)) {
            return myLocalTarget;
        }
        for (PlatformTarget target : getTargets()) {
            if (target.getId().equals(id)) {
                return target;
            }
        }
        return null;
    }

    @Override
    public @Nullable PlatformTarget findTargetOf(Path path) {
        for (PlatformTarget target : getTargets()) {
            Platform platform = target.getPlatform();
            if (platform != null && platform.user().homePath().getFileSystem() == path.getFileSystem()) {
                return target;
            }
        }
        return null;
    }
}
