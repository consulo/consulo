package org.jetbrains.builtInWebServer;

import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.ui.PortField;
import javax.annotation.Nonnull;

import javax.swing.*;

class BuiltInServerConfigurableUi implements ConfigurableUi<BuiltInServerOptions> {
  private JPanel mainPanel;

  private PortField builtInServerPort;
  private JCheckBox builtInServerAvailableExternallyCheckBox;
  private JCheckBox allowUnsignedRequestsCheckBox;

  public BuiltInServerConfigurableUi() {
    builtInServerPort.setMin(1024);
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified(@Nonnull BuiltInServerOptions settings) {
    return builtInServerPort.getNumber() != settings.builtInServerPort ||
           builtInServerAvailableExternallyCheckBox.isSelected() != settings.builtInServerAvailableExternally ||
           allowUnsignedRequestsCheckBox.isSelected() != settings.allowUnsignedRequests;
  }

  @Override
  public void apply(@Nonnull BuiltInServerOptions settings) {
    boolean builtInServerPortChanged = settings.builtInServerPort != builtInServerPort.getNumber() || settings.builtInServerAvailableExternally != builtInServerAvailableExternallyCheckBox.isSelected();
    settings.allowUnsignedRequests = allowUnsignedRequestsCheckBox.isSelected();
    if (builtInServerPortChanged) {
      settings.builtInServerPort = builtInServerPort.getNumber();
      settings.builtInServerAvailableExternally = builtInServerAvailableExternallyCheckBox.isSelected();

      BuiltInServerOptions.onBuiltInServerPortChanged();
    }
  }

  @Override
  public void reset(@Nonnull BuiltInServerOptions settings) {
    builtInServerPort.setNumber(settings.builtInServerPort);
    builtInServerAvailableExternallyCheckBox.setSelected(settings.builtInServerAvailableExternally);
    allowUnsignedRequestsCheckBox.setSelected(settings.allowUnsignedRequests);
  }
}
