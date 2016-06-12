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
    center.addSelectListener(new CheckBox.SelectListener() {
      @Override
      @RequiredUIThread
      public void selectChanged(@NotNull CheckBox checkBox) {
        top.setSelected(checkBox.isSelected());
        left.setSelected(checkBox.isSelected());
        right.setSelected(checkBox.isSelected());
        bottom.setSelected(checkBox.isSelected());
        bottom.setVisible(!checkBox.isSelected());
      }
    });

    final UIAccess uiAccess = UIAccess.get();

    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        uiAccess.give(new Runnable() {
          @Override
          public void run() {
            right.setSelected(!right.isSelected());
          }
        });
      }
    }, 5, 5, TimeUnit.SECONDS);

    center.setSelected(true);
    layout.add(center);
    layout.add(UIFactory.Layouts.horizontal().add(UIFactory.Components.checkBox("Test 1")).add(UIFactory.Components.checkBox("Test 2")));
    layout.add(UIFactory.Layouts.horizontal().add(UIFactory.Components.label("Test 1")).add(UIFactory.Components.label("Test 2")));
    layout.add(UIFactory.Layouts.horizontal().add(UIFactory.Components.htmlLabel("<b>Test 1</b>")).add(UIFactory.Components.label("<b>Test 1</b>")));
    //layout.add(UIFactory.Components.comboBox("test", "test2"));

    return layout;
  }

  private static CheckBox create(String text) {
    return UIFactory.Components.checkBox("UI proxy?=" + text, true);
  }
}
