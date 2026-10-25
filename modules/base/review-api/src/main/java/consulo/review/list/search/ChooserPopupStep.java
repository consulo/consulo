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
package consulo.review.list.search;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The items of a chooser and, while they load or failed to, a {@link ChooserStatusRow} after them. A multiple chooser
 * toggles the chosen item and stays open.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
final class ChooserPopupStep<T> extends BaseListPopupStep<Object> {
    private final MutableFlatDataModel<Object> myModel;
    private final Function<T, String> myFilteringMapper;
    private final @Nullable MultiChooserListModel<T> myChooserModel;
    private final Consumer<T> myOnChosen;

    ChooserPopupStep(LocalizeValue title,
                     MutableFlatDataModel<Object> model,
                     Function<T, String> filteringMapper,
                     @Nullable MultiChooserListModel<T> chooserModel,
                     Consumer<T> onChosen) {
        super(title.getNullIfEmpty(), model);
        myModel = model;
        myFilteringMapper = filteringMapper;
        myChooserModel = chooserModel;
        myOnChosen = onChosen;
    }

    void setItems(List<T> items, @Nullable ChooserStatusRow status) {
        List<Object> values = new ArrayList<>(items);
        if (status != null) {
            values.add(status);
        }
        myModel.replaceAll(values);
    }

    @Override
    public boolean isSpeedSearchEnabled() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getTextFor(Object value) {
        if (value instanceof ChooserStatusRow) {
            return "";
        }
        return myFilteringMapper.apply((T) value);
    }

    @Override
    public int getDefaultOptionIndex() {
        if (myModel.getSize() == 0 || myModel.get(0) instanceof ChooserStatusRow) {
            return -1;
        }
        return super.getDefaultOptionIndex();
    }

    @Override
    public boolean isSelectable(Object value) {
        return !(value instanceof ChooserStatusRow);
    }

    @Override
    public KeepPopupOnPerform getKeepPopupOnPerform(Object value) {
        return myChooserModel != null && !(value instanceof ChooserStatusRow) ? KeepPopupOnPerform.ALWAYS : KeepPopupOnPerform.NEVER;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable PopupStep onChosen(Object selectedValue, boolean finalChoice) {
        if (selectedValue instanceof ChooserStatusRow) {
            return FINAL_CHOICE;
        }

        T value = (T) selectedValue;
        MultiChooserListModel<T> chooserModel = myChooserModel;
        if (chooserModel != null) {
            chooserModel.toggleChosen(value);
            myModel.update(selectedValue);
            return FINAL_CHOICE;
        }

        myOnChosen.accept(value);
        return FINAL_CHOICE;
    }
}
