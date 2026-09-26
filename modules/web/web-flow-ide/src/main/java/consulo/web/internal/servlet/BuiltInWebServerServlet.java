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
package consulo.web.internal.servlet;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.internal.ApplicationEx;
import consulo.builtinWebServer.http.HttpFileRegion;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.impl.webServer.BuiltInWebServer;
import consulo.http.HttpMethod;
import consulo.logging.Logger;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
@WebServlet(urlPatterns = "/builtin/*")
public class BuiltInWebServerServlet extends HttpServlet {
    public static final String RELATIVE_PREFIX = "builtin/";

    private static final Logger LOG = Logger.getInstance(BuiltInWebServerServlet.class);

    private static final String CONTENT_SECURITY_POLICY = "sandbox allow-scripts allow-forms allow-popups allow-modals allow-downloads";

    static String prefix(String contextPath, String token) {
        return contextPath + "/" + RELATIVE_PREFIX + token;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        serve(req, resp, HttpMethod.GET);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        serve(req, resp, HttpMethod.HEAD);
    }

    private void serve(HttpServletRequest req, HttpServletResponse resp, HttpMethod method) throws IOException {
        Application application = ApplicationManager.getApplication();
        if (application == null || !((ApplicationEx) application).isLoaded()) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        ServletHttpRequest request = createRequest(req, method);
        if (request == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            HttpResponse response = application.getExtensionPoint(HttpRequestHandler.class)
                .findExtensionOrFail(BuiltInWebServer.class)
                .processPath(request, null, true);
            if (response == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            write(request, response, resp);
        }
        catch (IOException e) {
            if (resp.isCommitted()) {
                LOG.debug(e);
            }
            else {
                LOG.error(e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
        catch (Exception e) {
            LOG.error(e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private @Nullable ServletHttpRequest createRequest(HttpServletRequest req, HttpMethod method) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.charAt(0) != '/') {
            return null;
        }

        int tokenEnd = pathInfo.indexOf('/', 1);
        if (tokenEnd <= 1 || tokenEnd == pathInfo.length() - 1) {
            return null;
        }

        String token = pathInfo.substring(1, tokenEnd);
        if (!BuiltInWebServerSessionTokens.get(getServletContext()).isValid(token)) {
            return null;
        }

        String rawPrefix = prefix(req.getContextPath(), token);
        String requestUri = req.getRequestURI();
        if (!requestUri.startsWith(rawPrefix + "/")) {
            return null;
        }

        String rawRest = requestUri.substring(rawPrefix.length());
        String query = req.getQueryString();
        String rawRestWithQuery = query == null ? rawRest : rawRest + "?" + query;
        return new ServletHttpRequest(req, method, rawPrefix, pathInfo.substring(tokenEnd), rawRestWithQuery);
    }

    private static void write(ServletHttpRequest request, HttpResponse response, HttpServletResponse resp) throws IOException {
        if (response.getStreamingBody() != null) {
            LOG.error("Streaming responses are not supported by the built-in web server bridge: " + request.path());
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }

        int code = response.getCode();
        boolean notModified = code == HttpServletResponse.SC_NOT_MODIFIED;
        boolean writeBody = request.method() != HttpMethod.HEAD && !notModified && !request.isTerminated();

        HttpFileRegion region = response.getFileRegion();
        if (region != null && writeBody) {
            FileChannel channel = openRegion(region);
            if (channel == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try (channel) {
                writeHead(request, response, resp);
                resp.setContentLengthLong(region.length());
                OutputStream outputStream = resp.getOutputStream();
                transfer(channel, region, outputStream);
                byte[] suffix = region.suffix();
                if (suffix.length > 0) {
                    outputStream.write(suffix);
                }
            }
            return;
        }

        writeHead(request, response, resp);
        if (notModified || request.isTerminated()) {
            return;
        }

        byte[] content = response.getContent();
        if (content != null) {
            resp.setContentLength(content.length);
            if (writeBody) {
                resp.getOutputStream().write(content);
            }
        }
        else if (region != null) {
            resp.setContentLengthLong(region.length());
        }
    }

    private static void writeHead(ServletHttpRequest request, HttpResponse response, HttpServletResponse resp) {
        resp.setStatus(response.getCode());

        String contentType = response.getContentType();
        if (contentType != null) {
            resp.setContentType(contentType);
        }

        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            resp.setHeader(header.getKey(), header.getValue());
        }

        resp.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        resp.setHeader("Referrer-Policy", "same-origin");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        if (request.isTerminated()) {
            resp.setHeader("Connection", "close");
        }
    }

    private static @Nullable FileChannel openRegion(HttpFileRegion region) throws IOException {
        try {
            return FileChannel.open(region.file(), StandardOpenOption.READ);
        }
        catch (NoSuchFileException e) {
            return null;
        }
    }

    private static void transfer(FileChannel channel, HttpFileRegion region, OutputStream outputStream) throws IOException {
        WritableByteChannel target = Channels.newChannel(outputStream);
        long done = 0;
        while (done < region.count()) {
            long transferred = channel.transferTo(region.position() + done, region.count() - done, target);
            if (transferred <= 0) {
                break;
            }
            done += transferred;
        }
    }
}
