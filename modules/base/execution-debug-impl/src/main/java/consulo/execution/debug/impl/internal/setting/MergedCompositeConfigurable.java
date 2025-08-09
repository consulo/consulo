package consulo.execution.debug.impl.internal.setting;

import consulo.application.Application;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class MergedCompositeConfigurable implements SearchableConfigurable {
  static final EmptyBorder BOTTOM_INSETS = new EmptyBorder(0, 0, IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET, 0);

  private static final Insets FIRST_COMPONENT_INSETS = new Insets(0, 0, IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET, 0);
  private static final Insets N_COMPONENT_INSETS = new Insets(IdeBorderFactory.TITLED_BORDER_TOP_INSET, 0, IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET, 0);

  protected final Configurable[] children;
  protected JComponent myRootPanel;

  private Component myRootComponent;

  private final String id;
  private final LocalizeValue displayName;

  public MergedCompositeConfigurable(@Nonnull String id, @Nonnull LocalizeValue displayName, @Nonnull Configurable[] children) {
    this.children = children;
    this.id = id;
    this.displayName = displayName;
  }

  @Nonnull
  @Override
  public String getId() {
    return id;
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return displayName;
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return children.length == 1 ? children[0].getHelpTopic() : null;
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Component createUIComponent(@Nonnull Disposable uiDisposable) {
    if (myRootComponent == null) {
      Configurable firstConfigurable = children[0];
      if (children.length == 1) {
        myRootComponent = firstConfigurable.createUIComponent(uiDisposable);
      }
      else {
        VerticalLayout verticalLayout = VerticalLayout.create();
        for (Configurable configurable : children) {
          Component uiComponent = configurable.createUIComponent(uiDisposable);
          if (uiComponent == null) {
            continue;
          }

          LocalizeValue displayName = configurable.getDisplayName();
          if (displayName == LocalizeValue.of()) {
            verticalLayout.add(uiComponent);
          }
          else {
            LabeledLayout labeledLayout = LabeledLayout.create(displayName);
            labeledLayout.set(uiComponent);
            verticalLayout.add(labeledLayout);
          }
        }
        myRootComponent = verticalLayout;
      }
    }
    return myRootComponent;
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent(@Nonnull Disposable parentDisposable) {
    if (!Application.get().isSwingApplication()) {
      return null;
    }

    if (myRootPanel == null) {
      Configurable firstConfigurable = children[0];
      if (children.length == 1) {
        myRootPanel = ConfigurableUIMigrationUtil.createComponent(firstConfigurable, parentDisposable);
      }
      else {
        JPanel panel = createPanel(true);
        for (Configurable configurable : children) {
          JComponent component = ConfigurableUIMigrationUtil.createComponent(configurable, parentDisposable);
          assert component != null;
          LocalizeValue displayName = configurable.getDisplayName();
          if (displayName == LocalizeValue.of()) {
            component.setBorder(BOTTOM_INSETS);
          }
          else {
            component.setBorder(IdeBorderFactory.createTitledBorder(displayName.get(), false, firstConfigurable == configurable ? FIRST_COMPONENT_INSETS : N_COMPONENT_INSETS));
          }
          panel.add(component);
        }
        myRootPanel = panel;
      }
    }
    return myRootPanel;
  }

  @Nonnull
  static JPanel createPanel(boolean isUseTitledBorder) {
    int verticalGap = TitledSeparator.TOP_INSET;
    JPanel panel = new JPanel(new VerticalFlowLayout(0, isUseTitledBorder ? 0 : verticalGap));
    // VerticalFlowLayout incorrectly use vertical gap as top inset
    if (!isUseTitledBorder) {
      panel.setBorder(new EmptyBorder(-verticalGap, 0, 0, 0));
    }
    return panel;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    for (Configurable child : children) {
      if (child.isModified()) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    for (Configurable child : children) {
      if (child.isModified()) {
        child.apply();
      }
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    for (Configurable child : children) {
      child.reset();
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myRootPanel = null;
    myRootComponent = null;

    for (Configurable child : children) {
      child.disposeUIResources();
    }
  }
}