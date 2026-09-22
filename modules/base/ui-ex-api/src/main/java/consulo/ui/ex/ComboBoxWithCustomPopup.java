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
package consulo.ui.ex;

import consulo.ui.ComboBox;
import consulo.ui.ex.internal.UIInternalEx;
import consulo.ui.model.FlatDataModel;

/**
 * A combo box which never opens the list of its items. A press is reported as a click, so whoever placed the control
 * decides what drops down.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public interface ComboBoxWithCustomPopup<E> extends ComboBox<E> {
    static <E> ComboBoxWithCustomPopup<E> create(FlatDataModel<E> model) {
        return UIInternalEx.get()._Components_comboBoxWithCustomPopup(model);
    }
}
