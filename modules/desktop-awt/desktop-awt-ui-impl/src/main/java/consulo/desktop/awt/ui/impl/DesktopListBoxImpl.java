/*
 * Copyright 2013-2017 consulo.io
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

import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBList;
import consulo.ui.model.FlatDataModel;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
class DesktopListBoxImpl<E> extends DesktopListBoxBaseImpl<E, E> implements ListBox<E> {
    private boolean mySelectOnHover;

    public DesktopListBoxImpl(FlatDataModel<E> model) {
        super(model);
    }

    @Override
    protected ListSelectionModel createSelectionModel(JBList<E> component) {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1) {
                int index = findSelectableIndex(component.getModel(), index0, getLeadSelectionIndex());
                if (index >= 0) {
                    super.setSelectionInterval(index, index);
                }
            }
        };
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return selectionModel;
    }

    @Override
    protected boolean isSelectOnHover() {
        return mySelectOnHover;
    }

    @Override
    public void setSelectOnHover(boolean selectOnHover) {
        mySelectOnHover = selectOnHover;
    }

    @Override
    public void setValueByIndex(int index) {
        toAWTComponent().setSelectedIndex(index);
    }

    @RequiredUIAccess
    @Override
    public void setValue(E value, boolean fireListeners) {
        toAWTComponent().setSelectedValue(value, true);
    }

    @Override
    public E getValue() {
        return toAWTComponent().getSelectedValue();
    }
}
