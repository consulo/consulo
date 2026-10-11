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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.layout.DesktopQtTabImpl;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.BaseSpotlight;
import consulo.ui.layout.Layout;
import io.qt.widgets.QTabWidget;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class DesktopQtSpotlightImpl extends BaseSpotlight {
    private @Nullable DesktopQtSpotlightOverlay myOverlay;

    @RequiredUIAccess
    public DesktopQtSpotlightImpl(Layout<?> layout) {
        super(layout);

        if (layout instanceof QtComponentDelegate<?> delegate) {
            delegate.whenBound(this::attach);
        }
    }

    private void attach(QWidget host) {
        if (isDisposed()) {
            return;
        }

        DesktopQtSpotlightOverlay overlay = myOverlay;
        if (overlay != null && !overlay.isDisposed()) {
            if (overlay.getHost() == host) {
                return;
            }

            overlay.detach();
        }

        overlay = DesktopQtSpotlightOverlay.create(host, this::collectHoles);
        myOverlay = overlay;
        overlay.refresh(isLit());
    }

    private List<DesktopQtSpotlightHole> collectHoles() {
        List<DesktopQtSpotlightHole> holes = new ArrayList<>();
        for (Component target : getTargets()) {
            if (target instanceof QtComponentDelegate<?> delegate) {
                QWidget widget = delegate.toQtComponent();
                if (widget != null && !widget.isDisposed()) {
                    holes.add(DesktopQtSpotlightHole.component(widget, target instanceof Layout<?>));
                }
            }
        }

        for (Tab tab : getTabTargets()) {
            if (tab instanceof DesktopQtTabImpl qtTab) {
                QTabWidget tabWidget = qtTab.getTabWidget();
                int index = qtTab.getIndex();
                if (tabWidget != null && index != -1) {
                    holes.add(DesktopQtSpotlightHole.tab(tabWidget.tabBar(), index));
                }
            }
        }
        return holes;
    }

    @RequiredUIAccess
    @Override
    protected void onTargetsChanged(List<Component> oldTargets, List<Component> newTargets) {
        DesktopQtSpotlightOverlay overlay = myOverlay;
        if (overlay != null) {
            overlay.refresh(isLit());
        }
    }

    @Override
    protected void onDispose(List<Component> oldTargets) {
        DesktopQtSpotlightOverlay overlay = myOverlay;
        myOverlay = null;

        if (overlay != null) {
            overlay.detach();
        }
    }
}
