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
package consulo.ui;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.details.InputDetails;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
public interface Button extends Component, HasComponentStyle<ButtonStyle> {
    @Deprecated
    @DeprecationInfo("Use #create(LocalizeValue)")
    static Button create(String text) {
        return create(LocalizeValue.of(text));
    }
    @Deprecated
    @DeprecationInfo("Use #create(LocalizeValue, ComponentEventListener<Component, ClickEvent>)")
    static Button create(String text, ComponentEventListener<Component, ClickEvent> clickListener) {
        return create(LocalizeValue.of(text), clickListener);
    }
    static Button create(LocalizeValue text) {
        return UIInternal.get()._Components_button(text);
    }
    static Button create(LocalizeValue text, ComponentEventListener<Component, ClickEvent> clickListener) {
        Button button = create(text);
        button.addClickListener(clickListener);
        return button;
    }
    LocalizeValue getText();

    @RequiredUIAccess
    void setText(LocalizeValue text);

    @Nullable
    Image getIcon();

    @RequiredUIAccess
    void setIcon(@Nullable Image image);

    void invoke(InputDetails inputDetails);
}
