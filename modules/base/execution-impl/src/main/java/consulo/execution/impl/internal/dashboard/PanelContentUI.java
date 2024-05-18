// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.util.collection.JBIterable;
import consulo.util.lang.function.Conditions;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * PanelContentUI simply shows selected content in a panel.
 *
 * @author konstantin.aleev
 */
final class PanelContentUI implements ContentUI {
  private JPanel myPanel;
  private ContentManager myContentManager;

  PanelContentUI() {
  }

  @Override
  public JComponent getComponent() {
    initUI();
    return myPanel;
  }

  @Override
  public void setManager(ContentManager manager) {
    assert myContentManager == null;
    myContentManager = manager;
    manager.addContentManagerListener(new ContentManagerListener() {
      @RequiredUIAccess
      @Override
      public void selectionChanged(final ContentManagerEvent event) {
        initUI();
        if (ContentManagerEvent.ContentOperation.add == event.getOperation()) {
          showContent(event.getContent());
        }
        else if (ContentManagerEvent.ContentOperation.remove == event.getOperation()) {
          hideContent();
        }
      }
    });
  }

  private void initUI() {
    if (myPanel != null) {
      return;
    }
    myPanel = new JPanel(new BorderLayout());
    ComponentUtil
      .putClientProperty(myPanel, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, (Iterable<? extends Component>)(Iterable<JComponent>)() -> {
        if (myContentManager == null || myContentManager.getContentCount() == 0) {
          return Collections.emptyIterator();
        }
        return JBIterable.of(myContentManager.getContents())
                         .map(content -> {
                           JComponent component = content.getComponent();
                           return myPanel != component.getParent() ? component : null;
                         })
                         .filter(Conditions.notNull())
                         .iterator();
      });
  }

  private void showContent(Content content) {
    if (myPanel.getComponentCount() != 1 ||
      myPanel.getComponent(0) != content.getComponent()) {
      myPanel.removeAll();
      myPanel.add(content.getComponent(), BorderLayout.CENTER);

      myPanel.revalidate();
      myPanel.repaint();
    }
  }

  private void hideContent() {
    myPanel.removeAll();
    myPanel.revalidate();
    myPanel.repaint();
  }

  @Override
  public boolean isSingleSelection() {
    return true;
  }

  @Override
  public boolean isToSelectAddedContent() {
    return true;
  }

  @Override
  public boolean canBeEmptySelection() {
    return false;
  }

  @Override
  public void beforeDispose() {

  }

  @Override
  public boolean canChangeSelectionTo(Content content, boolean implicit) {
    return true;
  }

  @Override
  public String getCloseActionName() {
    return "";
  }

  @Override
  public String getCloseAllButThisActionName() {
    return "";
  }

  @Override
  public String getPreviousContentActionName() {
    return "";
  }

  @Override
  public String getNextContentActionName() {
    return "";
  }

  @Override
  public void dispose() {

  }
}
