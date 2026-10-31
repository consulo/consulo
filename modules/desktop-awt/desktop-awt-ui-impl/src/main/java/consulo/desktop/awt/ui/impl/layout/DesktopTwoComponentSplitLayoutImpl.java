/*
 * Copyright 2013-2016 consulo.io
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

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.event.SplitProportionChangedEvent;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2016-06-13
 */
public class DesktopTwoComponentSplitLayoutImpl extends SwingComponentDelegate<Splitter> implements TwoComponentSplitLayout {
    private SplitLayoutPosition myPosition;

    private @Nullable Component myFirstComponent;
    private @Nullable Component mySecondComponent;

    class MySplitter extends OnePixelSplitter implements FromSwingComponentWrapper {
        MySplitter(boolean vertical) {
            super(vertical);
        }

        @Override
        public Component toUIComponent() {
            return DesktopTwoComponentSplitLayoutImpl.this;
        }
    }

    public DesktopTwoComponentSplitLayoutImpl(SplitLayoutPosition position) {
        myPosition = position;
    }

    @Override
    protected Splitter createComponent() {
        MySplitter splitter = new MySplitter(myPosition == SplitLayoutPosition.VERTICAL);
        // the divider stays where the proportion puts it, as on the other frontends: honoring minimum sizes moved it
        // whenever a side's content changed - a longer path, a wider status line - while the user dragged it nowhere
        splitter.setHonorComponentsMinimumSize(false);
        splitter.addPropertyChangeListener(Splitter.PROP_PROPORTION, event -> {
            int percent = Math.round(((Float) event.getNewValue()) * 100f);
            getListenerDispatcher(SplitProportionChangedEvent.class)
                .onEvent(new SplitProportionChangedEvent(this, DesktopAWTInputDetails.currentEvent(splitter), percent));
        });
        return splitter;
    }

    @Override
    public void setProportion(int percent) {
        toAWTComponent().setProportion(percent / 100f);
    }

    @Override
    @RequiredUIAccess
    public void setPosition(SplitLayoutPosition position) {
        myPosition = position;
        toAWTComponent().setOrientation(position == SplitLayoutPosition.VERTICAL);
    }

    @Override
    @RequiredUIAccess
    public void setFirstComponent(Component component) {
        myFirstComponent = component;
        toAWTComponent().setFirstComponent((JComponent) TargetAWT.to(component));
    }

    @Override
    @RequiredUIAccess
    public void setSecondComponent(Component component) {
        mySecondComponent = component;
        toAWTComponent().setSecondComponent((JComponent) TargetAWT.to(component));
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        DesktopLayoutChildren.acceptAll(consumer, myFirstComponent, mySecondComponent);
    }
}
