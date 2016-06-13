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
import consulo.ui.layout.VerticalLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class SomeTestUIBuilder {
  @RequiredUIThread
  public static Component build() {
    VerticalLayout layout = UIFactory.Layouts.vertical();

    final CheckBox top = create("top");
    top.setEnabled(false);
    layout.add(top);
    final CheckBox left = create("left");
    layout.add(left);
    final CheckBox right = create("right (this item will blink every 5 sec)");
    layout.add(right);
    final CheckBox bottom = create("bottom");
    layout.add(bottom);

    final CheckBox center = UIFactory.Components.checkBox("UI proxy?=center", false);
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
    layout.add(UIFactory.Layouts.horizontal().add(UIFactory.Components.checkBox("Test 1")).add(UIFactory.Components.checkBox("Test 2")));
    layout.add(UIFactory.Layouts.horizontal().add(UIFactory.Components.label("Test 1")).add(UIFactory.Components.label("Test 2")));
    layout.add(UIFactory.Layouts.horizontal().add(UIFactory.Components.htmlLabel("<b>Test 1</b>")).add(UIFactory.Components.label("<b>Test 1</b>")));
    final ComboBox<String> comboBox = UIFactory.Components.comboBox("test", "test2");
    comboBox.setRender(new ListItemRender<String>() {
      @Override
      public void render(@NotNull ListItemPresentation render, int index, @Nullable String item) {
        if (item == null) {
          render.append("<null>");
          return;
        }

        if (item.equals("test2")) {
          render.append(item, TextStyle.BOLD);
        }
        else {
          render.append(item);
        }
      }
    });
    layout.add(UIFactory.Layouts.dock().left(UIFactory.Components.label("SDK:")).center(comboBox));
    final ComboBox<String> component = UIFactory.Components.comboBox("test1", "tet2");
    component.addValueListener(new ValueComponent.ValueListener<String>() {
      @Override
      public void valueChanged(@NotNull ValueComponent.ValueEvent<String> event) {
        System.out.println(event.getValue() + " selected");
      }
    });
    component.setValue("tet2");
    layout.add(UIFactory.Layouts.horizontal().add(UIFactory.Components.label("SDK:")).add(component));

    return layout;
  }

  private static CheckBox create(String text) {
    return UIFactory.Components.checkBox("UI proxy?=" + text, true);
  }
}
