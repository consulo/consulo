/*
 * Copyright 2013-2020 consulo.io
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

import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import consulo.ui.Component;
import consulo.ui.ProgressBar;
import consulo.ui.ProgressBarStyle;
import consulo.ui.impl.ProgressBarPresentation;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public class WebProgressBarImpl extends VaadinComponentDelegate<WebProgressBarImpl.Vaadin> implements ProgressBar {
    public class Vaadin extends com.vaadin.flow.component.progressbar.ProgressBar implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebProgressBarImpl.this;
        }
    }

    /**
     * The awt bar builds a spinner as a component of its own. A vaadin bar cannot be exchanged for one after it
     * exists, so the element stays and the style turns it round - the parts it is drawn from are the same.
     */
    private final Set<ProgressBarStyle> myStyles = EnumSet.noneOf(ProgressBarStyle.class);

    @Override
    public void addStyle(ProgressBarStyle style) {
        if (style.isTone() || style.isHeight()) {
            ProgressBarPresentation.add(myStyles, style);
            applyToneAndHeight();
            return;
        }

        switch (style) {
            case SPINNER -> getVaadinComponent().addClassName("consulo-progress-spinner");
            case TRANSPARENT_BACKGROUND -> getVaadinComponent().addClassName("consulo-progress-transparent");
        }
    }

    /**
     * Success and error are the bar's own theme variants. Vaadin has no warning variant, so that one fills the value
     * part through webProgressBar.css. The height is the one the bar part is drawn with.
     */
    private void applyToneAndHeight() {
        Vaadin vaadin = getVaadinComponent();

        vaadin.removeThemeVariants(ProgressBarVariant.SUCCESS, ProgressBarVariant.ERROR);
        vaadin.removeClassName("consulo-progress-toned");
        vaadin.getStyle().remove("--consulo-progress-value-color");
        if (myStyles.contains(ProgressBarStyle.SUCCESS)) {
            vaadin.addThemeVariants(ProgressBarVariant.SUCCESS);
        }
        else if (myStyles.contains(ProgressBarStyle.ERROR)) {
            vaadin.addThemeVariants(ProgressBarVariant.ERROR);
        }
        else {
            String color = WebColors.toCssColor(ProgressBarPresentation.valueColor(myStyles));
            if (color != null) {
                vaadin.addClassName("consulo-progress-toned");
                vaadin.getStyle().set("--consulo-progress-value-color", color);
            }
        }

        int height = ProgressBarPresentation.barHeight(myStyles);
        if (height > 0) {
            vaadin.getStyle().set("--vaadin-progress-bar-height", height + "px");
        }
        else {
            vaadin.getStyle().remove("--vaadin-progress-bar-height");
        }
    }

    /**
     * The range starts as 0..100 and values are taken as they come, the way the desktop bars do - the vaadin default of
     * 0..1 with every value divided by a hundred drew a bar of 0..100 at a hundredth of its value.
     */
    @Override
    public Vaadin createVaadinComponent() {
        Vaadin vaadin = new Vaadin();
        vaadin.setMin(0);
        vaadin.setMax(100);
        return vaadin;
    }

    @Override
    public void setIndeterminate(boolean value) {
        getVaadinComponent().setIndeterminate(value);
    }

    @Override
    public boolean isIndeterminate() {
        return getVaadinComponent().isIndeterminate();
    }

    @Override
    public void setMinimum(int value) {
        getVaadinComponent().setMin(value);
    }

    @Override
    public void setMaximum(int value) {
        getVaadinComponent().setMax(value);
    }

    @Override
    public void setValue(int value) {
        getVaadinComponent().setValue(value);
    }
}
