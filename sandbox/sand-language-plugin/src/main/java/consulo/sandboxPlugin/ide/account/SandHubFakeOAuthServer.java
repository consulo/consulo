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
package consulo.sandboxPlugin.ide.account;

import consulo.annotation.component.ExtensionImpl;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.http.OriginCheckResult;
import consulo.http.HttpMethod;
import consulo.util.io.DigestUtil;
import consulo.util.lang.StringUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class SandHubFakeOAuthServer extends HttpRequestHandler {
    public static final String URL = "http://127.0.0.1:" + SandHubFakeOAuthPortManager.PORT + "/sandHub";

    private static final String PREFIX = "/sandHub/";

    private final Map<String, String> myChallenges = new ConcurrentHashMap<>();

    @Override
    public boolean isSupported(HttpRequest request) {
        return request.localPort() == SandHubFakeOAuthPortManager.PORT
            && (request.method() == HttpMethod.GET || request.method() == HttpMethod.POST)
            && request.path().startsWith(PREFIX);
    }

    @Override
    protected OriginCheckResult isOriginAllowed(HttpRequest request) {
        return OriginCheckResult.ALLOW;
    }

    @Override
    public HttpResponse process(HttpRequest request) {
        String action = request.path().substring(PREFIX.length());
        switch (action) {
            case "authorize" -> {
                String callback = StringUtil.notNullize(request.getParameterValue("callback_url"));
                String code = "sand-" + DigestUtil.randomToken();
                myChallenges.put(code, StringUtil.notNullize(request.getParameterValue("code_challenge")));
                String accept = callback + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
                String deny = callback + "?error=access_denied&error_description=" + URLEncoder.encode("Denied in Sand Hub", StandardCharsets.UTF_8);
                return html("<h2>Sand Hub</h2><p>Consulo asks for access to your Sand Hub account.</p>"
                    + "<p><a id='accept' href='" + accept + "'>Authorize</a> &nbsp; <a id='deny' href='" + deny + "'>Deny</a></p>");
            }
            case "access_token" -> {
                String code = StringUtil.notNullize(request.getParameterValue("code"));
                String verifier = StringUtil.notNullize(request.getParameterValue("code_verifier"));
                String challenge = myChallenges.remove(code);
                String expected = Base64.getUrlEncoder().encodeToString(DigestUtil.sha256().digest(verifier.getBytes(StandardCharsets.UTF_8)));
                if (challenge == null || !challenge.equals(expected)) {
                    return HttpResponse.create(400, "text/plain", "Invalid code or code verifier".getBytes(StandardCharsets.UTF_8));
                }
                return HttpResponse.ok("text/plain", new byte[0]).withHeader("X-OAuth-Token", "oauth.user:read_user,repo");
            }
            case "complete" -> {
                return html("<h2>Sand Hub</h2><p>You are logged in. Return to Consulo.</p>");
            }
            case "error" -> {
                return html("<h2>Sand Hub</h2><p>Authorization failed.</p>");
            }
            default -> {
                return HttpResponse.notFound();
            }
        }
    }

    private static HttpResponse html(String body) {
        return HttpResponse.ok("text/html;charset=utf-8", ("<html><body>" + body + "</body></html>").getBytes(StandardCharsets.UTF_8));
    }
}
