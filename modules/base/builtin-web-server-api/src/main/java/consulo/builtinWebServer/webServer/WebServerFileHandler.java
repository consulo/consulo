// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.webServer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class WebServerFileHandler {
    public List<String> getPageFileExtensions() {
        return List.of();
    }

    /**
     * {@code canonicalPath} contains index file name (if not specified in the request)
     */
    public abstract @Nullable HttpResponse process(PathInfo pathInfo,
                                                   CharSequence canonicalPath,
                                                   Project project,
                                                   HttpRequest request,
                                                   @Nullable String projectNameIfNotCustomHost) throws IOException;

    protected String getRequestPath(CharSequence canonicalPath, @Nullable String projectNameIfNotCustomHost) {
        if (projectNameIfNotCustomHost == null) {
            return "/" + canonicalPath;
        }
        return "/" + projectNameIfNotCustomHost + "/" + canonicalPath;
    }
}
