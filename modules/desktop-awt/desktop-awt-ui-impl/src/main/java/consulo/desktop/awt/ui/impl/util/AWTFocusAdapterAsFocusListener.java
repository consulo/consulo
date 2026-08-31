/*
 * Copyright 2013-2019 consulo.io
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

import consulo.ui.HasFocus;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * @author VISTALL
 * @since 2019-11-09
 */
public class AWTFocusAdapterAsFocusListener extends FocusAdapter {
    private final HasFocus myComponent;
    private final ComponentEventListener<HasFocus, consulo.ui.event.FocusEvent> myFocusListener;

    public AWTFocusAdapterAsFocusListener(HasFocus component,
                                          ComponentEventListener<HasFocus, consulo.ui.event.FocusEvent> focusListener) {
        myComponent = component;
        myFocusListener = focusListener;
    }

    @Override
    @RequiredUIAccess
    public void focusGained(FocusEvent e) {
        myFocusListener.onEvent(new consulo.ui.event.FocusEvent(myComponent));
    }
}
