/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.bundle;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.ExtensionInstance;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.BundleType;
import consulo.content.bundle.SdkType;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2014-05-12
 */
@ExtensionImpl
public class SandBundleType extends BundleType {
    public static final Supplier<SandBundleType> INSTANCE = ExtensionInstance.from(SdkType.class);

    public SandBundleType() {
        super("SAND_BUNDLE");
    }

    @Override
    public boolean canCreatePredefinedSdks(@Nonnull Platform platform) {
        return true;
    }

    @Nonnull
    @Override
    public Set<String> getEnviromentVariables(@Nonnull Platform platform) {
        return Set.of("TEMP");
    }

    @Override
    public void collectHomePaths(@Nonnull Platform platform, @Nonnull Consumer<Path> pathConsumer) {
    }

    @Nullable
    @Override
    public String getVersionString(@Nonnull Platform platform, @Nonnull Path path) {
        return "1";
    }

    @Override
    public boolean isRootTypeApplicable(OrderRootType type) {
        return type == BinariesOrderRootType.getInstance();
    }

    @Nonnull
    @Override
    public String getPresentableName() {
        return "Sand Bundle";
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesStatic();
    }
}
