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
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.application.WebApplication;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 26/05/2023
 */
@PreserveOnRefresh
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // @PreserveOnRefresh moves this layout into a freshly created UI and closes the old one without
        // running the constructor again, so the session would keep pointing to a detached UIAccess
        WebApplication application = WebApplication.getInstance();
        if (application != null && application.getCurrentSession() != null) {
            application.setCurrentSession(new VaadinWebSessionImpl());
        }
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
