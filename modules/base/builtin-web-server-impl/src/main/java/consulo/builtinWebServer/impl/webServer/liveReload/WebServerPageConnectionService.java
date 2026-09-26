// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer.liveReload;

import com.google.common.net.HttpHeaders;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.builtinWebServer.http.FileResponses;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.webSocket.WebSocketConnection;
import consulo.codeEditor.EditorFactory;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.logging.Logger;
import consulo.util.lang.StringEscapeUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.webBrowser.ReloadMode;
import io.netty.handler.codec.http.QueryStringDecoder;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Provides support for automatic reloading of pages opened on built-in web server on related files modification.
 * <p>
 * Implementation:
 * <p>
 * &lt;-- html page with {@link #RELOAD_URL_PARAM} is requested<br>
 * --&gt; response with modified html which opens WebSocket connection listening for reload message
 * <p>
 * &lt;-- script or other resource of html is requested<br>
 * start listening for related file changes
 * <p>
 * file is changed<br>
 * --&gt; reload associated pages by sending WebSocket message
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public final class WebServerPageConnectionService {
    public static final String RELOAD_URL_PARAM = "_ij_reload";
    public static final String SERVER_RELOAD_TOOLTIP_ID = "builtin.web.server.reload.on.save";

    static final String RELOAD_WS_URL_PREFIX = "jb-server-page";

    private static final Logger LOG = Logger.getInstance(WebServerPageConnectionService.class);

    private static final String RELOAD_WS_REQUEST = "reload";
    private static final String UPDATE_LINK_WS_REQUEST_PREFIX = "update-css ";
    private static final String RELOAD_MODE_URL_PARAMETER = "reloadMode";
    private static final String REFERRER_URL_PARAMETER = "referrer";
    private static final String UPDATE_LINKS_ID_URL_PARAMETER = "jbUpdateLinksId";
    private static final Set<String> HTML_CONTENT_TYPES = Set.of("text/html", "application/xhtml+xml");
    private static final long CSS_RELOAD_CHECK_DELAY_SECONDS = 1;
    private static final long WAIT_FOR_CLIENT_SECONDS = 30;

    private final Application myApplication;
    private final ApplicationConcurrency myApplicationConcurrency;
    private final VirtualFileManager myVirtualFileManager;
    private final Provider<EditorFactory> myEditorFactory;
    private final FileDocumentManager myFileDocumentManager;

    private final AtomicBoolean myServerCreated = new AtomicBoolean();
    private final Set<WebSocketConnection> myAllClients = ConcurrentHashMap.newKeySet();
    private final RequestedPagesState myState = new RequestedPagesState();

    @Inject
    public WebServerPageConnectionService(Application application,
                                          ApplicationConcurrency applicationConcurrency,
                                          VirtualFileManager virtualFileManager,
                                          Provider<EditorFactory> editorFactory,
                                          FileDocumentManager fileDocumentManager) {
        myApplication = application;
        myApplicationConcurrency = applicationConcurrency;
        myVirtualFileManager = virtualFileManager;
        myEditorFactory = editorFactory;
        myFileDocumentManager = fileDocumentManager;
    }

    public static WebServerPageConnectionService getInstance() {
        return Application.get().getInstance(WebServerPageConnectionService.class);
    }

    /**
     * @return suffix to add to requested file in response
     */
    public @Nullable String fileRequested(HttpRequest request,
                                          boolean onlyIfHtmlFile,
                                          Supplier<? extends @Nullable VirtualFile> fileSupplier) {
        ReloadMode reloadRequest = ReloadMode.DISABLED;
        String uri = request.uri();
        String path = null;
        if (uri.contains(RELOAD_URL_PARAM)) {
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            path = decoder.path();
            List<String> values = decoder.parameters().get(RELOAD_URL_PARAM);
            ReloadMode requestedMode = values == null || values.isEmpty() ? null : parseReloadMode(values.get(0));
            if (requestedMode != null) {
                reloadRequest = requestedMode;
            }
        }
        if (reloadRequest == ReloadMode.DISABLED && myState.isEmpty()) {
            return null;
        }
        VirtualFile file = fileSupplier.get();
        if (reloadRequest == ReloadMode.DISABLED && file != null) {
            myState.resourceRequested(request, file);
            return null;
        }
        if (reloadRequest == ReloadMode.DISABLED) {
            return null;
        }
        if (file == null) {
            LOG.warn("VirtualFile for " + uri + " isn't resolved, reload on save can't be started");
            return null;
        }
        if (onlyIfHtmlFile && !isHtmlFile(file)) {
            return null;
        }
        if (path == null) {
            LOG.warn("path not evaluated for " + uri);
            return null;
        }
        myState.pageRequested(path, file, reloadRequest);

        String optionalConsoleLog = LOG.isDebugEnabled() ? "\nconsole.log('JetBrains Reload on Save script loaded');" : "";
        String contextPath = StringEscapeUtil.escape(request.contextPath(), '\'').replace("</", "<\\/");
        return """
            <script>
            (function() {%1$s
              var ws = new WebSocket((window.location.protocol === 'https:' ? 'wss://' : 'ws://') + window.location.host +
                         '%2$s/%3$s?%4$s=%5$s&'+
                         '%6$s=' + encodeURIComponent(window.location.pathname));
              ws.onmessage = function (msg) {
                  if (msg.data === 'reload') {
                      window.location.reload();
                  }
                  if (msg.data.startsWith('%7$s')) {
                      var messageId = msg.data.substring(%8$d);
                      var links = document.getElementsByTagName('link');
                      for (var i = 0; i < links.length; i++) {
                          var link = links[i];
                          if (link.rel !== 'stylesheet') continue;
                          var clonedLink = link.cloneNode(true);
                          var newHref = link.href.replace(/(&|\\?)%9$s=\\d+/, "$1%9$s=" + messageId);
                          if (newHref !== link.href) {
                            clonedLink.href = newHref;
                          }
                          else {
                            var indexOfQuest = newHref.indexOf('?');
                            if (indexOfQuest >= 0) {
                              // to support ?foo#hash
                              clonedLink.href = newHref.substring(0, indexOfQuest + 1) + '%9$s=' + messageId + '&' +
                                                newHref.substring(indexOfQuest + 1);
                            }
                            else {
                              clonedLink.href += '?' + '%9$s=' + messageId;
                            }
                          }
                          link.replaceWith(clonedLink);
                      }
                  }
              };
            })();
            </script>""".formatted(
            optionalConsoleLog,
            contextPath,
            RELOAD_WS_URL_PREFIX,
            RELOAD_MODE_URL_PARAMETER,
            reloadRequest.name(),
            REFERRER_URL_PARAMETER,
            UPDATE_LINK_WS_REQUEST_PREFIX,
            UPDATE_LINK_WS_REQUEST_PREFIX.length(),
            UPDATE_LINKS_ID_URL_PARAMETER
        );
    }

    public AsyncFileListener.@Nullable ChangeApplier reloadRelatedClients(List<VirtualFile> modifiedFiles) {
        if (!myServerCreated.get()) {
            return null;
        }

        if (myState.isRequestedFileWithoutReferrerModified(modifiedFiles)) {
            return new AsyncFileListener.ChangeApplier() {
                @Override
                public void afterVfsChange() {
                    myState.clear();
                    for (WebSocketConnection client : myAllClients) {
                        send(client, RELOAD_WS_REQUEST);
                    }
                }
            };
        }

        Map<RequestedPagesState.RequestedPage, List<VirtualFile>> affectedClients = myState.collectAffectedPages(modifiedFiles);
        if (affectedClients.isEmpty()) {
            return null;
        }

        return new AsyncFileListener.ChangeApplier() {
            @Override
            public void afterVfsChange() {
                for (Map.Entry<RequestedPagesState.RequestedPage, List<VirtualFile>> entry : affectedClients.entrySet()) {
                    RequestedPagesState.RequestedPage requestedPage = entry.getKey();
                    List<VirtualFile> affectedFiles = entry.getValue();
                    if (affectedFiles.isEmpty()) {
                        LOG.debug("Reloading page for " + requestedPage);
                        for (RequestedPageClient client : requestedPage.myClients) {
                            send(client.webSocket(), RELOAD_WS_REQUEST);
                        }
                    }
                    else {
                        int clientIndex = 0;
                        for (RequestedPageClient client : requestedPage.myClients) {
                            int messageId = myState.getNextMessageId();
                            myState.linkedFilesRequested(messageId, affectedFiles);
                            String message = UPDATE_LINK_WS_REQUEST_PREFIX + messageId;
                            for (VirtualFile affectedFile : affectedFiles) {
                                if (clientIndex == 0) {
                                    LOG.debug("Reload file " + affectedFile.getName() + " for " + requestedPage);
                                }
                                send(client.webSocket(), message);
                            }
                            myApplicationConcurrency.getScheduledExecutorService().schedule(
                                () -> {
                                    if (!myState.isAllLinkedFilesReloaded(messageId)) {
                                        LOG.debug("Some files weren't reloaded, reload whole " + requestedPage + " for client");
                                        send(client.webSocket(), RELOAD_WS_REQUEST);
                                    }
                                },
                                CSS_RELOAD_CHECK_DELAY_SECONDS,
                                TimeUnit.SECONDS
                            );
                            clientIndex++;
                        }
                    }
                }
            }
        };
    }

    public void connected(WebSocketConnection connection, @Nullable Map<String, List<String>> parameters, String referrerPrefix) {
        myServerCreated.set(true);
        myAllClients.add(connection);

        if (parameters == null) {
            return;
        }
        List<String> reloadModeParam = parameters.get(RELOAD_MODE_URL_PARAMETER);
        if (reloadModeParam == null || reloadModeParam.size() != 1) {
            return;
        }
        ReloadMode reloadMode = parseReloadMode(reloadModeParam.get(0));
        if (reloadMode == null) {
            return;
        }

        List<String> referrerParam = parameters.get(REFERRER_URL_PARAMETER);
        if (referrerParam == null || referrerParam.size() != 1) {
            return;
        }
        String referrer = referrerParam.get(0);
        String pageReferrer = !referrerPrefix.isEmpty() && referrer.startsWith(referrerPrefix)
            ? referrer.substring(referrerPrefix.length())
            : referrer;

        clientConnected(connection, pageReferrer, reloadMode);
    }

    public void disconnected(WebSocketConnection connection) {
        myAllClients.remove(connection);
        clientDisconnected(connection);
    }

    private void clientConnected(WebSocketConnection client, String referrer, ReloadMode reloadMode) {
        myState.clientConnected(client, referrer, reloadMode);
    }

    private void clientDisconnected(WebSocketConnection client) {
        myState.clientDisconnected(client);
    }

    private static void send(WebSocketConnection client, String message) {
        try {
            client.send(message);
        }
        catch (Throwable e) {
            LOG.warn("Cannot send '" + message + "' to live reload client", e);
        }
    }

    private static @Nullable ReloadMode parseReloadMode(String value) {
        try {
            return ReloadMode.valueOf(value);
        }
        catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isHtmlFile(VirtualFile file) {
        return HTML_CONTENT_TYPES.contains(FileResponses.getContentType(file.getName()));
    }

    private static @Nullable String extractRequestedPagePath(String referer) {
        return URI.create(referer).getPath();
    }

    private record RequestedPageClient(WebSocketConnection webSocket, ReloadMode reloadMode) {
    }

    private final class RequestedPagesState {
        private final Set<@Nullable VirtualFile> myRequestedFilesWithoutReferrer = new HashSet<>();
        private final Map<String, RequestedPage> myRequestedPages = new HashMap<>();

        private @Nullable Disposable myFileListenerDisposable;
        private @Nullable Disposable myDocumentListenerDisposable;

        private final AtomicInteger myMessageId = new AtomicInteger(0);
        private final Map<Integer, Set<VirtualFile>> myLinkedFilesToReload = new HashMap<>();

        synchronized void clear() {
            LOG.debug("Requested pages cleared");
            for (RequestedPage requestedPage : myRequestedPages.values()) {
                requestedPage.clear();
            }
            myRequestedPages.clear();
            cleanupIfEmpty();
        }

        synchronized void resourceRequested(HttpRequest request, VirtualFile file) {
            String referer = request.getHeaderValue(HttpHeaders.REFERER);
            boolean associatedPageFound = false;
            if (referer != null) {
                try {
                    String path = extractRequestedPagePath(referer);
                    RequestedPage page = path == null ? null : myRequestedPages.get(path);
                    if (page != null) {
                        page.myAssociatedFiles.add(file);
                        associatedPageFound = true;
                    }

                    List<String> messageIds = new QueryStringDecoder(request.uri()).parameters().get(UPDATE_LINKS_ID_URL_PARAMETER);
                    if (messageIds != null && messageIds.size() == 1) {
                        Set<VirtualFile> linkedFiles = myLinkedFilesToReload.get(Integer.parseInt(messageIds.get(0)));
                        if (linkedFiles != null) {
                            linkedFiles.remove(file);
                        }
                    }
                }
                catch (IllegalArgumentException ignored) {
                }
            }
            if (!associatedPageFound) {
                myRequestedFilesWithoutReferrer.add(file);
            }
        }

        synchronized void pageRequested(String path, VirtualFile file, ReloadMode reloadMode) {
            LOG.assertTrue(
                myRequestedPages.isEmpty() == (myFileListenerDisposable == null),
                "isEmpty: " + myRequestedPages.isEmpty() + ", disposable is null: " + (myFileListenerDisposable == null)
            );

            if (myFileListenerDisposable == null) {
                Disposable disposable = newApplicationDisposable("RequestedPagesState.myFileListenerDisposable");
                WebServerFileContentListener listener = new WebServerFileContentListener(WebServerPageConnectionService.this);
                myVirtualFileManager.addAsyncFileListener(listener, disposable);
                myFileListenerDisposable = disposable;
            }
            if (reloadMode == ReloadMode.RELOAD_ON_CHANGE && myDocumentListenerDisposable == null) {
                Disposable disposable = newApplicationDisposable("RequestedPagesState.myDocumentListenerDisposable");
                myEditorFactory.get().getEventMulticaster().addDocumentListener(new DocumentListener() {
                    @Override
                    public void documentChanged(DocumentEvent event) {
                        Document document = event.getDocument();
                        VirtualFile virtualFile = myFileDocumentManager.getFile(document);
                        if (isTrackedFile(virtualFile)) {
                            myFileDocumentManager.saveDocument(document);
                        }
                    }
                }, disposable);
                myDocumentListenerDisposable = disposable;
            }
            LOG.debug("Page is requested for " + path);

            RequestedPage page = myRequestedPages.computeIfAbsent(path, RequestedPage::new);
            page.myAssociatedFiles.add(file);
            page.scheduleCleanup();
        }

        private Disposable newApplicationDisposable(String debugName) {
            Disposable disposable = Disposable.newDisposable(debugName);
            Disposer.register(myApplication, disposable);
            return disposable;
        }

        synchronized boolean isTrackedFile(@Nullable VirtualFile virtualFile) {
            if (myRequestedFilesWithoutReferrer.contains(virtualFile)) {
                return true;
            }
            for (RequestedPage page : myRequestedPages.values()) {
                if (page.myAssociatedFiles.contains(virtualFile)) {
                    for (RequestedPageClient client : page.myClients) {
                        if (client.reloadMode() == ReloadMode.RELOAD_ON_CHANGE) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        synchronized void clientConnected(WebSocketConnection client, String referrer, ReloadMode reloadMode) {
            LOG.debug("WebSocket client connected for " + referrer);
            RequestedPage requestedPage;
            try {
                String path = extractRequestedPagePath(referrer);
                requestedPage = path == null ? null : myRequestedPages.get(path);
            }
            catch (IllegalArgumentException e) {
                requestedPage = null;
            }
            if (requestedPage == null) {
                LOG.warn("referrer not found");
                return;
            }
            requestedPage.clientConnected(client, reloadMode);
        }

        synchronized void clientDisconnected(WebSocketConnection client) {
            RequestedPage requestedPage = null;
            for (RequestedPage page : myRequestedPages.values()) {
                if (page.myClients.removeIf(it -> it.webSocket().equals(client))) {
                    requestedPage = page;
                    break;
                }
            }
            LOG.debug("WebSocket client disconnected for " + requestedPage);
            if (requestedPage != null) {
                requestedPage.scheduleCleanup();
            }
        }

        private void cleanupIfEmpty() {
            if (myRequestedPages.isEmpty()) {
                myRequestedFilesWithoutReferrer.clear();
                Disposable fileListenerDisposable = myFileListenerDisposable;
                if (fileListenerDisposable != null) {
                    Disposer.dispose(fileListenerDisposable);
                    myFileListenerDisposable = null;
                }
                Disposable documentListenerDisposable = myDocumentListenerDisposable;
                if (documentListenerDisposable != null) {
                    Disposer.dispose(documentListenerDisposable);
                    myDocumentListenerDisposable = null;
                }
            }
        }

        synchronized boolean isRequestedFileWithoutReferrerModified(List<? extends @Nullable VirtualFile> files) {
            for (VirtualFile file : files) {
                if (myRequestedFilesWithoutReferrer.contains(file)) {
                    return true;
                }
            }
            return false;
        }

        synchronized Map<RequestedPage, List<VirtualFile>> collectAffectedPages(List<VirtualFile> files) {
            Map<RequestedPage, List<VirtualFile>> result = new HashMap<>();
            for (VirtualFile modifiedFile : files) {
                for (RequestedPage requestedPage : myRequestedPages.values()) {
                    if (requestedPage.myAssociatedFiles.contains(modifiedFile)) {
                        if ("css".equalsIgnoreCase(modifiedFile.getExtension())) {
                            List<VirtualFile> affectedFiles = result.get(requestedPage);
                            if (affectedFiles == null) {
                                List<VirtualFile> cssFiles = new ArrayList<>();
                                cssFiles.add(modifiedFile);
                                result.put(requestedPage, cssFiles);
                            }
                            else if (!affectedFiles.isEmpty()) {
                                affectedFiles.add(modifiedFile);
                            }
                        }
                        else {
                            result.put(requestedPage, List.of());
                        }
                    }
                }
            }
            return result;
        }

        int getNextMessageId() {
            return myMessageId.incrementAndGet();
        }

        synchronized void linkedFilesRequested(int messageId, List<VirtualFile> affectedFiles) {
            myLinkedFilesToReload.put(messageId, new HashSet<>(affectedFiles));
        }

        synchronized boolean isAllLinkedFilesReloaded(int messageId) {
            Set<VirtualFile> linkedFiles = myLinkedFilesToReload.remove(messageId);
            return linkedFiles == null || linkedFiles.isEmpty();
        }

        synchronized boolean isEmpty() {
            return myRequestedPages.isEmpty();
        }

        synchronized void removeRequestedPageIfEmpty(RequestedPage requestedPage) {
            if (requestedPage.myDisposed) {
                return;
            }
            LOG.assertTrue(myRequestedPages.get(requestedPage.myUrl) == requestedPage);
            if (requestedPage.myClients.isEmpty()) {
                requestedPage.clear();
                myRequestedPages.remove(requestedPage.myUrl);
                cleanupIfEmpty();
            }
        }

        private final class RequestedPage {
            private final String myUrl;
            private final List<RequestedPageClient> myClients = new CopyOnWriteArrayList<>();
            private final Set<VirtualFile> myAssociatedFiles = new HashSet<>();
            private @Nullable ScheduledFuture<?> myWaitForClient;
            private boolean myDisposed;

            RequestedPage(String url) {
                myUrl = url;
            }

            void clear() {
                myDisposed = true;
                cancelWaitForClient();
            }

            void scheduleCleanup() {
                cancelWaitForClient();
                if (!myDisposed) {
                    myWaitForClient = myApplicationConcurrency.getScheduledExecutorService().schedule(
                        () -> removeRequestedPageIfEmpty(this),
                        WAIT_FOR_CLIENT_SECONDS,
                        TimeUnit.SECONDS
                    );
                }
            }

            void clientConnected(WebSocketConnection client, ReloadMode reloadMode) {
                myClients.add(new RequestedPageClient(client, reloadMode));
                cancelWaitForClient();
            }

            private void cancelWaitForClient() {
                ScheduledFuture<?> waitForClient = myWaitForClient;
                if (waitForClient != null) {
                    waitForClient.cancel(false);
                    myWaitForClient = null;
                }
            }

            @Override
            public String toString() {
                return "page (url=..." + myUrl.substring(Math.max(0, myUrl.length() - 10)) + ", " + myClients.size() + " client(s)}";
            }
        }
    }
}
