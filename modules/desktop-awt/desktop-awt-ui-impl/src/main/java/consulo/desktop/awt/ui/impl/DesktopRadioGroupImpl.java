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
package consulo.desktop.awt.ui.impl;

import consulo.ui.RadioButton;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.internal.BaseRadioGroup;

import javax.swing.*;

/**
 * Swing announces buttons as one set to a screen reader, and walks between them with the arrow keys, only for the
 * ones it was handed as a group of its own.
 *
 * @author VISTALL
 * @since 03-May-17
 */
public class DesktopRadioGroupImpl<V> extends BaseRadioGroup<V> {
    private final ButtonGroup myGroup = new ButtonGroup();

    @Override
    protected void attach(RadioButton button) {
        myGroup.add((AbstractButton) TargetAWT.to(button));
    }
}
