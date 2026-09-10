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

import consulo.ui.RadioButton;
import consulo.ui.internal.BaseRadioGroup;

import java.util.UUID;

/**
 * The browser already knows how to keep radios of one name exclusive, and how to walk them with the arrow keys,
 * so the buttons of a group are given a name of their own and it does the rest.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public class WebRadioGroupImpl<V> extends BaseRadioGroup<V> {
    private final String myName = UUID.randomUUID().toString();

    @Override
    protected void attach(RadioButton button) {
        if (button instanceof WebRadioButtonImpl webButton) {
            webButton.setGroupName(myName);
        }
    }
}
