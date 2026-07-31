/*
 * Copyright 2013-2019 consulo.io
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
package consulo.web.internal.ui;

import com.vaadin.flow.component.contextmenu.HasMenuItems;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.MenuItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.image.WebImageConverter;
import consulo.web.internal.ui.vaadin.SimpleComponent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebMenuItemImpl extends VaadinComponentDelegate<WebMenuItemImpl.Vaadin> implements MenuItem {
    public class Vaadin extends SimpleComponent implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebMenuItemImpl.this;
        }
    }

    private static final int ICON_SIZE = 16;

    /** the icon column is wider than the icon so the labels sit further from the edge */
    private static final int SLOT_SIZE = 24;

    private LocalizeValue myText;
    private @Nullable Image myIcon;
    private LocalizeValue myShortcutText = LocalizeValue.empty();
    private @Nullable Boolean myChecked;
    private boolean myEnabled = true;
    private boolean myVisible = true;

    private com.vaadin.flow.component.contextmenu.@Nullable MenuItem myVaadinItem;

    public WebMenuItemImpl(LocalizeValue text) {
        myText = text;
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    public void setText(LocalizeValue text) {
        myText = text;

        if (myVaadinItem != null) {
            myVaadinItem.setText(getPlainText());
        }
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public void setShortcutText(LocalizeValue shortcutText) {
        myShortcutText = shortcutText;
    }

    /**
     * @param checked null - item is not checkable at all
     */
    public void setChecked(@Nullable Boolean checked) {
        myChecked = checked;
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    @Override
    @RequiredUIAccess
    public void setEnabled(boolean value) {
        myEnabled = value;

        if (myVaadinItem != null) {
            myVaadinItem.setEnabled(value);
        }
    }

    @Override
    public boolean isVisible() {
        return myVisible;
    }

    @Override
    @RequiredUIAccess
    public void setVisible(boolean value) {
        myVisible = value;

        if (myVaadinItem != null) {
            myVaadinItem.setVisible(value);
        }
    }

    protected String getPlainText() {
        return LocalizeValueWithMnemonic.get(myText).getText();
    }

    /**
     * Builds vaadin representation of this item inside given menu holder. Called by {@link WebMenuBarImpl} for root items and by
     * {@link WebMenuImpl} for children.
     */
    @RequiredUIAccess
    public void render(HasMenuItems target) {
        com.vaadin.flow.component.contextmenu.MenuItem item = target.addItem(
            getPlainText(),
            event -> getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this))
        );

        myVaadinItem = item;

        item.setEnabled(myEnabled);
        item.setVisible(myVisible);

        if (myChecked != null && !(target instanceof MenuBar)) {
            item.setCheckable(true);
            item.setChecked(myChecked);
        }

        if (target instanceof MenuBar && myIcon != null) {
            // a root item of a bar has no icon column - a tool window header shows its gear by the icon alone,
            // so it goes in as a plain child instead
            com.vaadin.flow.component.Component icon = WebImageConverter.getImage(myIcon);
            icon.getElement().getStyle()
                .set("width", ICON_SIZE + "px")
                .set("height", ICON_SIZE + "px");

            item.addComponentAsFirst(icon);
        }

        if (!(target instanceof MenuBar)) {
            // the leading column is always reserved, otherwise the labels of a menu do not line up. the vaadin
            // checkmark slot is hidden by the theme and the checkmark is drawn in this same column, so a toggle
            // item is aligned with an item that carries an icon
            Span slot = new Span();
            slot.getElement().getStyle()
                .set("flex", "0 0 " + SLOT_SIZE + "px")
                .set("width", SLOT_SIZE + "px")
                .set("height", ICON_SIZE + "px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");

            if (myIcon != null) {
                com.vaadin.flow.component.Component icon = WebImageConverter.getImage(myIcon);
                icon.getElement().getStyle()
                    .set("width", ICON_SIZE + "px")
                    .set("height", ICON_SIZE + "px");

                slot.add(icon);
            }
            else if (Boolean.TRUE.equals(myChecked)) {
                slot.setText("\u2713");
            }

            item.addComponentAsFirst(slot);
        }

        if (myShortcutText.isNotEmpty()) {
            Span shortcut = new Span(myShortcutText.get());
            shortcut.getStyle()
                .set("margin-inline-start", "auto")
                .set("padding-inline-start", "2em")
                .set("opacity", "0.6");
            item.add(shortcut);
        }

        renderChildren(item);
    }

    @RequiredUIAccess
    protected void renderChildren(com.vaadin.flow.component.contextmenu.MenuItem item) {
    }

    protected com.vaadin.flow.component.contextmenu.@Nullable MenuItem getVaadinItem() {
        return myVaadinItem;
    }
}
