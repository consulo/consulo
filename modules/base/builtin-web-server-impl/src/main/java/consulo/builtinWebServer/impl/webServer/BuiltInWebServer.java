// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer;

import com.google.common.net.InetAddresses;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.http.util.HttpRequestUtil;
import consulo.builtinWebServer.impl.BuiltInServerOptions;
import consulo.builtinWebServer.impl.BuiltInWebServerAuth;
import consulo.builtinWebServer.impl.BuiltInWebServerKt;
import consulo.builtinWebServer.webServer.WebServerPathHandler;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.io.FileUtil;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Map;

@ExtensionImpl(id = "builtInWebServer", order = "last")
public final class BuiltInWebServer extends HttpRequestHandler {
    private static final Logger LOG = Logger.getInstance(BuiltInWebServer.class);

    private final Application myApplication;
    private final BuiltInServerOptions myOptions;
    private final BuiltInServerManager myServerManager;
    private final BuiltInWebServerAuth myAuth;

    @Inject
    public BuiltInWebServer(Application application,
                            BuiltInServerOptions options,
                            BuiltInServerManager serverManager,
                            BuiltInWebServerAuth auth) {
        myApplication = application;
        myOptions = options;
        myServerManager = serverManager;
        myAuth = auth;
    }

    @Override
    public boolean isSupported(HttpRequest request) {
        if (!super.isSupported(request)) {
            return false;
        }

        int localPort = request.localPort();
        return localPort == myServerManager.getPort() || localPort == myOptions.getEffectiveBuiltInServerPort();
    }

    @Override
    public boolean isAccessible(HttpRequest request) {
        return myOptions.isBuiltInServerAvailableExternally()
            || (HttpRequestUtil.parseAndCheckIsLocalHost(request.getHeaderValue("Origin"), false, true)
            && HttpRequestUtil.parseAndCheckIsLocalHost(request.getHeaderValue("Referer"), false, true));
    }

    @Override
    public @Nullable HttpResponse process(HttpRequest request) {
        String hostName = getHostName(request);
        if (hostName == null) {
            return null;
        }

        boolean isIpv6 = hostName.startsWith("[") && hostName.endsWith("]");
        String host = isIpv6 ? hostName.substring(1, hostName.length() - 1) : hostName;

        String projectNameAsHost;
        if (isIpv6 || InetAddresses.isInetAddress(host) || BuiltInWebServerKt.isOwnHostName(host) || host.endsWith(".ngrok.io")) {
            projectNameAsHost = null;
        }
        else if (host.endsWith(".localhost")) {
            projectNameAsHost = host.substring(0, host.lastIndexOf('.'));
        }
        else {
            projectNameAsHost = host;
        }

        return processPath(request, projectNameAsHost, false);
    }

    public @Nullable HttpResponse processPath(HttpRequest request, @Nullable String projectNameAsHost, boolean preAuthorized) {
        String decodedPath = request.path();
        boolean isCustomHost = projectNameAsHost != null;
        if (!isCustomHost && (decodedPath.length() < 2 || decodedPath.charAt(0) != '/')) {
            return null;
        }

        int offset = isCustomHost ? 0 : decodedPath.indexOf('/', 1);
        String projectName = projectNameAsHost != null
            ? projectNameAsHost
            : decodedPath.substring(1, offset == -1 ? decodedPath.length() : offset);
        boolean isEmptyPath = isCustomHost ? decodedPath.isEmpty() : offset == -1;

        String projectNameFromReferer = !isCustomHost && !preAuthorized ? getProjectNameFromReferer(request) : null;

        boolean isFileSystemCaseSensitive = Platform.current().fs().isCaseSensitive();
        Project project = null;
        Project candidateByDirectoryName = null;
        boolean isCandidateFromReferer = false;
        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            if (openProject.isDisposed()) {
                continue;
            }

            String name = openProject.getName();
            if (isCustomHost) {
                if (projectName.equalsIgnoreCase(name)) {
                    if (!isFileSystemCaseSensitive) {
                        projectName = name;
                    }
                    project = openProject;
                    break;
                }
            }
            else if (decodedPath.regionMatches(!isFileSystemCaseSensitive, 1, name, 0, name.length())) {
                boolean isEmptyPathCandidate = decodedPath.length() == name.length() + 1;
                if (isEmptyPathCandidate || decodedPath.charAt(name.length() + 1) == '/') {
                    projectName = name;
                    offset = name.length() + 1;
                    isEmptyPath = isEmptyPathCandidate;
                    project = openProject;
                    break;
                }
            }

            if (candidateByDirectoryName == null && BuiltInWebServerKt.compareNameAndProjectBasePath(projectName, openProject)) {
                candidateByDirectoryName = openProject;
            }
            if (candidateByDirectoryName == null
                && projectNameFromReferer != null
                && isRefererCandidate(projectNameFromReferer, openProject)) {
                candidateByDirectoryName = openProject;
                isCandidateFromReferer = true;
            }
        }

        if (project == null) {
            project = candidateByDirectoryName;
            if (isCandidateFromReferer && projectNameFromReferer != null) {
                projectName = projectNameFromReferer;
                offset = 0;
                isEmptyPath = false;
            }
        }

        Map<String, String> authHeaders = preAuthorized ? Map.of() : myAuth.validateToken(request);
        if (authHeaders == null) {
            return null;
        }

        if (project == null) {
            return null;
        }

        if (isEmptyPath) {
            return withHeaders(BuiltInWebServerKt.redirectToDirectory(request), authHeaders);
        }

        if ("script".equals(request.getHeaderValue("Service-Worker"))) {
            return null;
        }

        String rest = decodedPath.substring(offset);
        if (!rest.startsWith("/")) {
            return withHeaders(HttpResponse.notFound(), authHeaders);
        }

        String path = FileUtil.toCanonicalPath(rest).substring(1);
        for (WebServerPathHandler pathHandler : myApplication.getExtensionPoint(WebServerPathHandler.class).getExtensionList()) {
            try {
                HttpResponse response = pathHandler.process(path, project, request, projectName, isCustomHost);
                if (response != null) {
                    return withHeaders(response, authHeaders);
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }

        return withHeaders(HttpResponse.notFound(), authHeaders);
    }

    private static @Nullable String getHostName(HttpRequest request) {
        String hostAndPort = request.getHeaderValue("Host");
        if (hostAndPort == null || hostAndPort.isBlank()) {
            return null;
        }

        int portIndex = hostAndPort.lastIndexOf(':');
        if (portIndex > 0 && hostAndPort.indexOf(']', portIndex) < 0) {
            String hostName = hostAndPort.substring(0, portIndex).trim();
            return hostName.isEmpty() ? null : hostName;
        }
        return hostAndPort;
    }

    private static @Nullable String getProjectNameFromReferer(HttpRequest request) {
        String referer = request.getHeaderValue("Referer");
        if (referer == null) {
            return null;
        }

        try {
            String refererPath = URI.create(referer).getPath();
            if (refererPath != null && refererPath.startsWith("/")) {
                int secondSlashOffset = refererPath.indexOf('/', 1);
                if (secondSlashOffset > 1) {
                    return refererPath.substring(1, secondSlashOffset);
                }
            }
            return null;
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isRefererCandidate(String projectNameFromReferer, Project project) {
        return projectNameFromReferer.equals(project.getName())
            || BuiltInWebServerKt.compareNameAndProjectBasePath(projectNameFromReferer, project);
    }

    private static HttpResponse withHeaders(HttpResponse response, Map<String, String> headers) {
        HttpResponse result = response;
        for (Map.Entry<String, String> header : headers.entrySet()) {
            result = result.withHeader(header.getKey(), header.getValue());
        }
        return result;
    }
}
