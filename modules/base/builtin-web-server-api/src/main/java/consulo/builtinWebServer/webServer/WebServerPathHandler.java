// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.webServer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Path handlers help the built-in web server serve requests composed by {@code WebBrowserService.getUrlsToOpen}.
 * <p>
 * By default, {@link WebServerPathToFileManager} will be used to map the request to a file.
 * If a file physically exists in the file system, you must use {@link WebServerRootsProvider}.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface WebServerPathHandler {
    /**
     * Processes the given path request for the specified project
     * (e.g., {@code http://localhost:63342/<project>/<path>} or {@code http://<project>.localhost:63342/<path>}).
     *
     * @param path         the path of the request; does not include the project name
     * @param project      the project associated with the request
     * @param request      the request
     * @param projectName  the name of the project
     * @param isCustomHost {@code false} when a project name is a part of the request path ({@code /project/path}), {@code true} otherwise
     * @return the response, or {@code null} if the request is not handled
     */
    @Nullable HttpResponse process(String path, Project project, HttpRequest request, String projectName, boolean isCustomHost)
        throws IOException;
}
