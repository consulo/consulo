package com.intellij.compiler.impl.javaCompiler;

import com.intellij.compiler.JavaCompilerBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.ListCellRendererWrapper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 10:39/27.05.13
 */
public class JavaCompilerConfigurable implements Configurable {
  private final JavaCompilerConfiguration myCompilerConfiguration;
  private final Project myProject;
  private JComboBox myComboBox;
  private JCheckBox myNotNullAssertion;

  public JavaCompilerConfigurable(Project project) {
    myProject = project;
    myCompilerConfiguration = JavaCompilerConfiguration.getInstance(project);
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Java";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    JPanel panel = new JPanel(new VerticalFlowLayout());

    myComboBox = new JComboBox();
    myComboBox.setRenderer(new ListCellRendererWrapper<BackendCompiler>() {
      @Override
      public void customize(JList list, BackendCompiler value, int index, boolean selected, boolean hasFocus) {
        setText(value.getPresentableName());
      }
    });

    for (BackendCompilerEP ep : BackendCompiler.EP_NAME.getExtensions(myProject)) {
      myComboBox.addItem(ep.getInstance(myProject));
    }

    myComboBox.setSelectedItem(myCompilerConfiguration.getActiveCompiler());

    panel.add(myComboBox);

    myNotNullAssertion = new JCheckBox(JavaCompilerBundle.message("add.notnull.assertions"));
    panel.add(myNotNullAssertion);
    return panel;
  }

  @Override
  public boolean isModified() {
    BackendCompiler item = (BackendCompiler) myComboBox.getSelectedItem();
    if(!Comparing.equal(item, myCompilerConfiguration.getActiveCompiler())) {
      return true;
    }
    if(myNotNullAssertion.isSelected() != myCompilerConfiguration.isAddNotNullAssertions()) {
      return true;
    }
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    BackendCompiler ep = (BackendCompiler) myComboBox.getSelectedItem();

    myCompilerConfiguration.setActiveCompiler(ep);
    myCompilerConfiguration.setAddNotNullAssertions(myNotNullAssertion.isSelected());
  }

  @Override
  public void reset() {
    myComboBox.setSelectedItem(myCompilerConfiguration.getActiveCompiler());
    myNotNullAssertion.setSelected(myNotNullAssertion.isSelected());
  }

  @Override
  public void disposeUIResources() {

  }
}
