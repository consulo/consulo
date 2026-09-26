// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.impl.BuiltInWebServerKt;
import consulo.builtinWebServer.webServer.PathInfo;
import consulo.builtinWebServer.webServer.PathQuery;
import consulo.builtinWebServer.webServer.WebServerFileHandler;
import consulo.builtinWebServer.webServer.WebServerPathHandler;
import consulo.builtinWebServer.webServer.WebServerPathToFileManager;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.PathUtil;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

@ExtensionImpl(order = "last")
public final class DefaultWebServerPathHandler implements WebServerPathHandler {
    private static final Logger LOG = Logger.getInstance(DefaultWebServerPathHandler.class);

    private static final PathQuery DEFAULT_PATH_QUERY = new PathQuery();

    private final Application myApplication;

    @Inject
    public DefaultWebServerPathHandler(Application application) {
        myApplication = application;
    }

    @Override
    public @Nullable HttpResponse process(String path, Project project, HttpRequest request, String projectName, boolean isCustomHost)
        throws IOException {
        String decodedRawPath = request.path();

        WebServerPathToFileManagerImpl pathToFileManager = (WebServerPathToFileManagerImpl) WebServerPathToFileManager.getInstance(project);
        PathInfo pathInfo = pathToFileManager.getPathToInfoCache().getIfPresent(path);
        if (pathInfo == null || !pathInfo.isValid()) {
            pathInfo = pathToFileManager.doFindByRelativePath(path, DEFAULT_PATH_QUERY);
            if (pathInfo == null) {
                return HttpResponse.notFound();
            }
            pathToFileManager.getPathToInfoCache().put(path, pathInfo);
        }

        boolean indexUsed = false;
        if (pathInfo.isDirectory()) {
            VirtualFile indexVirtualFile = null;
            Path indexFile = null;
            VirtualFile file = pathInfo.getFile();
            if (file == null) {
                indexFile = BuiltInWebServerKt.findIndexFile(Objects.requireNonNull(pathInfo.getIoFile()));
            }
            else {
                indexVirtualFile = BuiltInWebServerKt.findIndexFile(file);
            }

            if (indexFile == null && indexVirtualFile == null) {
                return HttpResponse.notFound();
            }

            if (!decodedRawPath.endsWith("/")) {
                return BuiltInWebServerKt.redirectToDirectory(request);
            }

            indexUsed = true;
            pathInfo = new PathInfo(indexFile, indexVirtualFile, pathInfo.getRoot(), pathInfo.getModuleName(), pathInfo.isLibrary());
            pathToFileManager.getPathToInfoCache().put(path, pathInfo);
        }

        if (!indexUsed && !BuiltInWebServerKt.endsWithName(path, pathInfo.getName())) {
            if (decodedRawPath.endsWith("/")) {
                indexUsed = true;
            }
            else {
                String parentPath = PathUtil.getParentPath(pathInfo.getPath());
                if (!parentPath.isEmpty() && BuiltInWebServerKt.endsWithName(path, PathUtil.getFileName(parentPath))) {
                    return BuiltInWebServerKt.redirectToDirectory(request);
                }
            }
        }

        if (!checkAccess(pathInfo, project)) {
            int code = myApplication.isUnitTestMode() ? HttpURLConnection.HTTP_FORBIDDEN : HttpURLConnection.HTTP_NOT_FOUND;
            return HttpResponse.create(code, null, null);
        }

        CharSequence canonicalPath = indexUsed ? path + "/" + pathInfo.getName() : path;
        String projectNameForHandlers = isCustomHost ? null : projectName;
        for (WebServerFileHandler fileHandler : myApplication.getExtensionPoint(WebServerFileHandler.class).getExtensionList()) {
            try {
                HttpResponse response = fileHandler.process(pathInfo, canonicalPath, project, request, projectNameForHandlers);
                if (response != null) {
                    return response;
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
        return null;
    }

    static boolean checkAccess(PathInfo pathInfo, Project project) {
        Path ioFile = pathInfo.getIoFile();
        if (ioFile != null) {
            return checkAccess(ioFile, pathInfo.getRoot(), project);
        }

        VirtualFile file = Objects.requireNonNull(pathInfo.getFile());
        if (file.isInLocalFileSystem()) {
            return checkAccess(file.toNioPath(), pathInfo.getRoot(), project);
        }
        return !file.is(VFileProperty.HIDDEN);
    }

    private static boolean checkAccess(Path file, VirtualFile root, Project project) {
        try {
            Path realFile = file.toRealPath();
            Path matchedRoot = null;
            if (root.isInLocalFileSystem()) {
                Path realRoot = root.toNioPath().toRealPath();
                if (realFile.startsWith(realRoot)) {
                    matchedRoot = realRoot;
                }
            }

            String basePath = project.getBasePath();
            if (matchedRoot == null && basePath != null) {
                Path realBase = Path.of(basePath).toRealPath();
                if (realFile.startsWith(realBase)) {
                    matchedRoot = realBase;
                }
            }

            if (matchedRoot == null) {
                return false;
            }

            for (Path current = realFile; current != null && !current.equals(matchedRoot); current = current.getParent()) {
                if (!canAccess(current)) {
                    return false;
                }
            }
            return true;
        }
        catch (IOException | InvalidPathException e) {
            return false;
        }
    }

    private static boolean canAccess(Path path) throws IOException {
        Path fileName = path.getFileName();
        return fileName != null && Files.isReadable(path) && !(Files.isHidden(path) || fileName.toString().startsWith("."));
    }
}
