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
package consulo.desktop.awt.ui.impl;

import consulo.ui.model.FlatDataModel;
import consulo.ui.model.FlatDataModelEvent;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopFlatDataModelWrapper<E> extends AbstractListModel<E> implements ComboBoxModel<E> {
    private final FlatDataModel<E> myModel;
    private Object mySelectedItem;

    public DesktopFlatDataModelWrapper(FlatDataModel<E> model) {
        myModel = model;

        model.addListener(event -> {
            int from = event.getFromIndex();
            int to = event.getToIndex();

            switch (event.getType()) {
                case ADDED -> fireIntervalAdded(this, from, to);
                case REMOVED -> fireIntervalRemoved(this, from, to);
                case UPDATED -> fireContentsChanged(this, from, to);
                case RESET -> fireContentsChanged(this, -1, -1);
            }
        });
    }

    @Override
    public int getSize() {
        return myModel.getSize();
    }

    @Override
    public E getElementAt(int index) {
        return myModel.get(index);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (mySelectedItem != null && !mySelectedItem.equals(anItem) || mySelectedItem == null && anItem != null) {
            mySelectedItem = anItem;
            fireContentsChanged(this, -1, -1);
        }
    }

    @Override
    public Object getSelectedItem() {
        return mySelectedItem;
    }
}
