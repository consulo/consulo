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
package consulo.ui.event;

import consulo.ui.Table;
import consulo.ui.event.details.InputDetails;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Carries a list rather than one value, since {@code SelectionMode.MULTIPLE} exists.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public final class TableSelectEvent<Item> extends ComponentEvent<Table<Item>> {
    private final List<Item> myValues;

    public TableSelectEvent(Table<Item> component, List<Item> values) {
        this(component, values, null);
    }

    public TableSelectEvent(Table<Item> component, List<Item> values, @Nullable InputDetails inputDetails) {
        super(component, inputDetails);
        myValues = values;
    }

    public List<Item> getValues() {
        return myValues;
    }

    public @Nullable Item getValue() {
        return myValues.isEmpty() ? null : myValues.get(0);
    }
}
