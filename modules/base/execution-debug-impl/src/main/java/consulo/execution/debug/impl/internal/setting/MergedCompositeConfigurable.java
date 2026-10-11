package consulo.execution.debug.impl.internal.setting;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

class MergedCompositeConfigurable implements SearchableConfigurable {
  static final EmptyBorder BOTTOM_INSETS = new EmptyBorder(0, 0, IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET, 0);

  protected final Configurable[] children;

  private Component myRootComponent;

  private final String id;
  private final LocalizeValue displayName;

  public MergedCompositeConfigurable(String id, LocalizeValue displayName, Configurable[] children) {
    this.children = children;
    this.id = id;
    this.displayName = displayName;
  }

  
  @Override
  public String getId() {
    return id;
  }

  
  @Override
  public LocalizeValue getDisplayName() {
    return displayName;
  }

  @Override
  public @Nullable String getHelpTopic() {
    return children.length == 1 ? children[0].getHelpTopic() : null;
  }

  @RequiredUIAccess
  @Override
  public @Nullable Component createUIComponent(Disposable uiDisposable) {
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
          if (displayName.isEmpty()) {
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
    myRootComponent = null;

    for (Configurable child : children) {
      child.disposeUIResources();
    }
  }
}