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
package consulo.compiler.impl.resourceCompiler;

import com.intellij.compiler.MalformedPatternException;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 20:47/12.06.13
 */
public class ResourceCompilerConfigurable implements Configurable, Configurable.NoScroll {
  @Nonnull
  private final Project myProject;
  private ResourceCompilerConfiguration myResourceCompilerConfiguration;

  private CollectionListModel<String> myModel;

  @Inject
  public ResourceCompilerConfigurable(@Nonnull Project project) {
    myProject = project;
    myResourceCompilerConfiguration = ResourceCompilerConfiguration.getInstance(project);

    myModel = new CollectionListModel<String>(myResourceCompilerConfiguration.getResourceFilePatterns());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Resource";
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    JBList list = new JBList(myModel);
    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(list);
    decorator.disableUpDownActions();
    decorator.setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {
        showAddOrChangeDialog(null);
      }
    });

    decorator.setEditAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {
        JBList contextComponent = (JBList)anActionButton.getContextComponent();
        showAddOrChangeDialog((String)contextComponent.getSelectedValue());
      }
    });
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(decorator.createPanel(), BorderLayout.CENTER);

    JTextPane textPane = new JTextPane();
    textPane.setContentType("text/html");
    textPane.setEditable(false);
    textPane.setText("<html><body>" +
                     "Use <b>!</b> to negate a pattern. " +
                     "Accepted wildcards: <b>?</b> &mdash; exactly one symbol; <b>*</b> &mdash; zero or more symbols; " +
                     "<b>/</b> &mdash; path separator; <b>/**/</b> &mdash; any number of directories; " +
                     "<i>&lt;dir_name&gt;</i>:<i>&lt;pattern&gt;</i> &mdash; restrict to source roots with the specified name" +
                     "</body></html>");
    textPane.setForeground(new JBColor(Gray._50, Gray._130));
    panel.add(textPane, BorderLayout.SOUTH);
    return panel;
  }

  private void showAddOrChangeDialog(final String initialValue) {
    String pattern = Messages.showInputDialog(myProject, "Pattern", "Enter Pattern", null, initialValue, new InputValidatorEx() {
      @Override
      public boolean checkInput(String inputString) {
        return (initialValue == null && myModel.getElementIndex(inputString) == -1 || initialValue != null) && getErrorText(inputString) == null;
      }

      @Override
      public boolean canClose(String inputString) {
        return true;
      }

      @javax.annotation.Nullable
      @Override
      public String getErrorText(String inputString) {
        try {
          ResourceCompilerConfiguration.convertToRegexp(inputString);
          return null;
        }
        catch (Exception ex) {
          return ex.getMessage();
        }
      }
    });

    if (pattern != null) {
      if (initialValue != null) {
        myModel.remove(initialValue);
      }

      myModel.add(pattern);
    }
  }

  @Override
  public boolean isModified() {
    return !Comparing.haveEqualElements(myModel.toList(), myResourceCompilerConfiguration.getResourceFilePatterns());
  }

  @Override
  public void apply() throws ConfigurationException {
    myResourceCompilerConfiguration.removeResourceFilePatterns();

    for (String namePattern : myModel.toList()) {
      try {
        myResourceCompilerConfiguration.addResourceFilePattern(namePattern);
      }
      catch (MalformedPatternException e) {
      }
    }
  }

  @Override
  public void reset() {
    myResourceCompilerConfiguration.convertPatterns();

    myModel.replaceAll(myResourceCompilerConfiguration.getResourceFilePatterns());
  }

  @Override
  public void disposeUIResources() {
  }
}
