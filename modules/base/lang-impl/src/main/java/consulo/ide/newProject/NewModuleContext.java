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
package consulo.ide.newProject;

import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotations.DeprecationInfo;
import consulo.ide.wizard.newModule.NewModuleWizardContextBase;
import consulo.ide.wizard.newModule.ProjectOrModuleNameStep;
import consulo.ui.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 05.06.14
 */
public class NewModuleContext {
  public static final String UGROUPED = "ungrouped";

  public static class Group implements Comparable<Group> {
    @SuppressWarnings("deprecation")
    private static class BridgeStep extends ProjectOrModuleNameStep<BridgeWizardContext> implements WizardStep<BridgeWizardContext> {
      private NewModuleBuilderProcessor myProcessor;

      private JComponent myComponent;

      private BridgeStep(NewModuleBuilderProcessor processor, BridgeWizardContext context) {
        super(context);
        myProcessor = processor;
      }

      @RequiredUIAccess
      @Nonnull
      @Override
      public consulo.ui.Component getComponent() {
        throw new UnsupportedOperationException();
      }

      @RequiredUIAccess
      @Nonnull
      @Override
      public Component getSwingComponent() {
        Component component = super.getSwingComponent();
        myAdditionalContentPanel.add(myComponent = myProcessor.createConfigurationPanel(), BorderLayout.NORTH);
        return component;
      }

      @Override
      public void onStepLeave(@Nonnull BridgeWizardContext context) {
        if (myComponent == null) {
          throw new IllegalArgumentException("no call #getSwingComponent()");
        }
        context.setPanel(myComponent);
      }
    }

    private static class BridgeWizardContext extends NewModuleWizardContextBase {
      private JComponent myPanel;

      public BridgeWizardContext(boolean isNewProject) {
        super(isNewProject);
      }

      public void setPanel(JComponent panel) {
        myPanel = panel;
      }

      public Component getPanel() {
        return myPanel;
      }
    }

    private final Set<Item> myItems = new TreeSet<>();
    private final String myId;
    private final String myName;

    public Group(String id, String name) {
      myId = id;
      myName = name;
    }

    public void add(String name, Image icon, NewModuleBuilderProcessor2<?> processor) {
      myItems.add(new Item(name, icon, processor));
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    @Deprecated
    @DeprecationInfo("Use NewModuleBuilderProcessor2")
    public void add(String name, Image icon, NewModuleBuilderProcessor<? extends JComponent> processor) {
      add(name, icon, new NewModuleBuilderProcessor2<BridgeWizardContext>() {
        @Nonnull
        @Override
        public BridgeWizardContext createContext(boolean isNewProject) {
          return new BridgeWizardContext(isNewProject);
        }

        @Override
        public void buildSteps(@Nonnull Consumer<WizardStep<BridgeWizardContext>> consumer, @Nonnull BridgeWizardContext context) {
          consumer.accept(new BridgeStep(processor, context));
        }

        @Override
        public void process(@Nonnull BridgeWizardContext context, @Nonnull ContentEntry contentEntry, @Nonnull ModifiableRootModel modifiableRootModel) {
          Component panel = context.getPanel();
          NewModuleBuilderProcessor unchecked = processor;
          unchecked.setupModule((JComponent)panel, contentEntry, modifiableRootModel);
        }
      });
    }

    public String getName() {
      return myName;
    }

    public String getId() {
      return myId;
    }

    @Nonnull
    public Set<Item> getItems() {
      return myItems;
    }

    @Override
    public int compareTo(@Nonnull Group o) {
      int weight = getWeight();
      int oWeight = o.getWeight();
      if (weight != oWeight) {
        return oWeight - weight;
      }

      return getName().compareTo(o.getName());
    }

    private int getWeight() {
      return getId().equals(UGROUPED) ? 1 : 100;
    }
  }

  public static class Item implements Comparable<Item> {
    private String myName;
    private Image myIcon;
    private NewModuleBuilderProcessor2<?> myProcessor;

    public Item(String name, Image icon, NewModuleBuilderProcessor2<?> processor) {
      myName = name;
      myIcon = icon;
      myProcessor = processor;
    }

    public String getName() {
      return myName;
    }

    public Image getIcon() {
      return myIcon;
    }

    public NewModuleBuilderProcessor2<?> getProcessor() {
      return myProcessor;
    }

    @Override
    public int compareTo(@Nonnull Item o) {
      return myName.compareTo(o.myName);
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  private final Map<String, Group> myGroups = new HashMap<>();

  @Nonnull
  public Group get(@Nonnull String id) {
    Group group = myGroups.get(id);
    if (group == null) {
      throw new IllegalArgumentException("Group with " + id + " is not registered");
    }
    return group;
  }

  @Nonnull
  public Group createGroup(@Nonnull String id, @Nonnull String name) {
    return myGroups.computeIfAbsent(id, (s) -> new Group(id, name));
  }

  @Nonnull
  public Group[] getGroups() {
    Group[] groups = myGroups.values().toArray(new Group[myGroups.size()]);
    ContainerUtil.sort(groups);
    return groups;
  }
}
