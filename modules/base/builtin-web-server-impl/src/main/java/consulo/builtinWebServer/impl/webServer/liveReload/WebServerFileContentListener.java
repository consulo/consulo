// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer.liveReload;

import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

@SuppressWarnings("ExtensionImplIsNotAnnotated")
final class WebServerFileContentListener implements AsyncFileListener {
    private final WebServerPageConnectionService myService;

    WebServerFileContentListener(WebServerPageConnectionService service) {
        myService = service;
    }

    @Override
    public @Nullable ChangeApplier prepareChange(List<? extends VFileEvent> events) {
        List<VirtualFile> files = ContainerUtil.mapNotNull(events, VFileEvent::getFile);
        return myService.reloadRelatedClients(files);
    }
}
