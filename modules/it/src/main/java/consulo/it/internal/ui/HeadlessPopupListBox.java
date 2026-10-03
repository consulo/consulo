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
import consulo.ui.ex.internal.InlineButton;
import consulo.ui.ex.internal.InlineButtonsList;
import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 */
public class HeadlessPopupListBox<E> extends HeadlessListBox<E> implements InlineButtonsList<E> {
    private @Nullable Function<E, List<InlineButton>> myInlineButtons;
    private int myActiveInlineButton = -1;

    public HeadlessPopupListBox(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    @RequiredUIAccess
    public void setInlineButtons(Function<E, List<InlineButton>> buttons) {
        myInlineButtons = buttons;
    }

    @Override
    @RequiredUIAccess
    public void setActiveInlineButton(int index) {
        myActiveInlineButton = index;
    }

    @RequiredUIAccess
    public void click() {
        MouseInputDetails details = new MouseInputDetails(new Point2D(),
            new Point2D(),
            EnumSet.noneOf(ModifiedInputDetails.Modifier.class),
            MouseInputDetails.MouseButton.LEFT);
        getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, details));
    }

    public List<InlineButton> getInlineButtons(E item) {
        Function<E, List<InlineButton>> buttons = myInlineButtons;
        return buttons == null ? List.of() : buttons.apply(item);
    }

    public int getActiveInlineButton() {
        return myActiveInlineButton;
    }
}
