package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.ListCellRendererWrapper;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * @author VISTALL
 * @since 10:39/27.05.13
 */
public class JavaConfigurable implements Configurable {
  private final JavaCompilerSettings myJavaCompilerSettings;

  public JavaConfigurable(JavaCompilerSettings javaCompilerSettings) {
    myJavaCompilerSettings = javaCompilerSettings;
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
    JPanel panel = new JPanel(new BorderLayout());

    JComboBox comboBox = new JComboBox();
    comboBox.setRenderer(new ListCellRendererWrapper<BackendCompiler>(){
      @Override
      public void customize(JList list, BackendCompiler value, int index, boolean selected, boolean hasFocus) {
        setText(value.getPresentableName());
      }
    } );

    for (Map.Entry<BackendCompiler, CompilerSettings> entry : myJavaCompilerSettings.getCompilers().entrySet()) {
      comboBox.addItem(entry.getKey());
    }

    final CardLayout layout = new CardLayout();
    final JPanel innerPanel = new JPanel(layout);
    for (Map.Entry<BackendCompiler, CompilerSettings> entry : myJavaCompilerSettings.getCompilers().entrySet()) {
      final String id = entry.getKey().getId();

      final Configurable configurable = entry.getValue().createConfigurable();
      if(configurable != null) {
        ConfigurablePanel configurablePanel = new ConfigurablePanel(configurable);

        innerPanel.add(configurablePanel, id);
      }
      else {
        innerPanel.add(new JPanel(), id);
      }
    }

    final BackendCompiler selectedCompiler = myJavaCompilerSettings.getSelectedCompiler();
    comboBox.setSelectedItem(selectedCompiler);

    comboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        final BackendCompiler item = (BackendCompiler) e.getItem();
        layout.show(innerPanel, item.getId());
      }
    });
    layout.show(innerPanel, selectedCompiler.getId());

    panel.add(LabeledComponent.create(comboBox, "Java Compiler"), BorderLayout.NORTH);
    panel.add(innerPanel, BorderLayout.CENTER);
    return panel;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {

  }

  @Override
  public void reset() {

  }

  @Override
  public void disposeUIResources() {

  }
}
