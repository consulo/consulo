/*
 * Copyright 2013-2023 consulo.io
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

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLayout;
import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Label;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Not preserved across a refresh on purpose. A preserved layout is moved into a ui the browser never bootstraps,
 * which leaves the tab split between the ui it renders and the ui the server works on. A refresh builds everything
 * anew instead, and an open project of the previous ui is simply listed as opened on the welcome screen.
 *
 * @author VISTALL
 * @since 26/05/2023
 */
public class VaadinRootLayout extends HorizontalLayout implements RouterLayout, FromVaadinComponentWrapper {
    private UIWindowOverRouterLayout myUIWindow = new UIWindowOverRouterLayout(this);

    @RequiredUIAccess
    public VaadinRootLayout() {
        setSizeFull();
        setMargin(false);
        setPadding(false);
        setSpacing(false);

        UIServlet.RootUIInfo data = ComponentUtil.getData(UI.getCurrent(), UIServlet.RootUIInfo.class);
        if (data == null) {
            return;
        }

        Supplier<UIBuilder> builder = data.builder();

        UIBuilder uiBuilder = builder.get();

        uiBuilder.build(myUIWindow);
    }

    /**
     * Gives up what the ui was showing for a screen which says so. The ui itself stays alive - a closed one answers
     * nothing, and the button below has to be heard.
     */
    @RequiredUIAccess
    public void showClosed() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }

        removeAll();

        Window window = Window.create(
            Application.get().getName().get(),
            WindowOptions.builder().disableResize().disableClose().build()
        );
        window.setContent(buildClosedContent(ui));
        window.show();
    }

    @RequiredUIAccess
    private static consulo.ui.Component buildClosedContent(UI ui) {
        VerticalLayout content = VerticalLayout.create();
        content.add(Label.create(LocalizeValue.localizeTODO("Session Closed")));
        content.add(Button.create(LocalizeValue.localizeTODO("Refresh"), event -> ui.getPage().reload()));
        return content;
    }

    public void update(Component newContent) {
        removeAll();

        ((HasSize) newContent).setSizeFull();

        add(newContent);
        setFlexGrow(1, newContent);
    }

    @Override
    public consulo.ui.@Nullable Component toUIComponent() {
        return myUIWindow;
    }
}
