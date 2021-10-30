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
import com.intellij.openapi.util.Couple;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.app.WholeLeftWindowWrapper;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 25-Oct-17
 */
public class UnifiedSettingsDialog extends WholeLeftWindowWrapper {
  private final Map<Configurable, UnifiedConfigurableContext> myContexts = new ConcurrentHashMap<>();

  private Configurable[] myConfigurables;

  public UnifiedSettingsDialog(Configurable[] configurables) {
    super("Settings");
    myConfigurables = configurables;
  }

  @Nullable
  @Override
  protected Size getDefaultSize() {
    return new Size(1028, 500);
  }

  @RequiredUIAccess
  @Override
  public void doOKAction() {
    super.doOKAction();

    for (Map.Entry<Configurable, UnifiedConfigurableContext> entry : myContexts.entrySet()) {
      Configurable configurable = entry.getKey();
      UnifiedConfigurableContext context = entry.getValue();

      try {
        configurable.apply();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      finally {
        configurable.disposeUIResources();
      }
    }

    myContexts.clear();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Couple<Component> createComponents() {
    TreeModel<Configurable> configurableTreeModel = new TreeModel<>() {
      @Override
      public void buildChildren(@Nonnull Function<Configurable, TreeNode<Configurable>> nodeFactory, @Nullable Configurable parentValue) {
        if (parentValue != null) {
          if (parentValue instanceof Configurable.Composite) {
            build(nodeFactory, ((Configurable.Composite)parentValue).getConfigurables());
          }
        }
        else {
          build(nodeFactory, myConfigurables);
        }
      }

      @Nullable
      @Override
      public Comparator<TreeNode<Configurable>> getNodeComparator() {
        return UnifiedConfigurableComparator.INSTANCE;
      }

      private void build(@Nonnull Function<Configurable, TreeNode<Configurable>> nodeFactory, Configurable[] configurables) {
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
    rightPart.center(Label.create(LocalizeValue.localizeTODO("Select configurable")));

    component.addSelectListener(node -> {
      Configurable configurable = node.getValue();

      UnifiedConfigurableContext context = myContexts.computeIfAbsent(configurable, c -> {
        UnifiedConfigurableContext co = new UnifiedConfigurableContext(c);
        c.initialize();
        c.reset();
        return co;
      });

      Component uiComponent = context.getComponent();
      rightPart.center(uiComponent);
    });

    return Couple.of(ScrollableLayout.create(component), rightPart);
  }
}
