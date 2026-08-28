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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.slider.IntegerSlider;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.ColorPickerBuilder;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;
import org.vaadin.addons.tatu.ColorPicker;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-28
 */
public class WebColorPickerBuilderImpl implements ColorPickerBuilder {
    private static final int OPAQUE_ALPHA = 255;
    private static final int PERCENT_ALPHA = 100;

    private static final LocalizeValue ALPHA_LABEL = LocalizeValue.localizeTODO("Alpha");

    private LocalizeValue myTitle = LocalizeValue.empty();
    private @Nullable ColorValue myColor;
    private boolean myWithAlpha;
    private boolean myAlphaAsPercent;
    private @Nullable Consumer<ColorValue> myColorChangedConsumer;

    private @Nullable ColorValue myPickedColor;
    private int myPickedAlpha = OPAQUE_ALPHA;
    private boolean myAccepted;

    @Override
    public ColorPickerBuilder withTitle(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public ColorPickerBuilder withColor(@Nullable ColorValue color) {
        myColor = color;
        return this;
    }

    @Override
    public ColorPickerBuilder withAlpha() {
        myWithAlpha = true;
        return this;
    }

    @Override
    public ColorPickerBuilder withAlphaAsPercent() {
        myWithAlpha = true;
        myAlphaAsPercent = true;
        return this;
    }

    @Override
    public ColorPickerBuilder disableRecentColors() {
        return this;
    }

    @Override
    public ColorPickerBuilder disablePipette() {
        return this;
    }

    @Override
    public ColorPickerBuilder onColorChanged(Consumer<ColorValue> consumer) {
        myColorChangedConsumer = consumer;
        return this;
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<ColorValue> showAsync(@Nullable Window parent) {
        UIAccess.assertIsUIThread();

        CompletableFuture<ColorValue> result = new CompletableFuture<>();

        myAccepted = false;
        myPickedColor = myColor;
        myPickedAlpha = myColor == null ? OPAQUE_ALPHA : myColor.toRGB().getAlpha();

        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(false);
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setHeaderTitle(myTitle.get());

        dialog.add(createContent());
        dialog.getFooter().add(createCancelButton(dialog), createOkButton(dialog));

        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                result.complete(myAccepted ? currentColor() : null);

                dialog.getElement().removeFromParent();
            }
        });

        dialog.addDetachListener(event -> result.complete(null));

        UI.getCurrent().add(dialog);
        dialog.open();

        return result;
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setNoClear(true);
        colorPicker.setValue(toOpaqueCssColor(myPickedColor));
        colorPicker.addValueChangeListener(event -> {
            myPickedColor = WebColors.fromCssColor(event.getValue());

            fireColorChanged();
        });

        content.add(colorPicker);

        if (myWithAlpha) {
            content.add(createAlphaSlider());
        }

        return content;
    }

    private IntegerSlider createAlphaSlider() {
        int maxValue = myAlphaAsPercent ? PERCENT_ALPHA : OPAQUE_ALPHA;

        IntegerSlider slider = new IntegerSlider(ALPHA_LABEL.get(), 0, maxValue);
        slider.setValueAlwaysVisible(true);
        slider.setValue(myAlphaAsPercent ? Math.round(myPickedAlpha * (float) PERCENT_ALPHA / OPAQUE_ALPHA) : myPickedAlpha);
        slider.addValueChangeListener(event -> {
            Integer value = event.getValue();

            myPickedAlpha = value == null ? OPAQUE_ALPHA : toAlpha(value);

            fireColorChanged();
        });

        return slider;
    }

    private Button createOkButton(Dialog dialog) {
        Button button = new Button(CommonLocalize.buttonOk().get(), event -> {
            myAccepted = true;

            dialog.close();
        });
        button.addThemeVariants(ButtonVariant.PRIMARY);
        return button;
    }

    private Button createCancelButton(Dialog dialog) {
        Button button = new Button(CommonLocalize.buttonCancel().get(), event -> dialog.close());
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return button;
    }

    private int toAlpha(int sliderValue) {
        if (!myAlphaAsPercent) {
            return sliderValue;
        }
        return Math.round(sliderValue * (float) OPAQUE_ALPHA / PERCENT_ALPHA);
    }

    private @Nullable ColorValue currentColor() {
        if (myPickedColor == null) {
            return null;
        }
        return myWithAlpha ? myPickedColor.withAlpha(myPickedAlpha) : myPickedColor;
    }

    private void fireColorChanged() {
        ColorValue color = currentColor();

        if (myColorChangedConsumer != null && color != null) {
            myColorChangedConsumer.accept(color);
        }
    }

    private static String toOpaqueCssColor(@Nullable ColorValue color) {
        String cssColor = WebColors.toCssColor(color == null ? null : color.withAlpha(OPAQUE_ALPHA));
        return cssColor == null ? "" : cssColor;
    }
}
