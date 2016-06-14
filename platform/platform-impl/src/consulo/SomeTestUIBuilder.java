/*
 * Copyright 2013-2016 must-be.org
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
package consulo;

import consulo.ui.*;
import consulo.ui.hack.IconWithURL;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class SomeTestUIBuilder {
  @RequiredUIThread
  public static void build(Window window) {
    /*VerticalLayout layout = Layouts.vertical();

    final CheckBox top = create("top");
    top.setEnabled(false);
    layout.add(top);
    final CheckBox left = create("left");
    layout.add(left);
    final CheckBox right = create("right (this item will blink every 5 sec)");
    layout.add(right);
    final CheckBox bottom = create("bottom");
    layout.add(bottom);

    final CheckBox center = Components.checkBox("UI proxy?=center", false);
    center.addValueListener(new ValueComponent.ValueListener<Boolean>() {
      @Override
      public void valueChanged(@NotNull ValueComponent.ValueEvent<Boolean> event) {
        top.setValue(event.getValue());
        left.setValue(event.getValue());
        right.setValue(event.getValue());
        bottom.setValue(event.getValue());
        bottom.setVisible(!event.getValue());
      }
    });

    final UIAccess uiAccess = UIAccess.get();

    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        uiAccess.give(new Runnable() {
          @Override
          public void run() {
            right.setValue(!right.getValue());
          }
        });
      }
    }, 5, 5, TimeUnit.SECONDS);

    center.setValue(true);
    layout.add(center);
    layout.add(Layouts.horizontal().add(Components.checkBox("Test 1")).add(Components.checkBox("Test 2")));
    layout.add(Layouts.horizontal().add(Components.label("Test 1")).add(Components.label("Test 2")));
    layout.add(Layouts.horizontal().add(Components.htmlLabel("<b>Test 1</b>")).add(Components.label("<b>Test 1</b>")));
    final ComboBox<String> comboBox = Components.comboBox("test", "test2");
    comboBox.setRender(new ListItemRender<String>() {
      @Override
      public void render(@NotNull ListItemPresentation render, int index, @Nullable String item) {
        if (item == null) {
          render.append(fromIcon(AllIcons.Actions.Help));
          render.append("<null>");
          return;
        }

        if (item.equals("test2")) {
          render.append(fromIcon(AllIcons.Actions.IntentionBulb));
          render.append(item, TextStyle.BOLD);
        }
        else {
          render.append(fromIcon(AllIcons.Nodes.Class), fromIcon(AllIcons.Nodes.JunitTestMark));

          render.append(item);
        }
      }
    });
    final SplitLayout splitLayout = Layouts.horizontalSplit();

    splitLayout.setFirstComponent(Layouts.dock().left(Components.label("SDK:")).center(comboBox));
    final ComboBox<String> component = Components.comboBox("test1", "tet2");
    component.addValueListener(new ValueComponent.ValueListener<String>() {
      @Override
      public void valueChanged(@NotNull ValueComponent.ValueEvent<String> event) {
        System.out.println(event.getValue() + " selected");
      }
    });
    component.setValue("tet2");
    splitLayout.setSecondComponent(Layouts.horizontal().add(Components.label("SDK:")).add(component));

    splitLayout.setProportion(20);

    layout.add(splitLayout);   */

    try {
      Thread.sleep(1000L);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    final Menu file = MenuItems.menu("File");
    file.add(MenuItems.menu("New").add(MenuItems.item("Class")));
    file.separate();
    file.add(MenuItems.item("Exit"));

    window.setMenuBar(MenuItems.menuBar().add(file).add(MenuItems.menu("Help")));

    final SplitLayout splitLayout = Layouts.horizontalSplit();
    final TabbedLayout tabbed = Layouts.tabbed();
    tabbed.addTab("Hello", Components.label("test"));
    tabbed.addTab("Hello2", Components.label("test 1"));

    splitLayout.setFirstComponent(Components.label("tree"));
    splitLayout.setSecondComponent(tabbed);
    splitLayout.setProportion(20);

    window.setContent(splitLayout);
  }

  private static CheckBox create(String text) {
    return Components.checkBox("UI proxy?=" + text, true);
  }

  public static ImageRef fromIcon(Icon icon) {
    if (icon instanceof IconWithURL) {
      return ImageRefs.fromURL(((IconWithURL)icon).getURL());
    }
    throw new UnsupportedOperationException();
  }
}
