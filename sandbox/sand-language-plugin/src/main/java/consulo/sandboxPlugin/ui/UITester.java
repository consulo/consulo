/*
 * Copyright 2013-2020 consulo.io
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
package consulo.sandboxPlugin.ui;

import consulo.disposer.Disposable;
import consulo.ide.ui.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.cursor.StandardCursors;
import consulo.ui.dialog.DialogDescriptor;
import consulo.ui.dialog.DialogService;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.layout.*;
import consulo.ui.model.TableModel;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public class UITester {
  private static class MyWindowWrapper extends DialogDescriptor<Void> {
    public MyWindowWrapper() {
      super(LocalizeValue.of("UI Tester"));
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Component createCenterComponent(@Nonnull Disposable uiDisposable) {
      TabbedLayout tabbedLayout = TabbedLayout.create();

      tabbedLayout.addTab("Layouts", layouts()).setCloseHandler((tab, component) -> {
      });
      tabbedLayout.addTab("Components", components());
      tabbedLayout.addTab("Components > Table", table());
      tabbedLayout.addTab("Components > Tree", tree());
      tabbedLayout.addTab("Alerts", alerts());

      return tabbedLayout;
    }

    @RequiredUIAccess
    private Component layouts() {
      TabbedLayout tabbedLayout = TabbedLayout.create();

      VerticalLayout fold = VerticalLayout.create();
      fold.add(Label.create("Some label"));
      fold.add(Button.create(LocalizeValue.localizeTODO("Some &Button"), (e) -> Alerts.okError("Clicked!").showAsync()));

      FoldoutLayout layout = FoldoutLayout.create(LocalizeValue.of("Show Me"), fold);
      layout.addStateListener(state -> Alerts.okInfo("State " + state).showAsync());

      tabbedLayout.addTab("FoldoutLayout", layout);

      TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
      splitLayout.setFirstComponent(DockLayout.create().center(Button.create("Left")));
      splitLayout.setSecondComponent(DockLayout.create().center(Button.create("Second")));

      tabbedLayout.addTab("SplitLayout", splitLayout);

      SwipeLayout swipeLayout = SwipeLayout.create();

      swipeLayout.register("left", () -> swipeChildLayout(LocalizeValue.of("Right"), () -> swipeLayout.swipeRightTo("right")));
      swipeLayout.register("right", () -> swipeChildLayout(LocalizeValue.of("Left"), () -> swipeLayout.swipeLeftTo("left")));

      tabbedLayout.addTab("SwipeLayout", swipeLayout);

      VerticalLayout borderLayout = VerticalLayout.create();
      DockLayout dockLayout = DockLayout.create();
      Button centerBtn = Button.create(LocalizeValue.of("Center"));
      centerBtn.addClickListener(event -> {
        dockLayout.center(HorizontalLayout.create().add(Label.create(LocalizeValue.of(LocalDateTime.now().toString()))));
      });

      borderLayout.add(centerBtn).add(dockLayout);

      borderLayout.add(centerBtn);

      tabbedLayout.addTab("DockLayout", borderLayout);

      return tabbedLayout;
    }

    @Override
    public boolean isSetDefaultContentBorder() {
      return false;
    }

    @RequiredUIAccess
    private Layout swipeChildLayout(LocalizeValue text, @RequiredUIAccess Runnable runnable) {
      DockLayout dockLayout = DockLayout.create();

      dockLayout.center(HorizontalLayout.create().add(Button.create(text, e -> runnable.run())));

      return dockLayout;
    }

    @RequiredUIAccess
    private Component components() {
      VerticalLayout layout = VerticalLayout.create();

      FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(null);
      layout.add(builder.build());

      ToggleSwitch toggleSwitch = ToggleSwitch.create(true);
      toggleSwitch.addValueListener(event -> Alerts.okInfo(LocalizeValue.of("toggle")).showAsync());

      CheckBox checkBox = CheckBox.create(LocalizeValue.of("Check box"));
      checkBox.addValueListener(event -> Alerts.okInfo(LocalizeValue.of("checkBox")).showAsync());

      layout.add(AdvancedLabel.create().updatePresentation(presentation -> {
        presentation.append(LocalizeValue.of("Advanced "), TextAttribute.REGULAR_BOLD);
        presentation.append(LocalizeValue.of("Label"), new TextAttribute(Font.STYLE_PLAIN, StandardColors.RED, StandardColors.BLACK));
      }));

      layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("Toggle Switch"))).add(toggleSwitch).add(checkBox));

      layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("Password"))).add(PasswordBox.create()));

      IntSlider intSlider = IntSlider.create(3);
      intSlider.addValueListener(event -> Alerts.okInfo(LocalizeValue.of("intSlider " + event.getValue())).showAsync());
      layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("IntSlider"))).add(intSlider));

      layout.add(Hyperlink.create("Some Link", (e) -> Alerts.okInfo(LocalizeValue.of("Clicked!!!")).showAsync()));

      HtmlView component = HtmlView.create();
      component.withValue("<html><body><b>Some Bold Text</b> Test</body></html>");
      layout.add(component);
      return layout;
    }

    @RequiredUIAccess
    private Component table() {
      DockLayout layout = DockLayout.create();
      Map<String, String> map = new TreeMap<>();
      map.put("test1", "1");
      map.put("test2", "3");
      map.put("test3", "5");

      List<TableColumn<?, Map.Entry<String, String>>> columns = new ArrayList<>();
      columns.add(TableColumn.<String, Map.Entry<String, String>>create("Column 1", Map.Entry::getKey).build());
      columns.add(TableColumn.<String, Map.Entry<String, String>>create("Column 2", Map.Entry::getValue).build());

      TableModel<Map.Entry<String, String>> model = TableModel.of(map.entrySet());

      layout.center(ScrollableLayout.create(Table.create(columns, model)));

      return layout;
    }

    @RequiredUIAccess
    private Component tree() {
      Tree<String> tree = Tree.create(new TreeModel<String>() {
        @Override
        public void buildChildren(@Nonnull Function<String, TreeNode<String>> nodeFactory, @Nullable String parentValue) {
          if (parentValue == null) {
            for (int i = 0; i < 50; i++) {
              TreeNode<String> node = nodeFactory.apply("First Child = " + i);

              List<Image> icons =
                      List.of(PlatformIconGroup.nodesClass(), PlatformIconGroup.nodesEnum(), PlatformIconGroup.nodesStruct(), PlatformIconGroup.nodesInterface(), PlatformIconGroup.nodesAttribute());
              int r = new Random().nextInt(icons.size());

              node.setRender((s, textItemPresentation) -> {
                textItemPresentation.append(s);
                textItemPresentation.withIcon(icons.get(r));
              });
            }
          }
          else {
            for (int i = 0; i < 10; i++) {
              nodeFactory.apply(parentValue + ", second child = " + i);
            }
          }
        }
      });

      return ScrollableLayout.create(tree);
    }

    @RequiredUIAccess
    private Component alerts() {
      VerticalLayout layout = VerticalLayout.create();
      layout.add(Button.create(LocalizeValue.of("Info. Hand Cursor"), event -> {
        Alerts.okInfo(LocalizeValue.of("This is INFO")).showAsync();
      }).withCursor(StandardCursors.HAND));
      layout.add(Button.create(LocalizeValue.of("Warning"), event -> {
        Alerts.okWarning(LocalizeValue.of("This is WARN")).showAsync();
      }));
      layout.add(Button.create(LocalizeValue.of("Error. Wait Cursor"), event -> {
        Alerts.okError(LocalizeValue.of("This is ERROR")).showAsync();
      }).withCursor(StandardCursors.WAIT));
      layout.add(Button.create(LocalizeValue.of("Question"), event -> {
        Alerts.okQuestion(LocalizeValue.of("This is QUESTION")).showAsync();
      }));
      return layout;
    }

    @Nullable
    @Override
    public Size getInitialSize() {
      return new Size(500, 500);
    }
  }

  @RequiredUIAccess
  public static void show(DialogService dialogService) {
    dialogService.build(null, new MyWindowWrapper()).showAsync();
  }
}
