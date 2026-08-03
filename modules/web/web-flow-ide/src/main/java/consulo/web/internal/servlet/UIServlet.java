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
package consulo.web.internal.servlet;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.*;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.web.internal.ui.WebStyleCssRegistry;
import consulo.web.internal.ui.WebStyleImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
public class UIServlet extends VaadinServlet {
    public record DisposingTarget(Disposable disposable) {
    }

    public record RootUIInfo(Supplier<UIBuilder> builder) {
    }

    private final Supplier<UIBuilder> myBuilderSupplier;

    public UIServlet(Supplier<UIBuilder> aBuilderSupplier, String urlPrefix) {
        myBuilderSupplier = aBuilderSupplier;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!Boolean.getBoolean("consulo.in.sandbox")) {
            super.service(request, response);
            return;
        }

        // vaadin serves themes and other static files with max-age=3600, which makes every css/js edit
        // invisible until a manual hard refresh - unusable while developing the web ui
        super.service(request, new HttpServletResponseWrapper(response) {
            @Override
            public void setHeader(String name, String value) {
                if ("Cache-Control".equalsIgnoreCase(name)) {
                    super.setHeader(name, "no-store, must-revalidate");
                    return;
                }

                if (isCacheValidator(name)) {
                    return;
                }

                super.setHeader(name, value);
            }

            @Override
            public void setDateHeader(String name, long date) {
                if (!isCacheValidator(name)) {
                    super.setDateHeader(name, date);
                }
            }

            private boolean isCacheValidator(String name) {
                return "Expires".equalsIgnoreCase(name) || "Last-Modified".equalsIgnoreCase(name) || "ETag".equalsIgnoreCase(name);
            }
        });
    }

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException {
        VaadinServletService service = new VaadinServletService(this, deploymentConfiguration);
        service.init();
        service.setClassLoader(UIServlet.class.getClassLoader());
        return service;
    }

    public static Disposable getDisposable(UI ui) {
        DisposingTarget data = ComponentUtil.getData(ui, DisposingTarget.class);
        return Objects.requireNonNull(data, "Disposing Target is null").disposable();
    }

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();

        VaadinServletService service = getService();
        service.addSessionInitListener((SessionInitListener) se -> {
            VaadinService source = se.getSource();

            source.addUIInitListener(event -> {
                ComponentUtil.setData(event.getUI(), RootUIInfo.class, new RootUIInfo(myBuilderSupplier));
            });

            RouteRegistry registry = source.getRouter().getRegistry();

            registry.clean();

            registry.setRoute("/", VaadinRootLayout.class, List.of());
        });

        service.addSessionDestroyListener(event -> {
            VaadinSession session = event.getSession();

            Collection<UI> children = session.getUIs();
            for (UI child : children) {
                Disposable disposable = getDisposable(child);

                disposable.disposeWithTree();

                ComponentUtil.setData(child, DisposingTarget.class, null);
            }
        });

        service.addUIInitListener(event -> {
            UI ui = event.getUI();

            Disposable vaadinUiDisposable = Disposable.newDisposable("Vaadin UI Disposable");

            ComponentUtil.setData(ui, DisposingTarget.class, new DisposingTarget(vaadinUiDisposable));

            StyleManager styleManager = StyleManager.get();

            applyStyle(ui, styleManager.getCurrentStyle());

            Disposable listenerDisposer = styleManager.addChangeListener((oldStyle, newStyle) -> applyStyle(ui, newStyle));

            Disposer.register(vaadinUiDisposable, listenerDisposer);
        });
    }

    private static void applyStyle(UI ui, Style style) {
        // a style change arrives off the ui thread, and a ui the browser has already navigated away from is
        // still on the listener list until its session is cleaned up - touching its page throws
        if (ui.getSession() == null) {
            return;
        }

        ui.access(() -> {
            ui.getPage().setColorScheme(((WebStyleImpl)style).getVaadinThemeId());

            // vaadin owns the 'theme' attribute and knows light and dark only, this one carries the consulo
            // style id, which is what tells the styles sharing a vaadin color scheme apart
            ui.getPage().executeJs(
                "document.documentElement.setAttribute($0, $1)",
                WebStyleCssRegistry.STYLE_ATTRIBUTE,
                style.getId()
            );

            // every web-image leaf rewrites its url against this attribute and re-renders when it changes -
            // that is what makes the icons follow the style, including the ones the server re-syncs later
            // with the url they were first rendered with
            ui.getPage().executeJs(
                "document.documentElement.setAttribute('consulo-icon-version', $0)",
                String.valueOf(IconLibraryManager.get().getModificationCount())
            );
        });
    }
}