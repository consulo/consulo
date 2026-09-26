/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.internal.platform;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.logging.Logger;
import consulo.platform.impl.PlatformBase;
import consulo.ui.UIAccess;
import consulo.util.io.Urls;
import consulo.web.internal.servlet.BuiltInWebServerServlet;
import consulo.web.internal.servlet.BuiltInWebServerSessionTokens;
import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.URL;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebPlatformImpl extends PlatformBase {
    private static final String OPEN_TAB_SCRIPT = "const w = window.open($0, '_blank'); if (w) { w.opener = null; } return w !== null;";

    public WebPlatformImpl() {
        super(LOCAL, LOCAL, getSystemJvmProperties());
    }

    @Override
    public boolean isInBrowser() {
        return true;
    }

    @Override
    public void openInBrowser(URL url) {
        String protocol = url.getProtocol();
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            Logger.getInstance(WebPlatformImpl.class).warn("Only http(s) urls can be opened from the browser page: " + url);
            return;
        }

        UI ui = UI.getCurrent();
        if (ui == null) {
            Logger.getInstance(WebPlatformImpl.class).warn("There is no browser page to open " + url + " in");
            return;
        }

        String target = resolveTarget(ui, url);
        if (target == null) {
            return;
        }

        ui.getPage().executeJs(OPEN_TAB_SCRIPT, target).then(Boolean.class, opened -> {
            if (!Boolean.TRUE.equals(opened)) {
                Logger.getInstance(WebPlatformImpl.class).warn("Browser blocked the tab for " + url);
            }
        });
    }

    private static @Nullable String resolveTarget(UI ui, URL url) {
        String target = url.toExternalForm();
        if (!BuiltInServerManager.getInstance().isOnBuiltInWebServer(Urls.parseEncoded(target))) {
            return target;
        }

        VaadinSession session = ui.getSession();
        if (session == null || !(session.getService() instanceof VaadinServletService service)) {
            Logger.getInstance(WebPlatformImpl.class).warn("There is no servlet session to open " + url + " through");
            return null;
        }

        ServletContext context = service.getServlet().getServletContext();
        String token = BuiltInWebServerSessionTokens.get(context).acquire(session);
        String query = url.getQuery();
        return BuiltInWebServerServlet.RELATIVE_PREFIX + token + url.getPath() + (query == null ? "" : "?" + query);
    }

    @Override
    public void openFileInFileManager(File file, UIAccess uiAccess) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void openDirectoryInFileManager(File file, UIAccess uiAccess) {
        throw new UnsupportedOperationException();
    }
}
