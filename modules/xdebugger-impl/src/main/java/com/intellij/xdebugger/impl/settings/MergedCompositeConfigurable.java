package com.intellij.xdebugger.impl.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.TitledSeparator;
import consulo.annotations.RequiredDispatchThread;
import consulo.options.ConfigurableUIMigrationUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class MergedCompositeConfigurable implements SearchableConfigurable {
  static final EmptyBorder BOTTOM_INSETS = new EmptyBorder(0, 0, IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET, 0);

  private static final Insets FIRST_COMPONENT_INSETS = new Insets(0, 0, IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET, 0);
  private static final Insets N_COMPONENT_INSETS = new Insets(IdeBorderFactory.TITLED_BORDER_TOP_INSET, 0, IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET, 0);

  protected final Configurable[] children;
  protected JComponent rootComponent;

  private final String id;
  private final String displayName;

  public MergedCompositeConfigurable(@NotNull String id, @NotNull String displayName, @NotNull Configurable[] children) {
    this.children = children;
    this.id = id;
    this.displayName = displayName;
  }

  @NotNull
  @Override
  public String getId() {
    return id;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return children.length == 1 ? children[0].getHelpTopic() : null;
  }

  @RequiredDispatchThread
  @Nullable
  @Override
  public JComponent createComponent() {
    if (rootComponent == null) {
      Configurable firstConfigurable = children[0];
      if (children.length == 1) {
        rootComponent = ConfigurableUIMigrationUtil.createComponent(firstConfigurable);
      }
      else {
        JPanel panel = createPanel(true);
        for (Configurable configurable : children) {
          JComponent component = ConfigurableUIMigrationUtil.createComponent(configurable);
          assert component != null;
          String displayName = configurable.getDisplayName();
          if (StringUtil.isEmpty(displayName)) {
            component.setBorder(BOTTOM_INSETS);
          }
          else {
            component.setBorder(
                    IdeBorderFactory.createTitledBorder(displayName, false, firstConfigurable == configurable ? FIRST_COMPONENT_INSETS : N_COMPONENT_INSETS));
          }
          panel.add(component);
        }
        rootComponent = panel;
      }
    }
    return rootComponent;
  }

  @NotNull
  static JPanel createPanel(boolean isUseTitledBorder) {
    int verticalGap = TitledSeparator.TOP_INSET;
    JPanel panel = new JPanel(new VerticalFlowLayout(0, isUseTitledBorder ? 0 : verticalGap));
    // VerticalFlowLayout incorrectly use vertical gap as top inset
    if (!isUseTitledBorder) {
      panel.setBorder(new EmptyBorder(-verticalGap, 0, 0, 0));
    }
    return panel;
  }

  @RequiredDispatchThread
  @Override
  public boolean isModified() {
    for (Configurable child : children) {
      if (child.isModified()) {
        return true;
      }
    }
    return false;
  }

  @RequiredDispatchThread
  @Override
  public void apply() throws ConfigurationException {
    for (Configurable child : children) {
      if (child.isModified()) {
        child.apply();
      }
    }
  }

  @RequiredDispatchThread
  @Override
  public void reset() {
    for (Configurable child : children) {
      child.reset();
    }
  }

  @RequiredDispatchThread
  @Override
  public void disposeUIResources() {
    rootComponent = null;

    for (Configurable child : children) {
      child.disposeUIResources();
    }
  }
}