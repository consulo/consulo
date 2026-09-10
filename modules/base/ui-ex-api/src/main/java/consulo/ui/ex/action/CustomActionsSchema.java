/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 20-Mar-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CustomActionsSchema {
  /**
   * The schema loads its state through coroutines, so it can never be created on the UI thread. The returned
   * future is already completed once the schema exists, which is the case for every call after the first.
   */
  static CompletableFuture<CustomActionsSchema> getInstanceAsync() {
    return Application.get().getInstanceAsync(CustomActionsSchema.class);
  }

  static CompletableFuture<@Nullable AnAction> getCorrectedActionAsync(String id) {
    return getInstanceAsync().thenApply(schema -> schema.getCorrectedAction(id));
  }

  static CompletableFuture<@Nullable ActionGroup> getCorrectedGroupAsync(String id) {
    return getCorrectedActionAsync(id).thenApply(action -> action instanceof ActionGroup group ? group : null);
  }

  @Nullable AnAction getCorrectedAction(String id);
}
