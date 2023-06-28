/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.ex.action.*;
import consulo.util.concurrent.CancellablePromise;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 27/06/2023
 */
public class ActionGroupExpander {

  /**
   * @return actions from the given and nested non-popup groups that are visible after updating
   */
  public static List<AnAction> expandActionGroup(boolean isInModalContext,
                                                 @Nonnull ActionGroup group,
                                                 PresentationFactory presentationFactory,
                                                 @Nonnull DataContext context,
                                                 String place) {
    return new ActionUpdater(ActionManager.getInstance(),
                             isInModalContext,
                             presentationFactory,
                             context,
                             place,
                             false,
                             false
    ).expandActionGroup(group, group instanceof CompactActionGroup);
  }

  public static CancellablePromise<List<AnAction>> expandActionGroupAsync(boolean isInModalContext,
                                                                          @Nonnull ActionGroup group,
                                                                          BasePresentationFactory presentationFactory,
                                                                          @Nonnull DataContext context,
                                                                          String place) {
    if (!(context instanceof AsyncDataContext)) context = DataManager.getInstance().createAsyncDataContext(context);
    return new ActionUpdater(ActionManager.getInstance(), isInModalContext, presentationFactory, context, place, false, false)
      .expandActionGroupAsync(group, group instanceof CompactActionGroup);
  }

  public static List<AnAction> expandActionGroupWithTimeout(boolean isInModalContext,
                                                            @Nonnull ActionGroup group,
                                                            BasePresentationFactory presentationFactory,
                                                            @Nonnull DataContext context,
                                                            String place,
                                                            int timeoutMs) {
    return new ActionUpdater(ActionManager.getInstance(),
                             isInModalContext,
                             presentationFactory,
                             context,
                             place,
                             false,
                             false
    ).expandActionGroupWithTimeout
      (group, group instanceof CompactActionGroup, timeoutMs);
  }
}
