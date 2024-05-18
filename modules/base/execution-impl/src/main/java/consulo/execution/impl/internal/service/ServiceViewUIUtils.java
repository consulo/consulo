// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.util.function.Supplier;

public final class ServiceViewUIUtils {
  private ServiceViewUIUtils() {
  }

  public static boolean isNewServicesUIEnabled() {
    return false; // TODO enable it?
  }

  public static @Nonnull JComponent wrapServicesAligned(@Nonnull ActionToolbar toolbar) {
    JComponent toolbarComponent = toolbar.getComponent();
    toolbarComponent.setBorder(JBUI.Borders.empty());
    return new NonOpaquePanel(toolbarComponent) {
//      @Override
//      public Dimension getPreferredSize() {
//        Dimension size = super.getPreferredSize();
//        if (size.height > 0) {
//          size.height = JBUI.scale(JBUI.unscale(JBRunnerTabs.getTabLabelPreferredHeight()) - 1); // without bottom border
//        }
//        return size;
//      }
    };
  }

  private static final class TabbedPaneToolbarWrapper extends NonOpaquePanel implements UIResource {
    TabbedPaneToolbarWrapper() {
      super(new BorderLayout());
    }
  }

  private static final class ServicesTabbedPaneContentManagerListener implements ContentManagerListener {
    private final Supplier<JComponent> myToolbarWrapperSupplier;

    ServicesTabbedPaneContentManagerListener(Supplier<JComponent> toolbarWrapperSupplier) {
      myToolbarWrapperSupplier = toolbarWrapperSupplier;
    }

    @Override
    public void selectionChanged(@Nonnull ContentManagerEvent event) {
      JComponent toolbarWrapper = myToolbarWrapperSupplier.get();
      if (toolbarWrapper == null) return;

      int index = event.getIndex();
      if (index != -1 && event.getOperation() != ContentManagerEvent.ContentOperation.remove) {
        Content content = event.getContent();
        ActionGroup actionGroup = content.getActions();
        if (actionGroup != null) {
          toolbarWrapper.setVisible(true);

          ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(content.getPlace(), actionGroup, true);
          toolbar.setTargetComponent(content.getActionsContextComponent());
          toolbarWrapper.removeAll();
          toolbarWrapper.add(wrapServicesAligned(toolbar), BorderLayout.CENTER);

          toolbarWrapper.revalidate();
          toolbarWrapper.repaint();
        }
        else {
          toolbarWrapper.setVisible(false);
        }
      }
    }
  }
}
