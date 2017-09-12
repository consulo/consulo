/*
 * Copyright 2013-2016 consulo.io
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

import com.intellij.icons.AllIcons;
import consulo.ui.*;
import consulo.ui.image.Images;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class SomeTestUIBuilder {
  @RequiredUIAccess
  public static void buildTabbed(Window window) {
    DockLayout dock = Layouts.dock();

    TabbedLayout tabbed = Layouts.tabbed();

    Tab okey = tabbed.addTab("Test Me", Components.label("Okey"));
    okey.setIcon(AllIcons.Nodes.Class);
    okey.setCloseHandler((tab, component) -> {
    });

    DockLayout tabDock = Layouts.dock();
    build(tabDock);
    tabbed.addTab("Unactive", tabDock);

    dock.top(Components.label("Hello world"));

    dock.center(tabbed);

    MenuBar menuBar = MenuItems.menuBar();

    window.setMenuBar(menuBar);

    window.setContent(dock);
  }

  @RequiredUIAccess
  public static void build(DockLayout window) {
    VerticalLayout layout = Layouts.vertical();

    final CheckBox top = Components.checkBox("top");
    top.setEnabled(false);
    layout.add(top);
    final CheckBox left = Components.checkBox("left");
    layout.add(left);
    final CheckBox right = Components.checkBox("right (this item will blink every 5 sec)");
    layout.add(right);
    final CheckBox bottom = Components.checkBox("bottom");
    layout.add(bottom);

    final CheckBox center = Components.checkBox("UI proxy?=center", false);
    center.addValueListener(new ValueComponent.ValueListener<Boolean>() {
      @RequiredUIAccess
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

    Executors.newScheduledThreadPool(1).scheduleAtFixedRate((Runnable)() -> uiAccess.give(() -> right.setValue(!right.getValue())), 5, 5, TimeUnit.SECONDS);

    center.setValue(true);
    layout.add(center);
    layout.add(Layouts.horizontal().add(Components.checkBox("Test 1")).add(Components.checkBox("Test 2")));
    layout.add(Layouts.horizontal().add(Components.label("Test 1")).add(Components.label("Test 2")));
    layout.add(Layouts.horizontal().add(Components.htmlLabel("<b>Test 1</b>")).add(Components.label("<b>Test 1</b>")));
    final ComboBox<String> comboBox = Components.comboBox("test", "test2");
    comboBox.setRender((render, index, item) -> {
      if (item == null) {
        render.setIcon(AllIcons.Actions.Help);
        render.append("<null>");
        return;
      }

      if (item.equals("test2")) {
        render.setIcon(AllIcons.Actions.IntentionBulb);
        render.append(item, TextStyle.BOLD);
      }
      else {
        render.setIcon(Images.fold(AllIcons.Nodes.Class, AllIcons.Nodes.JunitTestMark));

        render.append(item);
      }
    });

    layout.add(LabeledComponents.left("SDK:", comboBox));
    layout.add(LabeledComponents.leftFilled("Some Field:", Components.textField("Hello")));

    layout.add(Layouts.horizontal().add(Components.imageBox(Images.fold(AllIcons.Nodes.AbstractClass, AllIcons.Nodes.JunitTestMark))));

    layout.add(Components.radioButton("Uncheck me!", true));
    layout.add(Components.radioButton("Check me!"));

    layout.add(Components.listBox("Test", "Test2", "Test3"));

    window.center(layout);
  }
}
