/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.content.tabs;

import consulo.annotation.DeprecationInfo;
import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.ui.ShadowAction;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.localize.UILocalize;
import jakarta.annotation.Nonnull;

public abstract class TabbedContentAction extends AnAction implements DumbAware {
  protected final ContentManager myManager;

  protected final ShadowAction myShadow;

  protected TabbedContentAction(@Nonnull ContentManager manager, @Nonnull AnAction shortcutTemplate, @Nonnull LocalizeValue text) {
    super(text);
    myManager = manager;
    myShadow = new ShadowAction(this, shortcutTemplate, manager.getComponent(), new Presentation(text));
  }

  @Deprecated
  @DeprecationInfo("Use variant with LocalizeValue")
  protected TabbedContentAction(@Nonnull ContentManager manager, @Nonnull AnAction shortcutTemplate, @Nonnull String text) {
    this(manager, shortcutTemplate, LocalizeValue.of(text));
  }

  protected TabbedContentAction(@Nonnull ContentManager manager, @Nonnull AnAction template) {
    myManager = manager;
    myShadow = new ShadowAction(this, template, manager.getComponent());
  }

  public abstract static class ForContent extends TabbedContentAction {
    protected final Content myContent;

    public ForContent(@Nonnull Content content, @Nonnull AnAction shortcutTemplate, @Nonnull LocalizeValue text) {
      super(content.getManager(), shortcutTemplate, text);
      myContent = content;
      Disposer.register(content, myShadow);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ForContent(@Nonnull Content content, @Nonnull AnAction shortcutTemplate, String text) {
      super(content.getManager(), shortcutTemplate, text);
      myContent = content;
      Disposer.register(content, myShadow);
    }

    public ForContent(@Nonnull Content content, final AnAction template) {
      super(content.getManager(), template);
      myContent = content;
      Disposer.register(content, myShadow);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(myManager.getIndexOfContent(myContent) >= 0);
    }
  }


  public static class CloseAction extends ForContent {

    public CloseAction(@Nonnull Content content) {
      super(content, ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE_ACTIVE_TAB));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myManager.removeContent(myContent, true);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setEnabled(myContent != null && myManager.canCloseContents() && myContent.isCloseable() && myManager.isSelected(myContent));
      presentation.setVisible(myManager.canCloseContents() && myContent.isCloseable());
      presentation.setText(myManager.getCloseActionName());
    }
  }

  public static class CloseAllButThisAction extends ForContent {

    public CloseAllButThisAction(Content content) {
      super(
          content,
          ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE_ALL_EDITORS_BUT_THIS),
          UILocalize.tabbedPaneCloseAllButThisActionName()
      );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      Content[] contents = myManager.getContents();
      for (Content content : contents) {
        if (myContent != content && content.isCloseable()) {
          myManager.removeContent(content, true);
        }
      }
      myManager.setSelectedContent(myContent);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setText(myManager.getCloseAllButThisActionName());
      presentation.setEnabled(myContent != null && myManager.canCloseContents() && myManager.getContentCount() > 1);
      presentation.setVisible(myManager.canCloseContents() && hasCloseableContents());
    }

    private boolean hasCloseableContents() {
      Content[] contents = myManager.getContents();
      for (Content content : contents) {
        if (myContent != content && content.isCloseable()) {
          return true;
        }
      }
      return false;
    }        
  }

  public static class CloseAllAction extends TabbedContentAction {
    public CloseAllAction(ContentManager manager) {
      super(
        manager,
        ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE_ALL_EDITORS),
        UILocalize.tabbedPaneCloseAllActionName()
      );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      Content[] contents = myManager.getContents();
      for (Content content : contents) {
        if (content.isCloseable()) {
          myManager.removeContent(content, true);
        }
      }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      presentation.setEnabled(myManager.canCloseAllContents());
      presentation.setVisible(myManager.canCloseAllContents());
    }
  }
  public static class MyNextTabAction extends TabbedContentAction {
    public MyNextTabAction(ContentManager manager) {
      super(manager, ActionManager.getInstance().getAction(IdeActions.ACTION_NEXT_TAB));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myManager.selectNextContent();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(myManager.getContentCount() > 1);
      e.getPresentation().setText(myManager.getNextContentActionName());
    }
  }

  public static class MyPreviousTabAction extends TabbedContentAction {
    public MyPreviousTabAction(ContentManager manager) {
      super(manager, ActionManager.getInstance().getAction(IdeActions.ACTION_PREVIOUS_TAB));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myManager.selectPreviousContent();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabled(myManager.getContentCount() > 1);
      e.getPresentation().setText(myManager.getPreviousContentActionName());
    }
  }
}
