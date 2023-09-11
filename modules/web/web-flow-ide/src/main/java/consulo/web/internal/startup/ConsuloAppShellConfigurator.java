/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.internal.startup;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;
import org.vaadin.stefan.table.Table;

/**
 * @author VISTALL
 * @since 28/05/2023
 */
@Theme("consulo")
@Push(PushMode.AUTOMATIC)
@Uses(Dialog.class)
@Uses(HorizontalLayout.class)
@Uses(Icon.class)
@Uses(VerticalLayout.class)
@Uses(Button.class)
@Uses(ListBox.class)
@Uses(ComboBox.class)
@Uses(Table.class)
@Uses(Scroller.class)
@Uses(SplitLayout.class)
@Uses(IntegerField.class)
@Uses(Checkbox.class)
@Uses(Select.class)
@Uses(Grid.class)
@Uses(Accordion.class)
@Uses(ContextMenu.class)
@Uses(MenuItem.class)
@Uses(MenuBar.class)
@Uses(RadioButtonGroup.class)
@Uses(Div.class)
@Uses(TreeGrid.class)
@Uses(TextField.class)
@Uses(TabSheet.class)
@Uses(TextArea.class)
public class ConsuloAppShellConfigurator implements AppShellConfigurator {
  @Override
  public void configurePage(AppShellSettings settings) {
  }
}
