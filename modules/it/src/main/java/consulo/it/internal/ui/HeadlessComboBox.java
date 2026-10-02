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

import consulo.ui.ComboBox;
import consulo.ui.model.FlatDataModel;

/**
 * Dummy-but-creatable headless {@link ComboBox}.
 *
 * @author VISTALL
 */
public class HeadlessComboBox<E> extends HeadlessComboBoxBase<E, E> implements ComboBox<E> {
    public HeadlessComboBox(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    public void setValueByIndex(int index) {
    }
}
