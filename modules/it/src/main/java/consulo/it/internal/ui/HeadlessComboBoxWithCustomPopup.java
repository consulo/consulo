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

import consulo.ui.Point2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.model.FlatDataModel;

import java.util.EnumSet;

/**
 * @author VISTALL
 */
public class HeadlessComboBoxWithCustomPopup<E> extends HeadlessComboBox<E> implements ComboBoxWithCustomPopup<E> {
    public HeadlessComboBoxWithCustomPopup(FlatDataModel<E> model) {
        super(model);
    }

    @RequiredUIAccess
    public void click() {
        MouseInputDetails details = new MouseInputDetails(new Point2D(),
            new Point2D(),
            EnumSet.noneOf(ModifiedInputDetails.Modifier.class),
            MouseInputDetails.MouseButton.LEFT);
        getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, details));
    }
}
