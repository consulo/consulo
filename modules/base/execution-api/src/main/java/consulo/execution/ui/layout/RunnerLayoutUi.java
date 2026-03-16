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
import org.jspecify.annotations.Nullable;

import javax.swing.*;

public interface RunnerLayoutUi  {

  
  LayoutStateDefaults getDefaults();

  
  LayoutViewOptions getOptions();

  
  ContentManager getContentManager();

  
  Content addContent(Content content);

  
  Content addContent(Content content, int defaultTabId, PlaceInGrid defaultPlace, boolean defaultIsMinimized);

  
  Content createContent(String contentId, JComponent component, String displayName, @Nullable Image icon, @Nullable JComponent toFocus);

  
  Content createContent(String contentId, ComponentWithActions contentWithActions, String displayName, @Nullable Image icon, @Nullable JComponent toFocus);

  boolean removeContent(@Nullable Content content, boolean dispose);

  @Nullable
  Content findContent(String contentId);

  
  ActionCallback selectAndFocus(@Nullable Content content, boolean requestFocus, boolean forced);
  
  ActionCallback selectAndFocus(@Nullable Content content, boolean requestFocus, boolean forced, boolean implicit);

  
  RunnerLayoutUi addListener(ContentManagerListener listener, Disposable parent);

  void removeListener(ContentManagerListener listener);

  void attractBy(String condition);
  void clearAttractionBy(String condition);

  void setBouncing(Content content, boolean activate);

  
  JComponent getComponent();

  boolean isDisposed();

  void updateActionsNow(UIAccess uiAccess);

  
  Content[] getContents();

}