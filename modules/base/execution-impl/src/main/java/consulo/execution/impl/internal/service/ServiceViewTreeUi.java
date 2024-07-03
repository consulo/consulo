// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.application.ApplicationManager;
import consulo.dataContext.DataManager;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.internal.ActionToolbarEx;
import consulo.util.collection.JBIterable;
import consulo.util.collection.Sets;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

final class ServiceViewTreeUi implements ServiceViewUi {
  private final JPanel myMainPanel;
  private final SimpleToolWindowPanel myContentPanel = new SimpleToolWindowPanel(false);
  private final Splitter mySplitter;
  private final JPanel myMasterPanel;
  private final JPanel myDetailsPanel;
  private final JPanel myContentComponentPanel;
  private final JBPanelWithEmptyText myMessagePanel =
    new JBPanelWithEmptyText().withEmptyText(ExecutionLocalize.serviceViewEmptySelectionText().get());
  private final Set<JComponent> myDetailsComponents = Sets.newWeakHashSet();
  private ActionToolbar myServiceActionToolbar;
  private JComponent myServiceActionToolbarWrapper;
  private ActionToolbar myMasterActionToolbar;

  ServiceViewTreeUi(@Nonnull ServiceViewState state) {
    myMainPanel = new SimpleToolWindowPanel(false);

    mySplitter = new OnePixelSplitter(false, state.contentProportion);
    myMainPanel.add(myContentPanel, BorderLayout.CENTER);
    myContentPanel.setContent(mySplitter);

    myMasterPanel = new JPanel(new BorderLayout());
    DataManager.registerDataProvider(myMasterPanel, dataId -> {
      if (ServiceViewActionUtils.IS_FROM_TREE_KEY.is(dataId)) {
        return true;
      }
      return null;
    });

    mySplitter.setFirstComponent(myMasterPanel);

    myDetailsPanel = new JPanel(new BorderLayout());
    myContentComponentPanel = new JPanel(new BorderLayout());
    myMessagePanel.setFocusable(true);
    myContentComponentPanel.add(myMessagePanel, BorderLayout.CENTER);
    myDetailsPanel.add(myContentComponentPanel, BorderLayout.CENTER);
    mySplitter.setSecondComponent(myDetailsPanel);

    if (state.showServicesTree) {
    }
    else {
      myMasterPanel.setVisible(false);
    }

    ComponentUtil
      .putClientProperty(myMainPanel, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, (Iterable<? extends Component>)(Iterable<JComponent>)() ->
        JBIterable.from(myDetailsComponents)
                  .append(myMessagePanel)
                  .filter(component -> myContentComponentPanel != component.getParent())
                  .iterator());
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myMainPanel;
  }

  @Override
  public void saveState(@Nonnull ServiceViewState state) {
    state.contentProportion = mySplitter.getProportion();
  }

  @Override
  public void setServiceToolbar(@Nonnull ServiceViewActionProvider actionProvider) {
    boolean inDetails = ServiceViewUIUtils.isNewServicesUIEnabled();
    myServiceActionToolbar = actionProvider.createServiceToolbar(myMainPanel, inDetails);
    if (inDetails) {
      JComponent wrapper = ServiceViewUIUtils.wrapServicesAligned(myServiceActionToolbar);
      myServiceActionToolbarWrapper = actionProvider.wrapServiceToolbar(wrapper, inDetails);
      myDetailsPanel.add(myServiceActionToolbarWrapper, BorderLayout.NORTH);
    }
    else {
      myContentPanel.setToolbar(actionProvider.wrapServiceToolbar(myServiceActionToolbar.getComponent(), inDetails));
    }
  }

  @Override
  public void setMasterComponent(@Nonnull JComponent component, @Nonnull ServiceViewActionProvider actionProvider) {
    myMasterPanel.add(ScrollPaneFactory.createScrollPane(component, SideBorder.TOP), BorderLayout.CENTER);

    myMasterActionToolbar = actionProvider.createMasterComponentToolbar(component);
    myMasterPanel.add(ServiceViewUIUtils.wrapServicesAligned(myMasterActionToolbar), BorderLayout.NORTH);
    myMasterPanel.updateUI();

    actionProvider.installPopupHandler(component);
  }

  @Override
  public void setMasterComponentVisible(boolean visible) {
    myMasterPanel.setVisible(visible);
  }

  @Override
  public void setDetailsComponent(@Nullable JComponent component) {
    if (component == null) {
      component = myMessagePanel;
    }
    if (component.getParent() != myContentComponentPanel) {
      if (ServiceViewUIUtils.isNewServicesUIEnabled()) {
        boolean visible = ServiceViewActionProvider.isActionToolBarRequired(component);
        myServiceActionToolbarWrapper.setVisible(visible);
        if (visible) {
          myContentComponentPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));
        }
        else {
          myContentComponentPanel.setBorder(null);
        }
      }

      myDetailsComponents.add(component);
      myContentComponentPanel.removeAll();
      myContentComponentPanel.add(component, BorderLayout.CENTER);
      myContentComponentPanel.revalidate();
      myContentComponentPanel.repaint();
    }
    ActionToolbar serviceActionToolbar = myServiceActionToolbar;
    if (serviceActionToolbar != null) {
      ((ActionToolbarEx)serviceActionToolbar).reset();
      serviceActionToolbar.updateActionsImmediately();
    }
    ApplicationManager.getApplication().invokeLater(() -> {
      ActionToolbar masterActionToolbar = myMasterActionToolbar;
      if (masterActionToolbar != null) {
        masterActionToolbar.updateActionsImmediately();
      }
    });
  }

  @Nullable
  @Override
  public JComponent getDetailsComponent() {
    int count = myContentComponentPanel.getComponentCount();
    if (count == 0) return null;

    Component component = myContentComponentPanel.getComponent(0);
    return component == myMessagePanel ? null : (JComponent)component;
  }
}
