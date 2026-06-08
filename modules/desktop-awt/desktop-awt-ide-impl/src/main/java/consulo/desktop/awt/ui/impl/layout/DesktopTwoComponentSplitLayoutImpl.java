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

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2016-06-13
 */
public class DesktopTwoComponentSplitLayoutImpl extends SwingComponentDelegate<Splitter> implements TwoComponentSplitLayout {
    private final SplitLayoutPosition myPosition;

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
        return new MySplitter(myPosition == SplitLayoutPosition.VERTICAL);
    }

    @Override
    public TwoComponentSplitLayout withProportion(int percent) {
        toAWTComponent().setProportion(percent / 100f);
        return this;
    }

    @Override
    @RequiredUIAccess
    public TwoComponentSplitLayout withFirstComponent(Component component) {
        toAWTComponent().setFirstComponent((JComponent) TargetAWT.to(component));
        return this;
    }

    @Override
    @RequiredUIAccess
    public TwoComponentSplitLayout withSecondComponent(Component component) {
        toAWTComponent().setSecondComponent((JComponent) TargetAWT.to(component));
        return this;
    }
}
