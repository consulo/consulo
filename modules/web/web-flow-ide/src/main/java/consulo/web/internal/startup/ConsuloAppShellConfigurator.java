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
import com.flowingcode.vaadin.addons.xterm.ConsuloPtyTerm;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.virtuallist.VirtualList;
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
import com.vaadin.flow.component.slider.IntegerSlider;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.componentfactory.ToggleButton;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.aura.Aura;
import consulo.web.ui.impl.internal.WebFontRegistry;
import consulo.web.ui.impl.internal.WebStyleCssRegistry;
import consulo.web.ui.impl.internal.vaadin.carousel.Carousel;
import consulo.web.ui.impl.internal.vaadin.carousel.Slide;
import org.vaadin.addons.tatu.ColorPicker;
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
@JsModule("./showNotifier.js")
@JsModule("./treeToggle.js")
@Push(PushMode.AUTOMATIC)
@Uses(Dialog.class)
@Uses(Carousel.class)
@Uses(Slide.class)
@Uses(ColorPicker.class)
@Uses(ConsuloPtyTerm.class)
@Uses(Popover.class)
@Uses(HorizontalLayout.class)
@Uses(Icon.class)
@Uses(VerticalLayout.class)
@Uses(Button.class)
@Uses(ListBox.class)
@Uses(VirtualList.class)
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
@Uses(Details.class)
@Uses(DatePicker.class)
@Uses(IntegerSlider.class)
@Uses(ToggleButton.class)
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

    // every style is emitted at once, the active one is selected by the attribute set on <html>, so switching
    // a style does not have to push a stylesheet to a running ui
    settings.addInlineWithContents(WebStyleCssRegistry.buildStyleCss(), Inline.Wrapping.STYLESHEET);

    // a vaadin component keeps its scroller inside its shadow root, and a ::-webkit-scrollbar rule of the
    // document does not cross that boundary - those scrollers keep the scrollbar of the browser, arrow buttons
    // and all. the rules of scrollbar.css are adopted into every shadow root instead, which is why they are
    // read back out of it rather than written twice. inlined rather than shipped as a module so that the
    // patch is installed before the components define their roots
    settings.addInlineWithContents(
      """
      (() => {
          const sheet = new CSSStyleSheet();

          const adopt = root => {
              if (!root.adoptedStyleSheets.includes(sheet)) {
                  root.adoptedStyleSheets = [...root.adoptedStyleSheets, sheet];
              }
          };

          const attachShadow = Element.prototype.attachShadow;
          Element.prototype.attachShadow = function (init) {
              const root = attachShadow.call(this, init);
              if (root.mode === 'open') {
                  adopt(root);
              }
              return root;
          };

          const adoptExisting = node => {
              for (const element of node.querySelectorAll('*')) {
                  if (element.shadowRoot) {
                      adopt(element.shadowRoot);
                      adoptExisting(element.shadowRoot);
                  }
              }
          };

          // the text of the same file the document is styled with, so the two cannot drift. a sheet already
          // adopted by a root updates in place when it is replaced, so filling it in late is fine
          fetch('/consulo/scrollbar.css')
              .then(response => response.text())
              .then(text => sheet.replaceSync(text));

          // a component whose class was defined before the patch above may hand out a root the patch never
          // saw, and flow builds the whole ui long after the document is loaded - so the tree is swept again
          // whenever it grows, coalesced into one pass per frame
          let sweeping = false;
          const sweep = () => {
              if (sweeping) {
                  return;
              }
              sweeping = true;
              requestAnimationFrame(() => {
                  sweeping = false;
                  adoptExisting(document);
              });
          };

          new MutationObserver(sweep).observe(document.documentElement, { childList: true, subtree: true });

          document.documentElement.setAttribute('consulo-scrollbar', 'on');
      })();
      """,
      Inline.Wrapping.JAVASCRIPT
    );
  }
}
