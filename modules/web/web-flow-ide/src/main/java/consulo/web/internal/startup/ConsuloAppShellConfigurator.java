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
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
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
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.aura.Aura;
import consulo.web.internal.ui.WebFontRegistry;
import org.vaadin.stefan.table.Table;

/**
 * @author VISTALL
 * @since 28/05/2023
 */
@StyleSheet(Aura.STYLESHEET)
// not under /themes/ on purpose - that path is vaadin's theme-in-a-jar convention, and the flow plugin
// snapshots everything below it into prod.bundle, which is then reused without noticing the css changed
@StyleSheet("/consulo/aura-utility.css")
@StyleSheet("/consulo/styles.css")
@StyleSheet("/consulo/scrollbar.css")
@StyleSheet("/consulo/editor.css")
@StyleSheet("/consulo/tabs.css")
@StyleSheet("/consulo/statusbar.css")
@StyleSheet("/consulo/toolwindow.css")
// a progress bar lives only while its task runs, so its styles cannot be carried by the component itself
@StyleSheet("/progress/webProgressBar.css")
@JsModule("./webImage.js")
@JsModule("./shortcuts.js")
@JsModule("./treeToggle.js")
@Push(PushMode.AUTOMATIC)
@Uses(Dialog.class)
@Uses(Popover.class)
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
@Uses(Breadcrumbs.class)
@Uses(BreadcrumbsItem.class)
@Uses(RadioButtonGroup.class)
@Uses(Div.class)
@Uses(TreeGrid.class)
@Uses(TextField.class)
@Uses(PasswordField.class)
@Uses(TabSheet.class)
@Uses(TextArea.class)
@Uses(ProgressBar.class)
public class ConsuloAppShellConfigurator implements AppShellConfigurator {
  @Override
  public void configurePage(AppShellSettings settings) {
    // the ide draws its own popup menus everywhere, the browser one must never appear over them
    settings.addInlineWithContents(
      "document.addEventListener('contextmenu', event => event.preventDefault());",
      Inline.Wrapping.JAVASCRIPT
    );

    // built from the same list the font manager reports, a stylesheet of the theme would have to be kept in
    // step with it by hand
    settings.addInlineWithContents(WebFontRegistry.buildFontFaceCss(), Inline.Wrapping.STYLESHEET);
  }
}
