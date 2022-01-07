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
package com.intellij.ui.content;

import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.BusyObject;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface ContentManager extends Disposable, BusyObject {
  boolean canCloseContents();

  void addContent(@Nonnull Content content);

  void addContent(@Nonnull Content content, final int order);

  void addContent(@Nonnull Content content, Object constraints);

  boolean removeContent(@Nonnull Content content, final boolean dispose);

  @Nonnull
  AsyncResult<Void> removeContent(@Nonnull Content content, final boolean dispose, boolean trackFocus, boolean forcedFocus);

  void setSelectedContent(@Nonnull Content content);

  @Nonnull
  AsyncResult<Void> setSelectedContentCB(@Nonnull Content content);

  void setSelectedContent(@Nonnull Content content, boolean requestFocus);

  @Nonnull
  AsyncResult<Void> setSelectedContentCB(@Nonnull Content content, boolean requestFocus);

  void setSelectedContent(@Nonnull Content content, boolean requestFocus, boolean forcedFocus);

  @Nonnull
  AsyncResult<Void> setSelectedContentCB(@Nonnull Content content, boolean requestFocus, boolean forcedFocus);

  @Nonnull
  AsyncResult<Void> setSelectedContent(@Nonnull Content content, boolean requestFocus, boolean forcedFocus, boolean implicit);

  void addSelectedContent(@Nonnull Content content);

  @Nullable
  Content getSelectedContent();

  @Nonnull
  Content[] getSelectedContents();

  void removeAllContents(final boolean dispose);

  int getContentCount();

  @Nonnull
  Content[] getContents();

  //TODO[anton,vova] is this method needed?
  Content findContent(String displayName);

  @Nullable
  Content getContent(int index);

  int getIndexOfContent(Content content);

  @Nonnull
  String getCloseActionName();

  boolean canCloseAllContents();

  AsyncResult<Void> selectPreviousContent();

  AsyncResult<Void> selectNextContent();

  void addContentManagerListener(@Nonnull ContentManagerListener l);

  void removeContentManagerListener(@Nonnull ContentManagerListener l);

  /**
   * Returns the localized name of the "Close All but This" action.
   *
   * @return the action name.
   * @since 5.1
   */
  @Nonnull
  String getCloseAllButThisActionName();

  @Nonnull
  String getPreviousContentActionName();

  @Nonnull
  String getNextContentActionName();

  List<AnAction> getAdditionalPopupActions(@Nonnull Content content);

  void removeFromSelection(@Nonnull Content content);

  boolean isSelected(@Nonnull Content content);

  @Nonnull
  AsyncResult<Void> requestFocus(@Nullable Content content, boolean forced);

  void addDataProvider(@Nonnull DataProvider provider);

  @Nonnull
  default ContentFactory getFactory() {
    return ContentFactory.getInstance();
  }

  boolean isDisposed();

  boolean isSingleSelection();

  default Content getContent(@Nonnull Component component) {
    throw new AbstractMethodError();
  }

  @Nonnull
  @RequiredUIAccess
  default Component getUIComponent() {
    throw new AbstractMethodError();
  }

  // TODO [VISTALL] awt & swing dependency
  // region awt & swing dependency
  @Nonnull
  default javax.swing.JComponent getComponent() {
    throw new AbstractMethodError();
  }

  default Content getContent(javax.swing.JComponent component) {
    throw new AbstractMethodError();
  }
  // endregion
}
