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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class HeadlessOptionButton extends HeadlessButton implements OptionButton {
    private List<OptionButtonItem> myOptions = List.of();

    public HeadlessOptionButton(LocalizeValue text) {
        super(text);
    }

    @Override
    public List<OptionButtonItem> getOptions() {
        return myOptions;
    }

    @Override
    public void setOptions(List<OptionButtonItem> options) {
        myOptions = List.copyOf(options);
    }

    @Override
    public void showOptions() {
    }
}
