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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.vaadin.CompositeComponent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-05-29
 */
public class WebThreeComponentSplitLayoutImpl extends VaadinComponentDelegate<WebThreeComponentSplitLayoutImpl.Vaadin>
    implements ThreeComponentSplitLayout {
    private static final double FIRST_PROPORTION = 25;
    private static final double CENTER_PROPORTION = 75;

    private static final String NO_DIVIDER_CLASS = "web-split-layout-no-divider";

    public class Vaadin extends CompositeComponent implements FromVaadinComponentWrapper {
        private final SplitLayout myFirstLayout;
        private final SplitLayout mySecondLayout;

        private com.vaadin.flow.component.Component myFirstComponent;
        private com.vaadin.flow.component.Component myCenterComponent;
        private com.vaadin.flow.component.Component mySecondComponent;

        public Vaadin() {
            myFirstLayout = new SplitLayout(SplitLayout.Orientation.HORIZONTAL);
            mySecondLayout = new SplitLayout(SplitLayout.Orientation.HORIZONTAL);

            myFirstLayout.addToSecondary(mySecondLayout);

            myFirstLayout.setSizeFull();
            add(myFirstLayout);

            updateSplitterPositions();
        }

        public void setOrientation(SplitLayout.Orientation orientation) {
            myFirstLayout.setOrientation(orientation);
            mySecondLayout.setOrientation(orientation);
        }

        public void setFirst(com.vaadin.flow.component.@Nullable Component component) {
            removeIfChild(myFirstLayout, myFirstComponent);

            myFirstComponent = component;

            if (component != null) {
                myFirstLayout.addToPrimary(component);
            }

            updateSplitterPositions();
        }

        public void setCenter(com.vaadin.flow.component.@Nullable Component component) {
            removeIfChild(mySecondLayout, myCenterComponent);

            myCenterComponent = component;

            if (component != null) {
                mySecondLayout.addToPrimary(component);
            }
        }

        public void setSecond(com.vaadin.flow.component.@Nullable Component component) {
            removeIfChild(mySecondLayout, mySecondComponent);

            mySecondComponent = component;

            if (component != null) {
                mySecondLayout.addToSecondary(component);
            }

            updateSplitterPositions();
        }

        // a missing side must not reserve space, otherwise the center keeps only a fraction of the frame,
        // and like the awt splitter it must not show a divider for a side that is not there
        private void updateSplitterPositions() {
            myFirstLayout.setSplitterPosition(myFirstComponent == null ? 0 : FIRST_PROPORTION);
            mySecondLayout.setSplitterPosition(mySecondComponent == null ? 100 : CENTER_PROPORTION);

            myFirstLayout.setClassName(NO_DIVIDER_CLASS, myFirstComponent == null);
            mySecondLayout.setClassName(NO_DIVIDER_CLASS, mySecondComponent == null);
        }

        // the component may already have been reparented, for example into a splitter built for a
        // primary plus split tool window pair, and vaadin refuses to remove a component it does not own
        private static void removeIfChild(SplitLayout layout, com.vaadin.flow.component.@Nullable Component component) {
            if (component != null && layout.getChildren().anyMatch(child -> child == component)) {
                layout.remove(component);
            }
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebThreeComponentSplitLayoutImpl.this;
        }
    }

    public WebThreeComponentSplitLayoutImpl(SplitLayoutPosition position) {
        // createVaadinComponent() already ran from the super constructor, so the orientation is applied here
        toVaadinComponent().setOrientation(position == SplitLayoutPosition.VERTICAL
            ? SplitLayout.Orientation.VERTICAL
            : SplitLayout.Orientation.HORIZONTAL);
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    @RequiredUIAccess
    public ThreeComponentSplitLayout setFirstComponent(@Nullable Component component) {
        toVaadinComponent().setFirst(toSizedVaadin(component));
        return this;
    }

    @Override
    @RequiredUIAccess
    public ThreeComponentSplitLayout setCenterComponent(@Nullable Component component) {
        toVaadinComponent().setCenter(toSizedVaadin(component));
        return this;
    }

    @Override
    @RequiredUIAccess
    public ThreeComponentSplitLayout setSecondComponent(@Nullable Component component) {
        toVaadinComponent().setSecond(toSizedVaadin(component));
        return this;
    }

    private static com.vaadin.flow.component.@Nullable Component toSizedVaadin(@Nullable Component component) {
        if (component == null) {
            return null;
        }

        com.vaadin.flow.component.Component vComponent = TargetVaadin.to(component);
        ((HasSize) vComponent).setSizeFull();
        return vComponent;
    }
}
