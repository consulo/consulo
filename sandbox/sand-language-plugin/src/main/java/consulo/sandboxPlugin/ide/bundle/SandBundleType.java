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
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

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
        super("SAND_BUNDLE", LocalizeValue.localizeTODO("Sand Bundle"), PlatformIconGroup.nodesStatic());
    }

    @Override
    public boolean canCreatePredefinedSdks(Platform platform) {
        return true;
    }

    
    @Override
    public Set<String> getEnviromentVariables(Platform platform) {
        return Set.of("TEMP");
    }

    @Override
    public void collectHomePaths(Platform platform, Consumer<Path> pathConsumer) {
    }

    @Nullable
    @Override
    public String getVersionString(Platform platform, Path path) {
        return "1";
    }

    @Override
    public boolean isRootTypeApplicable(OrderRootType type) {
        return type == BinariesOrderRootType.getInstance();
    }
}
