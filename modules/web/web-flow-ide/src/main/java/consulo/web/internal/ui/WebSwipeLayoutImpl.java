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
package consulo.web.internal.ui;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.ui.layout.SwipeLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaadin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.VaadinSizeUtil;
import consulo.web.internal.ui.vaadin.carousel.Carousel;
import consulo.web.internal.ui.vaadin.carousel.Slide;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-15
 */
public class WebSwipeLayoutImpl extends VaadinComponentDelegate<WebSwipeLayoutImpl.Vaadin> implements SwipeLayout {
    public class Vaadin extends Carousel implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebSwipeLayoutImpl.this;
        }
    }

    private static class LayoutInfo {
        private final Supplier<Layout> myLayoutSupplier;

        private @Nullable Layout myLayout;
        private @Nullable Slide mySlide;

        LayoutInfo(Supplier<Layout> layoutSupplier) {
            myLayoutSupplier = layoutSupplier;
        }

        Layout get() {
            Layout layout = myLayout;
            if (layout == null) {
                myLayout = layout = myLayoutSupplier.get();
            }
            return layout;
        }
    }

    private final Map<String, LayoutInfo> myLayoutInfos = new HashMap<>();
    private final List<LayoutInfo> mySlideOrder = new ArrayList<>();

    private @Nullable LayoutInfo myShownInfo;

    public WebSwipeLayoutImpl() {
        Vaadin vaadin = getVaadinComponent();
        vaadin.setHideNavigation(true);
        vaadin.setDisableSwipe(true);
        vaadin.setAutoProgress(false);
        vaadin.setWidth("100%");
        vaadin.setHeight("100%");
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public SwipeLayout register(String id, @RequiredUIAccess Supplier<Layout> layoutSupplier) {
        LayoutInfo layoutInfo = new LayoutInfo(layoutSupplier);

        myLayoutInfos.put(id, layoutInfo);

        if (myShownInfo == null) {
            show(layoutInfo);
        }
        return this;
    }

    @Override
    public Layout swipeLeftTo(String id) {
        return show(info(id));
    }

    @Override
    public Layout swipeRightTo(String id) {
        return show(info(id));
    }

    private LayoutInfo info(String id) {
        LayoutInfo info = myLayoutInfos.get(id);
        if (info == null) {
            throw new IllegalArgumentException(id + " is not registered");
        }
        return info;
    }

    private Layout show(LayoutInfo layoutInfo) {
        Layout layout = layoutInfo.get();

        Vaadin vaadin = getVaadinComponent();

        if (layoutInfo.mySlide == null) {
            VaadinSizeUtil.setSizeFull(layout);

            Slide slide = new Slide(TargetVaadin.to(layout));
            layoutInfo.mySlide = slide;

            mySlideOrder.add(layoutInfo);

            vaadin.getElement().appendChild(slide.getElement());
            vaadin.setTotalSlides(mySlideOrder.size());
        }

        vaadin.setStartPosition(mySlideOrder.indexOf(layoutInfo));

        myShownInfo = layoutInfo;
        return layout;
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        myLayoutInfos.clear();
        mySlideOrder.clear();
        myShownInfo = null;

        Vaadin vaadin = getVaadinComponent();
        vaadin.getElement().removeAllChildren();
        vaadin.setTotalSlides(0);
    }

    @Override
    public void remove(Component component) {
        String id = null;
        for (Map.Entry<String, LayoutInfo> entry : myLayoutInfos.entrySet()) {
            if (entry.getValue().myLayout == component) {
                id = entry.getKey();
                break;
            }
        }

        if (id != null) {
            LayoutInfo info = myLayoutInfos.remove(id);

            if (myShownInfo == info) {
                myShownInfo = null;
            }

            Slide slide = info.mySlide;
            if (slide != null) {
                mySlideOrder.remove(info);

                slide.getElement().removeFromParent();
                getVaadinComponent().setTotalSlides(mySlideOrder.size());
            }
        }
    }
}
