package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.options.Configurable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 11:01/27.05.13
 */
public class ConfigurablePanel extends JPanel {
  private final Configurable myConfigurable;

  public ConfigurablePanel(Configurable configurable) {
    super(new BorderLayout());
    myConfigurable = configurable;
    final JComponent component = configurable.createComponent();
    if(component != null) {
      add(component, BorderLayout.NORTH);
    }
  }

  public Configurable getConfigurable() {
    return myConfigurable;
  }
}
