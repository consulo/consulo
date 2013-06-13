/*
 * Copyright 2013 Consulo.org
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
package org.consulo.compiler.impl.resourceCompiler;

import com.intellij.compiler.MalformedPatternException;
import com.intellij.compiler.options.ComparingUtils;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author VISTALL
 * @since 20:47/12.06.13
 */
public class ResourceCompilerConfigurable implements Configurable {
  public static final Function<String, List<String>> LINE_PARSER = new Function<String, List<String>>() {
    @Override
    public List<String> fun(String text) {
      final ArrayList<String> result = ContainerUtilRt.newArrayList();
      final StringTokenizer tokenizer = new StringTokenizer(text, ";", false);
      while (tokenizer.hasMoreTokens()) {
        result.add(tokenizer.nextToken());
      }
      return result;
    }
  };
  public static final Function<List<String>, String> LINE_JOINER = new Function<List<String>, String>() {
    @Override
    public String fun(List<String> strings) {
      return StringUtil.join(strings, ";");
    }
  };

  private RawCommandLineEditor myResourcePatternsField;
  private JBLabel myPatternLegendLabel;
  private JPanel myRootPanel;

  private ResourceCompilerConfiguration myResourceCompilerConfiguration;

  public ResourceCompilerConfigurable(@NotNull Project project) {
    myPatternLegendLabel.setText("<html><body>" +
                                 "Use <b>;</b> to separate patterns and <b>!</b> to negate a pattern. " +
                                 "Accepted wildcards: <b>?</b> &mdash; exactly one symbol; <b>*</b> &mdash; zero or more symbols; " +
                                 "<b>/</b> &mdash; path separator; <b>/**/</b> &mdash; any number of directories; " +
                                 "<i>&lt;dir_name&gt;</i>:<i>&lt;pattern&gt;</i> &mdash; restrict to source roots with the specified name" +
                                 "</body></html>");
    myPatternLegendLabel.setForeground(new JBColor(Gray._50, Gray._130));
    myResourceCompilerConfiguration = ResourceCompilerConfiguration.getInstance(project);
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Resource";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myRootPanel;
  }

  @Override
  public boolean isModified() {
    return ComparingUtils.isModified(myResourcePatternsField, patternsToString(myResourceCompilerConfiguration.getResourceFilePatterns()));
  }

  @Override
  public void apply() throws ConfigurationException {
    myResourceCompilerConfiguration.removeResourceFilePatterns();
    String extensionString = myResourcePatternsField.getText().trim();
    applyResourcePatterns(extensionString);
  }

  private void applyResourcePatterns(String extensionString)
    throws ConfigurationException {
    StringTokenizer tokenizer = new StringTokenizer(extensionString, ";", false);
    List<String[]> errors = new ArrayList<String[]>();

    while (tokenizer.hasMoreTokens()) {
      String namePattern = tokenizer.nextToken();
      try {
        myResourceCompilerConfiguration.addResourceFilePattern(namePattern);
      }
      catch (MalformedPatternException e) {
        errors.add(new String[]{namePattern, e.getLocalizedMessage()});
      }
    }

    if (errors.size() > 0) {
      final StringBuilder pattersnsWithErrors = new StringBuilder();
      for (final Object error : errors) {
        String[] pair = (String[])error;
        pattersnsWithErrors.append("\n");
        pattersnsWithErrors.append(pair[0]);
        pattersnsWithErrors.append(": ");
        pattersnsWithErrors.append(pair[1]);
      }

      throw new ConfigurationException(
        CompilerBundle.message("error.compiler.configurable.malformed.patterns", pattersnsWithErrors.toString()), CompilerBundle.message("bad.resource.patterns.dialog.title")
      );
    }
  }

  @Override
  public void reset() {
    myResourceCompilerConfiguration.convertPatterns();

    myResourcePatternsField.setText(patternsToString(myResourceCompilerConfiguration.getResourceFilePatterns()));
  }

  private void createUIComponents() {
    myResourcePatternsField = new RawCommandLineEditor(LINE_PARSER, LINE_JOINER);
    myResourcePatternsField.setDialogCaption("Resource patterns");
  }

  private static String patternsToString(final String[] patterns) {
    final StringBuilder extensionsString = new StringBuilder();
    for (int idx = 0; idx < patterns.length; idx++) {
      if (idx > 0) {
        extensionsString.append(";");
      }
      extensionsString.append(patterns[idx]);
    }
    return extensionsString.toString();
  }

  @Override
  public void disposeUIResources() {
  }
}
