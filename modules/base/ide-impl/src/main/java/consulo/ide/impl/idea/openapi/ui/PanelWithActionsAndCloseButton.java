/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.openapi.ui;

import consulo.application.HelpManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.CloseTabToolbarAction;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.action.*;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.TabbedContent;
import consulo.ui.ex.content.event.ContentManagerAdapter;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.util.dataholder.Key;
import consulo.ui.ex.content.ContentsUtil;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;


public abstract class PanelWithActionsAndCloseButton extends JPanel implements DataProvider, Disposable {
  protected final ContentManager myContentManager;
  private final String myHelpId;
  private final boolean myVerticalToolbar;
  private boolean myCloseEnabled;
  private final DefaultActionGroup myToolbarGroup = new DefaultActionGroup();

  public PanelWithActionsAndCloseButton(ContentManager contentManager, @NonNls String helpId) {
    this(contentManager, helpId, true);
  }

  public PanelWithActionsAndCloseButton(ContentManager contentManager, @NonNls String helpId, boolean verticalToolbar) {
    super(new BorderLayout());
    myContentManager = contentManager;
    myHelpId = helpId;
    myVerticalToolbar = verticalToolbar;
    myCloseEnabled = true;

    if (myContentManager != null) {
      myContentManager.addContentManagerListener(new ContentManagerAdapter(){
        @Override
        public void contentRemoved(ContentManagerEvent event) {
          if (event.getContent().getComponent() == PanelWithActionsAndCloseButton.this) {
            Disposer.dispose(PanelWithActionsAndCloseButton.this);
            myContentManager.removeContentManagerListener(this);
          }
        }
      });
    }

  }

  public String getHelpId() {
    return myHelpId;
  }

  protected void disableClose() {
    myCloseEnabled = false;
  }

  protected void init(){
    addActionsTo(myToolbarGroup);
    myToolbarGroup.add(new MyCloseAction());
    myToolbarGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_CONTEXT_HELP));

    ActionToolbar toolbar = ActionManager.getInstance()
      .createActionToolbar(ActionPlaces.FILEHISTORY_VIEW_TOOLBAR, myToolbarGroup, ! myVerticalToolbar);
    JComponent centerPanel = createCenterPanel();
    toolbar.setTargetComponent(centerPanel);
    for (AnAction action : myToolbarGroup.getChildren(null)) {
      action.registerCustomShortcutSet(action.getShortcutSet(), centerPanel);
    }

    add(centerPanel, BorderLayout.CENTER);
    if (myVerticalToolbar) {
      add(toolbar.getComponent(), BorderLayout.WEST);
    } else {
      add(toolbar.getComponent(), BorderLayout.NORTH);
    }
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    return HelpManager.HELP_ID == dataId ? myHelpId : null;
  }

  protected abstract JComponent createCenterPanel();

  protected void addActionsTo(DefaultActionGroup group) {}

  private class MyCloseAction extends CloseTabToolbarAction {
    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setVisible(myCloseEnabled);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      if (myContentManager != null) {
        Content content = myContentManager.getContent(PanelWithActionsAndCloseButton.this);
        if (content != null) {
          ContentsUtil.closeContentTab(myContentManager, content);
          if (content instanceof TabbedContent && ((TabbedContent)content).getTabs().size() > 1) {
            TabbedContent tabbedContent = (TabbedContent)content;
            JComponent component = content.getComponent();
            tabbedContent.removeContent(component);
            myContentManager.setSelectedContent(content, true, true); //we should request focus here
          } else {
            myContentManager.removeContent(content, true);
          }
        }
      }
    }
  }
}
