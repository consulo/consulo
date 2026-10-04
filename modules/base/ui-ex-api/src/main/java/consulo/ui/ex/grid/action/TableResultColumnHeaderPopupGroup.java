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
package consulo.ui.ex.grid.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * The context menu of the column headers of a data grid: its column actions work on the column the menu was opened on. It is not a
 * popup group, so a data source which shows a menu of its own adds this group to it flat.
 *
 * @since 2026-10-04
 */
@ActionImpl(
    id = TableResultColumnHeaderPopupGroup.ID,
    children = {
        @ActionRef(type = SetFirstRowIsHeaderAction.class),
        @ActionRef(type = RenameColumnAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = AddColumnAction.class),
        @ActionRef(type = InsertColumnBeforeAction.class),
        @ActionRef(type = InsertColumnAfterAction.class),
        @ActionRef(type = CloneColumnAction.class),
        @ActionRef(type = DeleteColumnsAction.class)
    }
)
public class TableResultColumnHeaderPopupGroup extends DefaultActionGroup implements DumbAware {
    public static final String ID = "Console.TableResult.ColumnHeaderPopup";

    public TableResultColumnHeaderPopupGroup() {
        super(LocalizeValue.localizeTODO("Results Header"), false);
    }
}
