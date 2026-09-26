// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.impl.BuiltInServerOptions;
import consulo.builtinWebServer.impl.BuiltInWebServerAuth;
import consulo.builtinWebServer.impl.BuiltInWebServerKt;
import consulo.builtinWebServer.impl.webServer.liveReload.WebServerPageConnectionService;
import consulo.builtinWebServer.webServer.PathInfo;
import consulo.builtinWebServer.webServer.WebServerPathToFileManager;
import consulo.language.psi.PsiFile;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.Url;
import consulo.util.io.Urls;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.webBrowser.OpenInBrowserRequest;
import consulo.webBrowser.ReloadMode;
import consulo.webBrowser.WebBrowserManager;
import consulo.webBrowser.WebBrowserUrlProvider;
import consulo.webBrowser.WebFileFilter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@ExtensionImpl(order = "last")
public class BuiltInWebBrowserUrlProvider extends WebBrowserUrlProvider implements DumbAware {
    @Override
    public boolean canHandleElement(OpenInBrowserRequest request) {
        boolean isSuitableFile = request.getVirtualFile() instanceof HttpVirtualFile
            || (request.isPhysicalFile() && isFileOfMyLanguage(request.getFile()));
        return isSuitableFile && super.canHandleElement(request);
    }

    protected boolean isFileOfMyLanguage(PsiFile psiFile) {
        return WebFileFilter.isFileAllowed(psiFile);
    }

    @Override
    protected @Nullable Url getUrl(OpenInBrowserRequest request, VirtualFile file) {
        if (file instanceof HttpVirtualFile) {
            return VirtualFileUtil.newFromVirtualFile(file);
        }

        boolean appendAccessToken = request.isAppendAccessToken() && !Platform.current().isInBrowser();
        return ContainerUtil.getFirstItem(
            getBuiltInServerUrls(file, request.getProject(), null, appendAccessToken, request.getReloadMode())
        );
    }

    public static List<Url> getBuiltInServerUrls(VirtualFile file, Project project, @Nullable String currentAuthority) {
        return getBuiltInServerUrls(file, project, currentAuthority, true);
    }

    public static List<Url> getBuiltInServerUrls(VirtualFile file,
                                                 Project project,
                                                 @Nullable String currentAuthority,
                                                 boolean appendAccessToken) {
        return getBuiltInServerUrls(file, project, currentAuthority, appendAccessToken, null);
    }

    public static List<Url> getBuiltInServerUrls(VirtualFile file,
                                                 Project project,
                                                 @Nullable String currentAuthority,
                                                 boolean appendAccessToken,
                                                 @Nullable ReloadMode reloadMode) {
        if (currentAuthority != null && !compareAuthority(currentAuthority)) {
            return List.of();
        }

        PathInfo info = WebServerPathToFileManager.getInstance(project).getPathInfo(file);
        if (info == null) {
            return List.of();
        }

        int effectivePort = BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort();
        String path = info.getPath();
        String authority = currentAuthority != null ? currentAuthority : "localhost:" + effectivePort;
        ReloadMode effectiveReloadMode = reloadMode != null ? reloadMode : WebBrowserManager.getInstance().getWebServerReloadMode();
        boolean appendReloadOnSave = effectiveReloadMode != ReloadMode.DISABLED;
        StringBuilder queryBuilder = new StringBuilder();
        if (appendAccessToken || appendReloadOnSave) {
            queryBuilder.append('?');
        }
        if (appendAccessToken) {
            queryBuilder.append(BuiltInWebServerKt.TOKEN_PARAM_NAME)
                .append('=')
                .append(Application.get().getInstance(BuiltInWebServerAuth.class).acquireToken());
        }
        if (appendAccessToken && appendReloadOnSave) {
            queryBuilder.append('&');
        }
        if (appendReloadOnSave) {
            queryBuilder.append(WebServerPageConnectionService.RELOAD_URL_PARAM).append('=').append(effectiveReloadMode.name());
        }
        String query = queryBuilder.toString();

        List<Url> urls = new ArrayList<>();
        urls.add(newHttpUrl(authority, project, path, query));

        String path2 = info.getRootLessPathIfPossible();
        if (path2 != null) {
            urls.add(newHttpUrl(authority, project, path2, query));
        }

        int defaultPort = BuiltInServerManager.getInstance().getPort();
        if (currentAuthority == null && defaultPort != effectivePort) {
            String defaultAuthority = "localhost:" + defaultPort;
            urls.add(newHttpUrl(defaultAuthority, project, path, query));
            if (path2 != null) {
                urls.add(newHttpUrl(defaultAuthority, project, path2, query));
            }
        }

        return urls;
    }

    public static boolean compareAuthority(@Nullable String currentAuthority) {
        if (currentAuthority == null || currentAuthority.isEmpty()) {
            return false;
        }

        int portIndex = currentAuthority.indexOf(':');
        if (portIndex < 0) {
            return false;
        }

        String host = currentAuthority.substring(0, portIndex);
        if (!BuiltInWebServerKt.isOwnHostName(host)) {
            return false;
        }

        int port = StringUtil.parseInt(currentAuthority.substring(portIndex + 1), -1);
        return port == BuiltInServerOptions.getInstance().getEffectiveBuiltInServerPort()
            || port == BuiltInServerManager.getInstance().getPort();
    }

    private static Url newHttpUrl(String authority, Project project, String path, @Nullable String query) {
        return Urls.newUrl("http", authority, "/" + project.getName() + "/" + path, query);
    }
}
