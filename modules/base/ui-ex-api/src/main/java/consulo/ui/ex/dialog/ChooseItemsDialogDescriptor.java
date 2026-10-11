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
package consulo.ui.ex.dialog;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.ListBox;
import consulo.ui.MultiSelectListBox;
import consulo.ui.TextItemPresentation;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class ChooseItemsDialogDescriptor<T> extends DialogDescriptor {
    private final List<T> myItems;
    private final LocalizeValue myDescription;
    private final BiConsumer<T, TextItemPresentation> myRenderer;
    private final boolean myMultipleSelect;
    private final List<T> mySelected;

    private @Nullable ValueComponent<?> myList;

    public ChooseItemsDialogDescriptor(
        LocalizeValue title,
        LocalizeValue description,
        List<T> items,
        BiConsumer<T, TextItemPresentation> renderer,
        boolean multipleSelect,
        List<T> selected
    ) {
        super(title);
        myItems = List.copyOf(items);
        myDescription = description;
        myRenderer = renderer;
        myMultipleSelect = multipleSelect;
        mySelected = List.copyOf(selected);
    }

    @RequiredUIAccess
    public static <T> CompletableFuture<List<T>> showAsync(
        @Nullable Component parent,
        LocalizeValue title,
        LocalizeValue description,
        List<T> items,
        BiConsumer<T, TextItemPresentation> renderer,
        boolean multipleSelect,
        List<T> selected
    ) {
        ChooseItemsDialogDescriptor<T> descriptor =
            new ChooseItemsDialogDescriptor<>(title, description, items, renderer, multipleSelect, selected);

        DialogService dialogService = Application.get().getInstance(DialogService.class);
        Dialog dialog = parent == null ? dialogService.build(descriptor) : dialogService.build(parent, descriptor);
        return dialog.showAsync().thenApply(descriptor::getChosenItems);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        Component list;
        if (myMultipleSelect) {
            MultiSelectListBox<T> listBox = MultiSelectListBox.create(myItems);
            listBox.setRender((presentation, item) -> render(presentation, item.getValue()));
            listBox.setValue(mySelected);
            myList = listBox;
            list = listBox;
        }
        else {
            ListBox<T> listBox = ListBox.create(myItems);
            listBox.setRender((presentation, item) -> render(presentation, item.getValue()));
            listBox.setValue(mySelected.isEmpty() ? null : mySelected.get(0));
            myList = listBox;
            list = listBox;
        }

        DockLayout layout = DockLayout.create();
        if (myDescription.isNotEmpty()) {
            layout.top(HtmlLabel.create(myDescription));
        }
        layout.center(ScrollableLayout.create(list));
        return layout;
    }

    private void render(TextItemPresentation presentation, @Nullable T item) {
        if (item != null) {
            myRenderer.accept(item, presentation);
        }
    }

    @SuppressWarnings("unchecked")
    private List<T> getChosenItems(@Nullable DialogValue value) {
        ValueComponent<?> list = myList;
        if (list == null) {
            return List.of();
        }

        Object selected = list.getValue();
        if (selected instanceof List<?> selectedList) {
            return new ArrayList<>((List<T>) selectedList);
        }
        return selected == null ? List.of() : List.of((T) selected);
    }
}
