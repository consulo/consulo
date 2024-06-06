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
package consulo.ide.impl.idea.ui.content.impl;

import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Comparing;
import consulo.util.dataholder.Key;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ide.impl.wm.impl.ContentManagerBase;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@SuppressWarnings("deprecation")
public class DesktopContentManagerImpl extends ContentManagerBase {
  protected JComponent myComponent;

  public DesktopContentManagerImpl(@Nonnull ContentUI contentUI, boolean canCloseContents, @Nonnull Project project) {
    super(contentUI, canCloseContents, project);
  }

  @Override
  protected void updateUI() {
    myUI.getComponent().updateUI();
  }

  @Nonnull
  @Override
  protected AsyncResult<Void> requestFocusForComponent() {
    return getFocusManager().requestFocus(myComponent, true);
  }

  @Override
  protected boolean isSelectionHoldsFocus() {
    boolean focused = false;
    final Content[] selection = getSelectedContents();
    for (Content each : selection) {
      if (UIUtil.isFocusAncestor(each.getComponent())) {
        focused = true;
        break;
      }
    }
    return focused;
  }

  @Override
  public Content getContent(JComponent component) {
    Content[] contents = getContents();
    for (Content content : contents) {
      if (Comparing.equal(component, content.getComponent())) {
        return content;
      }
    }
    return null;
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    if (myComponent == null) {
      myComponent = new MyNonOpaquePanel();

      NonOpaquePanel contentComponent = new NonOpaquePanel();
      contentComponent.setContent(myUI.getComponent());
      contentComponent.setFocusCycleRoot(true);

      myComponent.add(contentComponent, BorderLayout.CENTER);
    }
    return myComponent;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> requestFocus(final Content content, final boolean forced) {
    final Content toSelect = content == null ? getSelectedContent() : content;
    if (toSelect == null) return AsyncResult.rejected();
    assert myContents.contains(toSelect);
    JComponent preferredFocusableComponent = toSelect.getPreferredFocusableComponent();
    return preferredFocusableComponent != null ? getFocusManager().requestFocusInProject(preferredFocusableComponent, myProject) : AsyncResult.rejected();
  }

  private class MyNonOpaquePanel extends NonOpaquePanel implements DataProvider {
    public MyNonOpaquePanel() {
      super(new BorderLayout());
    }

    @Override
    @Nullable
    public Object getData(@Nonnull @NonNls Key<?> dataId) {
      if (PlatformDataKeys.CONTENT_MANAGER == dataId || PlatformDataKeys.NONEMPTY_CONTENT_MANAGER == dataId && getContentCount() > 1) {
        return DesktopContentManagerImpl.this;
      }

      for (DataProvider dataProvider : myDataProviders) {
        Object data = dataProvider.getData(dataId);
        if (data != null) {
          return data;
        }
      }

      if (myUI instanceof DataProvider) {
        return ((DataProvider)myUI).getData(dataId);
      }

      DataProvider provider = DataManager.getDataProvider(this);
      return provider == null ? null : provider.getData(dataId);
    }
  }
}
