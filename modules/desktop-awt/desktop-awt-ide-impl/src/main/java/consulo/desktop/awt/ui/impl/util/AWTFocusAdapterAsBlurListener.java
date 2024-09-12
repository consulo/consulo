/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.impl.util;

import consulo.ui.FocusableComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.BlurEvent;
import consulo.ui.event.ComponentEventListener;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * @author VISTALL
 * @since 2024-09-11
 */
public class AWTFocusAdapterAsBlurListener extends FocusAdapter {
    private final FocusableComponent myComponent;
    private final ComponentEventListener<FocusableComponent, BlurEvent> myBlurListener;

    public AWTFocusAdapterAsBlurListener(FocusableComponent component, ComponentEventListener<FocusableComponent, BlurEvent> blurListener) {
        myComponent = component;
        myBlurListener = blurListener;
    }

    @Override
    @RequiredUIAccess
    public void focusLost(FocusEvent e) {
        myBlurListener.onEvent(new BlurEvent(myComponent));
    }
}
