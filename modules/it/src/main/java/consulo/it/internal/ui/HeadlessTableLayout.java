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

import consulo.ui.Component;
import consulo.ui.StaticPosition;
import consulo.ui.layout.TableLayout;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 */
public class HeadlessTableLayout extends HeadlessLayoutBase<TableLayout.TableCell> implements TableLayout {
    private final StaticPosition myFillOption;
    private final Map<Component, TableCell> myCells = new ConcurrentHashMap<>();

    public HeadlessTableLayout(StaticPosition fillOption) {
        myFillOption = fillOption;
    }

    public StaticPosition getFillOption() {
        return myFillOption;
    }

    public @Nullable TableCell getCell(Component component) {
        return myCells.get(component);
    }

    @Override
    public TableLayout add(Component component, TableCell constraint) {
        if (myCells.put(component, constraint) != null) {
            super.remove(component);
        }
        addChild(component);
        return this;
    }

    @Override
    public void remove(Component component) {
        myCells.remove(component);
        super.remove(component);
    }

    @Override
    public void removeAll() {
        myCells.clear();
        super.removeAll();
    }
}
