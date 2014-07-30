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
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.moduleLayerActions.AddLayerAction;
import com.intellij.openapi.roots.ui.configuration.projectRoot.moduleLayerActions.RemoveLayerAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.MutableCollectionComboBoxModel;
import com.intellij.util.IconUtil;
import lombok.val;
import org.jdesktop.swingx.HorizontalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

/**
 * @author VISTALL
 * @since 29.07.14
 */
public class ModuleLayerPanel extends JPanel {
  private static final String ACTION_PLACE = "ModuleLayerPanel";

  public ModuleLayerPanel(@NotNull final ModuleEditor moduleEditor) {
    super(new BorderLayout());

    val moduleRootModel = moduleEditor.getModifiableRootModelProxy();

    val model = new MutableCollectionComboBoxModel<String>(new ArrayList<String>(moduleRootModel.getLayers().keySet()), moduleRootModel.getCurrentLayerName());
    val comboBox = new ComboBox(model);

    moduleEditor.addChangeListener(new ModuleEditor.ChangeListener() {
      @Override
      public void moduleStateChanged(ModifiableRootModel moduleRootModel) {
        model.update(new ArrayList<String>(moduleRootModel.getLayers().keySet()));
        model.setSelectedItem(moduleRootModel.getCurrentLayerName());
        model.update();

        comboBox.setEnabled(comboBox.getItemCount() > 1);
      }
    });

    comboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          moduleEditor.getModifiableRootModelProxy().setCurrentLayer((String)comboBox.getSelectedItem());
        }
      }
    });

    JPanel panel = new JPanel(new HorizontalLayout());
    panel.add(new ActionButton(new AddLayerAction(moduleEditor, false), presentation("Add", IconUtil.getAddIcon()), ACTION_PLACE,
                               ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));

    panel.add(new ActionButton(new RemoveLayerAction(moduleEditor), presentation("Remove", IconUtil.getRemoveIcon()), ACTION_PLACE,
                               ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));

    panel.add(new ActionButton(new AddLayerAction(moduleEditor, true), presentation("Copy", AllIcons.Actions.Copy), ACTION_PLACE,
                               ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));

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
