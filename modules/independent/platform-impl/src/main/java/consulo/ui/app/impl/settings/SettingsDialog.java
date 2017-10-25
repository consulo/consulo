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
package consulo.ui.app.impl.settings;

import com.intellij.openapi.options.Configurable;
import consulo.ui.*;
import consulo.ui.app.WindowWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 25-Oct-17
 */
public class SettingsDialog extends WindowWrapper {
  private Configurable[] myConfigurables;

  public SettingsDialog(Configurable[] configurables) {
    super("Settings");
    myConfigurables = configurables;
  }

  @Nullable
  @Override
  protected Size getDefaultSize() {
    return new Size(1028, 500);
  }

  @RequiredUIAccess
  @NotNull
  @Override
  protected Component createCenterComponent() {
    TreeModel<Configurable> configurableTreeModel = new TreeModel<Configurable>() {
      @Override
      public void fetchChildren(@NotNull Function<Configurable, TreeNode<Configurable>> nodeFactory, @Nullable Configurable parentValue) {
        if (parentValue != null) {
          if (parentValue instanceof Configurable.Composite) {
            build(nodeFactory, ((Configurable.Composite)parentValue).getConfigurables());
          }
        }
        else {
          build(nodeFactory, myConfigurables);
        }
      }

      private void build(@NotNull Function<Configurable, TreeNode<Configurable>> nodeFactory, Configurable[] configurables) {
        for (Configurable configurable : configurables) {
          TreeNode<Configurable> node = nodeFactory.apply(configurable);

          boolean b = configurable instanceof Configurable.Composite && ((Configurable.Composite)configurable).getConfigurables().length > 0;
          node.setLeaf(!b);

          node.setRender((item, itemPresentation) -> itemPresentation.append(item.getDisplayName()));
        }
      }
    };

    Tree<Configurable> component = Tree.create(configurableTreeModel);

    DockLayout rightPart = DockLayout.create();
    rightPart.center(Label.create("Select configurable"));

    component.addSelectListener(node -> {
      Configurable configurable = node.getValue();

      Component uiComponent = configurable.createUIComponent();
      if (uiComponent != null) {
        configurable.reset();

        rightPart.center(uiComponent);
      }
      else {
        rightPart.center(Label.create("Not supported UI"));
      }
    });

    SplitLayout splitLayout = SplitLayout.createHorizontal();
    splitLayout.setProportion(30);

    splitLayout.setFirstComponent(component);

    splitLayout.setSecondComponent(rightPart);

    return splitLayout;
  }
}
