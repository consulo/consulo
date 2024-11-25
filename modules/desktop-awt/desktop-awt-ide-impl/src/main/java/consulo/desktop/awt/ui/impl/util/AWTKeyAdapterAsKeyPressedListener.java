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

import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.KeyPressedEvent;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 2019-11-07
 */
public class AWTKeyAdapterAsKeyPressedListener extends KeyAdapter {
    private final Component myComponent;
    private final ComponentEventListener<Component, KeyPressedEvent> myKeyListener;

    public AWTKeyAdapterAsKeyPressedListener(Component component, ComponentEventListener<Component, KeyPressedEvent> keyListener) {
        myComponent = component;
        myKeyListener = keyListener;
    }

    @Override
    @RequiredUIAccess
    public void keyPressed(KeyEvent e) {
        myKeyListener.onEvent(new KeyPressedEvent(myComponent, (KeyboardInputDetails) DesktopAWTInputDetails.convert(TargetAWT.to(myComponent), e)));
    }
}
