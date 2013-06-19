/*
 * Copyright 2004-2005 Alexey Efimov
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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.ProjectTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanelImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.OrderPanelListener;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 4, 2003
 *         Time: 6:54:57 PM
 */
public class ClasspathEditor extends ModuleElementsEditor implements ModuleRootListener {
  public static final String NAME = ProjectBundle.message("modules.classpath.title");

  private ClasspathPanelImpl myPanel;

  public ClasspathEditor(final ModuleConfigurationState state) {
    super(state);

    final Disposable disposable = Disposer.newDisposable();

    state.getProject().getMessageBus().connect(disposable).subscribe(ProjectTopics.PROJECT_ROOTS, this);
    registerDisposable(disposable);
  }

  @Override
  public boolean isModified() {
    return super.isModified();
  }

  @Override
  public String getHelpTopic() {
    return "projectStructure.modules.dependencies";
  }

  @Override
  public String getDisplayName() {
    return NAME;
  }

  @Override
  public void saveData() {
    myPanel.stopEditing();
    flushChangesToModel();
  }

  @Override
  public void apply () throws ConfigurationException {

  }

  @Override
  public void canApply() throws ConfigurationException {
    super.canApply();

  }

  @NotNull
  @Override
  public JComponent createComponentImpl() {
    myPanel = new ClasspathPanelImpl(getState());

    myPanel.addListener(new OrderPanelListener() {
      @Override
      public void entryMoved() {
        flushChangesToModel();
      }
    });

    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
    panel.add(myPanel, BorderLayout.CENTER);

    return panel;
  }

  public void flushChangesToModel() {
    List<OrderEntry> entries = myPanel.getEntries();
    getModel().rearrangeOrderEntries(entries.toArray(new OrderEntry[entries.size()]));
  }

  public void selectOrderEntry(@NotNull final OrderEntry entry) {
    myPanel.selectOrderEntry(entry);
  }

  @Override
  public void moduleStateChanged() {
    if (myPanel != null) {
      myPanel.initFromModel();
    }
  }

  @Override
  public void beforeRootsChange(ModuleRootEvent event) {
  }

  @Override
  public void rootsChanged(ModuleRootEvent event) {
    if (myPanel != null) {
      myPanel.rootsChanged();
    }
  }
}
