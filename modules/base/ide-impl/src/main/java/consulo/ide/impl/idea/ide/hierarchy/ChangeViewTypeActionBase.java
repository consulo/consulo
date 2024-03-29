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

package consulo.ide.impl.idea.ide.hierarchy;

import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.ToggleAction;
import consulo.application.ApplicationManager;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author cdr
 */
abstract class ChangeViewTypeActionBase extends ToggleAction {
  public ChangeViewTypeActionBase(final String shortDescription, final String longDescription, final Image icon) {
    super(shortDescription, longDescription, icon);
  }

  protected ChangeViewTypeActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
    super(text, description);
  }

  @Override
  public final boolean isSelected(final AnActionEvent event) {
    final TypeHierarchyBrowserBase browser = getTypeHierarchyBrowser(event.getDataContext());
    return browser != null && getTypeName().equals(browser.getCurrentViewType());
  }

  protected abstract String getTypeName();

  @Override
  public final void setSelected(final AnActionEvent event, final boolean flag) {
    if (flag) {
      final TypeHierarchyBrowserBase browser = getTypeHierarchyBrowser(event.getDataContext());
      //        setWaitCursor();
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          if (browser != null) {
            browser.changeView(getTypeName());
          }
        }
      });
    }
  }

  @Override
  public void update(final AnActionEvent event) {
    // its important to assign the myTypeHierarchyBrowser first
    super.update(event);
    final Presentation presentation = event.getPresentation();
    final TypeHierarchyBrowserBase browser = getTypeHierarchyBrowser(event.getDataContext());
    presentation.setEnabled(browser != null && browser.isValidBase());
  }

  protected static TypeHierarchyBrowserBase getTypeHierarchyBrowser(final DataContext context) {
    return context.getData(TypeHierarchyBrowserBase.DATA_KEY);
  }
}
