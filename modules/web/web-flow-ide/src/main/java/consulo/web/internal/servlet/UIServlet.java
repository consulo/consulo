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
import consulo.ui.style.StyleManager;
import consulo.web.internal.ui.WebStyleImpl;
import jakarta.servlet.ServletException;

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

            Disposable listenerDisposer = styleManager.addChangeListener((oldStyle, newStyle) -> {
                ThemeList themeList = ui.getElement().getThemeList();

                themeList.remove(((WebStyleImpl) oldStyle).getVaadinThemeId());

                themeList.add(((WebStyleImpl) newStyle).getVaadinThemeId());
            });

            Disposer.register(vaadinUiDisposable, listenerDisposer);
        });
    }
}