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
package consulo.ide.impl.compiler.resourceCompiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompilerBundle;
import consulo.compiler.resourceCompiler.ResourceCompilerConfiguration;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.util.lang.Comparing;
import consulo.project.Project;
import consulo.ui.ex.Gray;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.regex.PatternSyntaxException;

/**
 * @author VISTALL
 * @since 20:47/12.06.13
 */
@ExtensionImpl
public class ResourceCompilerConfigurable implements ProjectConfigurable, Configurable.NoScroll {
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
    return CompilerBundle.message("resource.compiler.description");
  }

  @Nonnull
  @Override
  public String getId() {
    return "project.propCompiler.resourceCompiler";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.COMPILER_GROUP;
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

      @jakarta.annotation.Nullable
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
      catch (PatternSyntaxException e) {
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
