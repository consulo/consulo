// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer;

import consulo.annotation.component.ExtensionImpl;
import consulo.builtinWebServer.http.FileResponses;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.impl.webServer.liveReload.WebServerPageConnectionService;
import consulo.builtinWebServer.webServer.PathInfo;
import consulo.builtinWebServer.webServer.WebServerFileHandler;
import consulo.http.HttpMethod;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@ExtensionImpl(order = "last")
public final class StaticFileHandler extends WebServerFileHandler {
    private static final List<String> PAGE_FILE_EXTENSIONS = List.of("html", "htm", "shtml", "stm", "shtm");

    private final WebServerPageConnectionService myPageConnectionService;

    @Inject
    public StaticFileHandler(WebServerPageConnectionService pageConnectionService) {
        myPageConnectionService = pageConnectionService;
    }

    @Override
    public List<String> getPageFileExtensions() {
        return PAGE_FILE_EXTENSIONS;
    }

    @Override
    public @Nullable HttpResponse process(PathInfo pathInfo,
                                          CharSequence canonicalPath,
                                          Project project,
                                          HttpRequest request,
                                          @Nullable String projectNameIfNotCustomHost) throws IOException {
        Path ioFile = pathInfo.getIoFile();
        VirtualFile pathFile = pathInfo.getFile();
        if (ioFile != null || Objects.requireNonNull(pathFile).isInLocalFileSystem()) {
            Path localFile = ioFile != null ? ioFile : Objects.requireNonNull(pathFile).toNioPath();

            String extraSuffix = myPageConnectionService.fileRequested(request, true, pathInfo::getOrResolveVirtualFile);
            byte @Nullable [] extraBuffer = extraSuffix == null
                ? null
                : extraSuffix.getBytes(pathFile != null ? pathFile.getCharset() : StandardCharsets.UTF_8);
            return FileResponses.sendFile(request, localFile, extraBuffer);
        }

        VirtualFile file = Objects.requireNonNull(pathFile);
        byte @Nullable [] content = request.method() == HttpMethod.HEAD ? null : file.contentsToByteArray();
        return FileResponses.prepareSend(request, file.getTimeStamp(), file.getName(), content);
    }
}
