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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Menu;
import consulo.ui.MenuItem;
import consulo.ui.PopupMenu;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * @author VISTALL
 * @since 2026-07-31
 */
public class WebPopupMenuImpl extends VaadinComponentDelegate<WebPopupMenuImpl.Vaadin> implements PopupMenu {
    public class Vaadin extends ContextMenu implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebPopupMenuImpl.this;
        }
    }

    private final LocalizeValue myText = LocalizeValue.empty();
    private final com.vaadin.flow.component.Component myTarget;

    private @Nullable Image myIcon;
    private boolean myEnabled = true;

    public WebPopupMenuImpl(Component target) {
        myTarget = TargetVaadin.to(target);

        getVaadinComponent().setTarget(myTarget);
    }

    @Override
    public void setOpenOnClick(boolean openOnClick) {
        getVaadinComponent().setOpenOnClick(openOnClick);
    }

    @Override
    public void hide() {
        getVaadinComponent().close();
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    @Override
    @RequiredUIAccess
    public void setEnabled(boolean value) {
        myEnabled = value;
    }

    @Override
    @RequiredUIAccess
    public Menu add(MenuItem menuItem) {
        if (menuItem instanceof WebMenuItemImpl webMenuItem) {
            webMenuItem.render(getVaadinComponent());
        }

        return this;
    }

    @Override
    @RequiredUIAccess
    public void show(int relativeX, int relativeY) {
        Vaadin menu = getVaadinComponent();

        // vaadin only attaches the overlay by itself on the browser driven open, a programmatic one has to
        // put it into the ui first, otherwise there is no element to run the script below on
        if (!menu.isAttached()) {
            Optional<UI> ui = myTarget.getUI();
            if (ui.isEmpty()) {
                return;
            }

            if (ui.get().hasModalComponent()) {
                ui.get().addToModalComponent(menu);
            }
            else {
                ui.get().add(menu);
            }
        }

        // the overlay is opened by the client and the flow api exposes no way to open it at a point - the
        // element method takes a stand-in for the mouse event it reads the coordinates and the target from
        menu.getElement().executeJs(
            """
                const target = $0;
                const rect = target.getBoundingClientRect();
                this.open({
                    detail: {x: rect.left + $1, y: rect.top + $2},
                    target: target,
                    composedPath: () => [target],
                    preventDefault: () => {},
                    stopPropagation: () => {}
                });
                """,
            myTarget.getElement(),
            relativeX,
            relativeY
        );
    }
}
