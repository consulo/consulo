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
package consulo.ide.impl.wm.impl;

import consulo.ide.impl.idea.openapi.wm.impl.status.widget.StatusBarWidgetWrapper;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.PseudoComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.image.Image;
import consulo.ui.layout.WrappedLayout;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class UnifiedStatusBarWidgetWrapper {
    @RequiredUIAccess
    public static PseudoComponent wrap(StatusBarWidget widget) {
        StatusBarWidget.WidgetPresentation presentation = widget.getPresentation();

        if (presentation instanceof StatusBarWidget.TextPresentation textPresentation) {
            return new LabelWidget(widget, presentation, textPresentation::getText, () -> null);
        }

        // the awt wrapper renders these as a label too, only the source of the text and of the icon differs
        if (presentation instanceof StatusBarWidget.MultipleTextValuesPresentation valuesPresentation) {
            return new LabelWidget(widget, presentation, valuesPresentation::getSelectedValue, valuesPresentation::getIcon);
        }

        if (presentation instanceof StatusBarWidget.IconPresentation iconPresentation) {
            return new LabelWidget(widget, presentation, () -> "", iconPresentation::getIcon);
        }

        return new DummyWidget(widget);
    }

    private static class DummyWidget implements PseudoComponent {
        private final WrappedLayout myLayout;

        @RequiredUIAccess
        public DummyWidget(StatusBarWidget widget) {
            String id = widget.getId();

            Label label = Label.create(id.substring(0, 2));
            myLayout = pad(label);
        }

        @RequiredUIAccess
        @Override
        public Component getComponent() {
            return myLayout;
        }
    }

    /**
     * Every presentation the unified frame can render comes down to a label with an optional icon. The value is
     * pulled on each {@link #beforeUpdate()}, which the status bar calls whenever a widget asks for a refresh.
     */
    private static class LabelWidget implements PseudoComponent, StatusBarWidgetWrapper {
        private final StatusBarWidget myWidget;
        private final StatusBarWidget.WidgetPresentation myPresentation;
        private final TextSupplier myTextSupplier;
        private final IconSupplier myIconSupplier;

        private final Label myLabel;
        private final WrappedLayout myWrappedLayout;

        @RequiredUIAccess
        public LabelWidget(StatusBarWidget widget, StatusBarWidget.WidgetPresentation presentation, TextSupplier text, IconSupplier icon) {
            myWidget = widget;
            myPresentation = presentation;
            myTextSupplier = text;
            myIconSupplier = icon;

            myLabel = Label.create(LocalizeValue.empty());
            myWrappedLayout = pad(myLabel);
        }

        @Override
        public StatusBarWidget.WidgetPresentation getPresentation() {
            return myPresentation;
        }

        @RequiredUIAccess
        @Override
        public void beforeUpdate() {
            myWidget.beforeUpdate();

            String text = StringUtil.notNullize(myTextSupplier.get());
            Image icon = myIconSupplier.get();

            myLabel.setText(LocalizeValue.of(text));
            myLabel.setImage(icon);
            myLabel.setToolTipText(myPresentation.getTooltipText());
            // the awt wrapper is the widget panel itself, so hiding it drops its padding too
            myWrappedLayout.setVisible(!text.isEmpty() || icon != null);
        }

        @RequiredUIAccess
        @Override
        public Component getComponent() {
            return myWrappedLayout;
        }
    }

    private interface TextSupplier {
        @RequiredUIAccess
        @Nullable
        String get();
    }

    private interface IconSupplier {
        @RequiredUIAccess
        @Nullable
        Image get();
    }

    @RequiredUIAccess
    private static WrappedLayout pad(Component component) {
        WrappedLayout layout = WrappedLayout.create(component);
        layout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 4);
        layout.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, null, 4);
        return layout;
    }
}
