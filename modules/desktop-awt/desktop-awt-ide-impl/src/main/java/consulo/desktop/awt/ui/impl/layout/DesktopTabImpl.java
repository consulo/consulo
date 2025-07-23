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
package consulo.desktop.awt.ui.impl.layout;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.tab.TabInfo;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
public class DesktopTabImpl implements Tab {
    private static class CloseAction extends DumbAwareAction {
        private final BiConsumer<Tab, Component> myCloseHandler;
        private final DesktopTabImpl myTabInfo;

        public CloseAction(BiConsumer<Tab, Component> closeHandler, DesktopTabImpl tabInfo) {
            myCloseHandler = closeHandler;
            myTabInfo = tabInfo;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myCloseHandler.accept(myTabInfo, myTabInfo.myComponent);
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setIcon(PlatformIconGroup.actionsCancel());
            e.getPresentation().setTextValue(LocalizeValue.localizeTODO("Close"));
        }
    }

    private BiConsumer<Tab, TextItemPresentation> myRender = (tab, presentation) -> presentation.append(toString());

    private final TabInfo myTabInfo = new TabInfo(null);

    private final DesktopTabbedLayoutImpl myTabbedLayout;

    private Component myComponent;

    public DesktopTabImpl(DesktopTabbedLayoutImpl tabbedLayout) {
        myTabbedLayout = tabbedLayout;
    }

    public void setComponent(Component component) {
        myComponent = component;

        myTabInfo.setComponent(TargetAWT.to(component));
    }

    public TabInfo getTabInfo() {
        return myTabInfo;
    }

    @Override
    public void update() {
        myRender.accept(this, new TextItemPresentation() {
            @Override
            public void clearText() {
                myTabInfo.setText("");
            }

            @Nonnull
            @Override
            public TextItemPresentation withIcon(@Nullable Image image) {
                myTabInfo.setIcon(image);
                return this;
            }

            @Override
            public void append(@Nonnull LocalizeValue text, @Nonnull TextAttribute textAttribute) {
                String oldText = myTabInfo.getText();
                myTabInfo.setText(StringUtil.notNullize(oldText) + text.getValue());
            }
        });
    }

    @Override
    public void setRender(@Nonnull BiConsumer<Tab, TextItemPresentation> render) {
        myRender = render;
    }

    @Override
    public void setCloseHandler(@Nullable BiConsumer<Tab, Component> closeHandler) {
        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
        builder.add(new CloseAction(closeHandler, this));

        myTabInfo.setTabLabelActions(builder.build(), "TabActions");
    }

    @Override
    public void select() {
        myTabbedLayout.toAWTComponent().select(myTabInfo, true);
    }
}
