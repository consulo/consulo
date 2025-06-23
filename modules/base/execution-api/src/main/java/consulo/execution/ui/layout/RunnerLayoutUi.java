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

package consulo.execution.ui.layout;

import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.ex.ComponentWithActions;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.image.Image;
import consulo.util.concurrent.ActionCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public interface RunnerLayoutUi  {

  @Nonnull
  LayoutStateDefaults getDefaults();

  @Nonnull
  LayoutViewOptions getOptions();

  @Nonnull
  ContentManager getContentManager();

  @Nonnull
  Content addContent(@Nonnull Content content);

  @Nonnull
  Content addContent(@Nonnull Content content, int defaultTabId, @Nonnull PlaceInGrid defaultPlace, boolean defaultIsMinimized);

  @Nonnull
  Content createContent(@Nonnull String contentId, @Nonnull JComponent component, @Nonnull String displayName, @Nullable Image icon, @Nullable JComponent toFocus);

  @Nonnull
  Content createContent(@Nonnull String contentId, @Nonnull ComponentWithActions contentWithActions, @Nonnull String displayName, @Nullable Image icon, @Nullable JComponent toFocus);

  boolean removeContent(@Nullable Content content, boolean dispose);

  @Nullable
  Content findContent(@Nonnull String contentId);

  @Nonnull
  ActionCallback selectAndFocus(@Nullable Content content, boolean requestFocus, final boolean forced);
  @Nonnull
  ActionCallback selectAndFocus(@Nullable Content content, boolean requestFocus, final boolean forced, final boolean implicit);

  @Nonnull
  RunnerLayoutUi addListener(@Nonnull ContentManagerListener listener, @Nonnull Disposable parent);

  void removeListener(@Nonnull final ContentManagerListener listener);

  void attractBy(@Nonnull String condition);
  void clearAttractionBy(@Nonnull String condition);

  void setBouncing(@Nonnull Content content, final boolean activate);

  @Nonnull
  JComponent getComponent();

  boolean isDisposed();

  void updateActionsNow(UIAccess uiAccess);

  @Nonnull
  Content[] getContents();

}