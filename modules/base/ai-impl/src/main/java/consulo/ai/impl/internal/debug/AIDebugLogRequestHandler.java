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
package consulo.ai.impl.internal.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.http.HttpMethod;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Takes the log lines of Debug mode from code which cannot write a file - {@code POST /ai-debug/log/<chat>/<token>}
 * appends the body as a line, {@code GET /ai-debug/logs/<chat>/<token>} reads the log back.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class AIDebugLogRequestHandler extends HttpRequestHandler {
    private static final String TEXT_CONTENT_TYPE = "text/plain; charset=utf-8";

    private final AIDebugLogManager myManager;

    @Inject
    public AIDebugLogRequestHandler(AIDebugLogManager manager) {
        myManager = manager;
    }

    @Override
    public boolean isSupported(HttpRequest request) {
        return checkPrefix(request.path(), AIDebugLogManager.PATH)
            && (request.method() == HttpMethod.POST || request.method() == HttpMethod.GET);
    }

    @Override
    public HttpResponse process(HttpRequest request) throws IOException {
        String[] parts = request.path().substring(1).split("/");
        if (parts.length != 4) {
            return HttpResponse.notFound();
        }
        String id = parts[2];
        String token = parts[3];
        if ("log".equals(parts[1]) && request.method() == HttpMethod.POST) {
            boolean appended = myManager.append(id, token, request.getContentAsString(StandardCharsets.UTF_8));
            return HttpResponse.create(appended ? HttpURLConnection.HTTP_NO_CONTENT : HttpURLConnection.HTTP_FORBIDDEN, null, null);
        }
        if ("logs".equals(parts[1]) && request.method() == HttpMethod.GET) {
            String text = myManager.read(id, token);
            return text == null
                ? HttpResponse.create(HttpURLConnection.HTTP_FORBIDDEN, null, null)
                : HttpResponse.ok(TEXT_CONTENT_TYPE, text.getBytes(StandardCharsets.UTF_8));
        }
        return HttpResponse.create(HttpURLConnection.HTTP_BAD_METHOD, null, null);
    }
}
