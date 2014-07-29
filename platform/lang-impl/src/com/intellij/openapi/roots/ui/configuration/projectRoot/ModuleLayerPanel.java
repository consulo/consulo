/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.impl.ModuleRootLayerImpl;
import com.intellij.openapi.roots.impl.RootModelImpl;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.IconUtil;
import lombok.val;
import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.combobox.MapComboBoxModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 29.07.14
 */
public class ModuleLayerPanel extends JPanel {
  public ModuleLayerPanel(@NotNull final ModifiableRootModel modifiableRootModel) {
    super(new BorderLayout());

    val model = new MapComboBoxModel<String, ModuleRootLayerImpl>(((RootModelImpl)modifiableRootModel).getLayers0());
    model.setSelectedItem(modifiableRootModel.getCurrentLayerName());
    val comboBox = new ComboBox(model);

    comboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {

      }
    });

    val removePresentation = presentation("Remove", IconUtil.getRemoveIcon());
    val removeUpdater = new Runnable() {
      @Override
      public void run() {
        removePresentation.setEnabled(comboBox.getItemCount() > 1);
        comboBox.setEnabled(comboBox.getItemCount() > 1);
      }
    };
    removeUpdater.run();

    JPanel panel = new JPanel(new HorizontalLayout());
    panel.add(new ActionButton(new AnAction() {
      @Override
      public void actionPerformed(AnActionEvent anActionEvent) {

      }
    }, presentation("Add", IconUtil.getAddIcon()), "ConfigurationProfilePanel.ComboBox", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));

    panel.add(new ActionButton(new AnAction() {
      @Override
      public void actionPerformed(AnActionEvent anActionEvent) {

      }
    }, removePresentation, "ConfigurationProfilePanel.ComboBox", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));

    panel.add(new ActionButton(new AnAction() {
      @Override
      public void actionPerformed(AnActionEvent anActionEvent) {
      }

    }, presentation("Copy", AllIcons.Actions.Copy), "ConfigurationProfilePanel.ComboBox", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));

    JPanel newPanel = new JPanel(new BorderLayout());
    newPanel.add(comboBox, BorderLayout.CENTER);
    newPanel.add(panel, BorderLayout.EAST);
    add(newPanel, BorderLayout.NORTH);
  }

  private static Presentation presentation(String text, Icon icon) {
    Presentation presentation = new Presentation();
    presentation.setText(text);
    presentation.setIcon(icon);
    return presentation;
  }
}
