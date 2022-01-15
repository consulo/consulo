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
package com.intellij.application.options.editor;

import com.intellij.codeInsight.daemon.*;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorWithText;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.speedSearch.SpeedSearchSupply;
import com.intellij.util.NullableFunction;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.DialogUtil;
import com.intellij.util.ui.EmptyIcon;
import consulo.awt.TargetAWT;
import consulo.container.plugin.PluginDescriptor;
import consulo.ide.settings.impl.EditorGeneralConfigurable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Dmitry Avdeev
 */
public class GutterIconsConfigurable implements SearchableConfigurable, Configurable.NoScroll {
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

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent() {
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
        if(pluginDescriptor != null) {
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

    ExtensionPoint<LanguageExtensionPoint<LineMarkerProvider>> point = Application.get().getExtensionPoint(ExtensionPointName.create(LineMarkerProviders.EP_NAME));
    List<LanguageExtensionPoint<LineMarkerProvider>> extensions = point.getExtensionList();
    NullableFunction<LanguageExtensionPoint<LineMarkerProvider>, PluginDescriptor> function = point1 -> {
      LineMarkerProvider instance = point1.getInstance();
      return instance instanceof LineMarkerProviderDescriptor && ((LineMarkerProviderDescriptor)instance).getName() != null ? point1.getPluginDescriptor() : null;
    };

    MultiMap<PluginDescriptor, LanguageExtensionPoint<LineMarkerProvider>> map = ContainerUtil.groupBy(extensions, function);
    Map<GutterIconDescriptor, PluginDescriptor> pluginDescriptorMap = ContainerUtil.newHashMap();
    Set<String> ids = new HashSet<>();
    myDescriptors = new ArrayList<>();
    for (final PluginDescriptor descriptor : map.keySet()) {
      Collection<LanguageExtensionPoint<LineMarkerProvider>> points = map.get(descriptor);
      for (LanguageExtensionPoint<LineMarkerProvider> extensionPoint : points) {
        GutterIconDescriptor instance = (GutterIconDescriptor)extensionPoint.getInstance();
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
