/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
class DesktopValueListenerAsItemListenerImpl<E> implements ItemListener {
    private final ValueComponent<E> myComponent;
    private final ComponentEventListener<ValueComponent<E>, ValueComponentEvent<E>> myValueListener;
    private final boolean mySelectEvent;

    public DesktopValueListenerAsItemListenerImpl(ValueComponent<E> component,
                                                  ComponentEventListener<ValueComponent<E>, ValueComponentEvent<E>> valueListener,
                                                  boolean selectEvent) {
        myComponent = component;
        myValueListener = valueListener;
        mySelectEvent = selectEvent;
    }

    @RequiredUIAccess
    @Override
    @SuppressWarnings("unchecked")
    public void itemStateChanged(ItemEvent e) {
        if (mySelectEvent) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                myValueListener.onEvent(new ValueComponentEvent<>(myComponent, myComponent.getValue()));
            }
        }
        else {
            if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
                myValueListener.onEvent(new ValueComponentEvent<>(myComponent, myComponent.getValue()));
            }
        }
    }
}
