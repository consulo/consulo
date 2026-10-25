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
package consulo.fileChooser.internal;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The file system of one machine as a tab of the chooser. A remote machine is reached when the tab asks for its
 * roots, so opening the chooser connects to it; the roots are asked for on the UI thread, where connecting starts.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public final class PlatformFileChooserContributor implements UniversalFileChooserContributor {
    private final LocalizeValue myTitle;
    private final @Nullable Image myIcon;
    private final Supplier<@Nullable Platform> myReachablePlatform;
    private final Supplier<CompletableFuture<Platform>> myConnect;

    public PlatformFileChooserContributor(LocalizeValue title,
                                          @Nullable Image icon,
                                          Supplier<@Nullable Platform> reachablePlatform,
                                          Supplier<CompletableFuture<Platform>> connect) {
        myTitle = title;
        myIcon = icon;
        myReachablePlatform = reachablePlatform;
        myConnect = connect;
    }

    @Override
    public LocalizeValue getTabTitle() {
        return myTitle;
    }

    @Override
    public @Nullable Image getTabIcon() {
        return myIcon;
    }

    @Override
    public CompletableFuture<List<Root>> getRoots() {
        return myConnect.get().thenApplyAsync(platform -> {
            List<Root> roots = new ArrayList<>();
            for (Path root : platform.fs().getRootDirectories()) {
                roots.add(UniversalFileChooserContributor.asDefaultRoot(root));
            }
            return roots;
        }, AppExecutorUtil.getAppExecutorService());
    }

    @Override
    public boolean ownsPath(Path path) {
        Platform platform = myReachablePlatform.get();
        return platform != null && platform.user().homePath().getFileSystem() == path.getFileSystem();
    }

    @Override
    public @Nullable Path parsePresentablePath(String text) {
        Platform platform = myReachablePlatform.get();
        if (platform == null) {
            return null;
        }
        try {
            return platform.fs().getPath(text);
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public @Nullable Platform getPlatform() {
        return myReachablePlatform.get();
    }
}
