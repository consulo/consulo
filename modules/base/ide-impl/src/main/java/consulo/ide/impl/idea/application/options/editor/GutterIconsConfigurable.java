/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ApplicationBundle;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.configurable.*;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ui.ex.awt.speedSearch.ListSpeedSearch;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.settings.impl.EditorGeneralConfigurable;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.gutter.GutterIconDescriptor;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.language.editor.gutter.LineMarkerSettings;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.util.DialogUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.MultiMap;
import consulo.util.lang.ObjectUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class GutterIconsConfigurable implements SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
  public static final String DISPLAY_NAME = "Gutter Icons";
  public static final String ID = "editor.gutterIcons";

  private CheckBoxList<GutterIconDescriptor> myList;
  private JBCheckBox myShowGutterIconsJBCheckBox;
  private List<GutterIconDescriptor> myDescriptors;

  @Nls
  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    myShowGutterIconsJBCheckBox = new JBCheckBox(ApplicationBundle.message("checkbox.show.gutter.icons"));
    DialogUtil.registerMnemonic(myShowGutterIconsJBCheckBox, '&');

    JPanel panel = new JPanel(new BorderLayout());

    panel.add(myShowGutterIconsJBCheckBox, BorderLayout.NORTH);

    Map<GutterIconDescriptor, PluginDescriptor> firstDescriptors = new HashMap<>();

    myList = new CheckBoxList<GutterIconDescriptor>() {
      @Override
      protected JComponent adjustRendering(JComponent rootComponent, JCheckBox checkBox, int index, boolean selected, boolean hasFocus) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder());
        GutterIconDescriptor descriptor = myList.getItemAt(index);
        Icon icon = descriptor == null ? null : TargetAWT.to(descriptor.getIcon());
        JLabel label = new JLabel(icon == null ? EmptyIcon.ICON_16 : icon);
        label.setOpaque(true);
        label.setPreferredSize(new Dimension(25, -1));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.WEST);
        panel.add(checkBox, BorderLayout.CENTER);
        panel.setBackground(getBackground(false));
        label.setBackground(getBackground(selected));
        if (!checkBox.isOpaque()) {
          checkBox.setOpaque(true);
        }
        checkBox.setBorder(null);

        PluginDescriptor pluginDescriptor = firstDescriptors.get(descriptor);
        if (pluginDescriptor != null) {
          SeparatorWithText separator = new SeparatorWithText();
          String name = pluginDescriptor.getName();
          separator.setCaption(name);
          panel.add(separator, BorderLayout.NORTH);
        }

        return panel;
      }

      @Nullable
      @Override
      protected Point findPointRelativeToCheckBox(int x, int y, @Nonnull JCheckBox checkBox, int index) {
        return super.findPointRelativeToCheckBoxWithAdjustedRendering(x, y, checkBox, index);
      }
    };
    myList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    myList.setBorder(BorderFactory.createEmptyBorder());
    new ListSpeedSearch(myList, o -> o instanceof JCheckBox ? ((JCheckBox)o).getText() : null);
    panel.add(ScrollPaneFactory.createScrollPane(myList), BorderLayout.CENTER);

    List<LineMarkerProvider> providers = Application.get().getExtensionList(LineMarkerProvider.class);
    Function<LineMarkerProvider, PluginDescriptor> function = provider -> {
      PluginDescriptor plugin = PluginManager.getPlugin(provider.getClass());
      return provider instanceof LineMarkerProviderDescriptor && ((LineMarkerProviderDescriptor)provider).getName() != null ? plugin : null;
    };

    MultiMap<PluginDescriptor, LineMarkerProvider> map = ContainerUtil.groupBy(providers, function);
    Map<GutterIconDescriptor, PluginDescriptor> pluginDescriptorMap = ContainerUtil.newHashMap();
    Set<String> ids = new HashSet<>();
    myDescriptors = new ArrayList<>();
    for (final PluginDescriptor descriptor : map.keySet()) {
      Collection<LineMarkerProvider> markerProviders = map.get(descriptor);
      for (LineMarkerProvider provider : markerProviders) {
        GutterIconDescriptor instance = (GutterIconDescriptor)provider;
        if (instance.getOptions().length > 0) {
          for (GutterIconDescriptor option : instance.getOptions()) {
            if (ids.add(option.getId())) {
              myDescriptors.add(option);
            }
            pluginDescriptorMap.put(option, descriptor);
          }
        }
        else {
          if (ids.add(instance.getId())) {
            myDescriptors.add(instance);
          }
          pluginDescriptorMap.put(instance, descriptor);
        }
      }
    }
    /*
    List<GutterIconDescriptor> options = new ArrayList<GutterIconDescriptor>();
    for (Iterator<GutterIconDescriptor> iterator = myDescriptors.iterator(); iterator.hasNext(); ) {
      GutterIconDescriptor descriptor = iterator.next();
      if (descriptor.getOptions().length > 0) {
        options.addAll(Arrays.asList(descriptor.getOptions()));
        iterator.remove();
      }
    }
    myDescriptors.addAll(options);
    */
    myDescriptors.sort((o1, o2) -> {
      if (pluginDescriptorMap.get(o1) != pluginDescriptorMap.get(o2)) return 0;
      return Comparing.compare(o1.getName(), o2.getName());
    });
    PluginDescriptor current = null;
    for (GutterIconDescriptor descriptor : myDescriptors) {
      PluginDescriptor pluginDescriptor = pluginDescriptorMap.get(descriptor);
      if (pluginDescriptor != current) {
        firstDescriptors.put(descriptor, pluginDescriptor);
        current = pluginDescriptor;
      }
    }

    myList.setItems(myDescriptors, GutterIconDescriptor::getName);
    myShowGutterIconsJBCheckBox.addChangeListener(e -> myList.setEnabled(myShowGutterIconsJBCheckBox.isSelected()));
    return panel;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    for (GutterIconDescriptor descriptor : myDescriptors) {
      if (myList.isItemSelected(descriptor) != LineMarkerSettings.getInstance().isEnabled(descriptor)) {
        return true;
      }
    }
    return myShowGutterIconsJBCheckBox.isSelected() != EditorSettingsExternalizable.getInstance().areGutterIconsShown();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable editorSettings = EditorSettingsExternalizable.getInstance();
    if (myShowGutterIconsJBCheckBox.isSelected() != editorSettings.areGutterIconsShown()) {
      editorSettings.setGutterIconsShown(myShowGutterIconsJBCheckBox.isSelected());
      EditorGeneralConfigurable.reinitAllEditors();
    }
    for (GutterIconDescriptor descriptor : myDescriptors) {
      LineMarkerSettings.getInstance().setEnabled(descriptor, myList.isItemSelected(descriptor));
    }
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      DaemonCodeAnalyzer.getInstance(project).restart();
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    for (GutterIconDescriptor descriptor : myDescriptors) {
      myList.setItemSelected(descriptor, LineMarkerSettings.getInstance().isEnabled(descriptor));
    }
    boolean gutterIconsShown = EditorSettingsExternalizable.getInstance().areGutterIconsShown();
    myShowGutterIconsJBCheckBox.setSelected(gutterIconsShown);
    myList.setEnabled(gutterIconsShown);
  }

  @Nonnull
  @Override
  public String getId() {
    return ID;
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return () -> ObjectUtil.assertNotNull(SpeedSearchSupply.getSupply(myList, true)).findAndSelectElement(option);
  }

  @TestOnly
  public List<GutterIconDescriptor> getDescriptors() {
    return myDescriptors;
  }
}
