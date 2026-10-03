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
import consulo.ui.Menu;
import consulo.ui.MenuItem;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 */
public class HeadlessMenu extends HeadlessMenuItem implements Menu {
    private final List<MenuItem> myItems = new CopyOnWriteArrayList<>();

    public HeadlessMenu(LocalizeValue text) {
        super(text);
    }

    @Override
    public Menu add(MenuItem menuItem) {
        myItems.add(menuItem);
        return this;
    }

    public List<MenuItem> getItems() {
        return List.copyOf(myItems);
    }
}
