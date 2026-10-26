// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileChooser.UniversalFileChooserContributor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class LocalFileChooserContributor implements UniversalFileChooserContributor {
    @Override
    public LocalizeValue getTabTitle() {
        return LocalizeValue.localizeTODO("Local");
    }

    @Override
    public Image getTabIcon() {
        return PlatformIconGroup.nodesHomefolder();
    }

    @Override
    public CompletableFuture<List<Root>> getRoots() {
        return CompletableFuture.completedFuture(UniversalFileChooserContributor.getFilteredSystemRoots(this::ownsPath));
    }

    @Override
    public boolean ownsPath(Path path) {
        return path.getFileSystem() == FileSystems.getDefault();
    }

    @Override
    public Platform getPlatform() {
        return Platform.current();
    }
}
