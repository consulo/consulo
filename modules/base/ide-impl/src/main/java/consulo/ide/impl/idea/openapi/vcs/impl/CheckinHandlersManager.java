/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.checkin.BaseCheckinHandlerFactory;
import consulo.versionControlSystem.checkin.VcsCheckinHandlerFactory;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author irengrig
 * @since 2011-01-28
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CheckinHandlersManager {
  public static CheckinHandlersManager getInstance() {
    return ServiceManager.getService(CheckinHandlersManager.class);
  }

  /**
   * Returns the list of all registered factories which provide callbacks to run before and after
   * VCS checkin operations.
   *
   * @return the list of registered factories.
   * @param allActiveVcss
   */
  public abstract List<BaseCheckinHandlerFactory> getRegisteredCheckinHandlerFactories(AbstractVcs[] allActiveVcss);

  public abstract List<VcsCheckinHandlerFactory> getMatchingVcsFactories(@Nonnull final List<AbstractVcs> keys);
  /**
   * Registers a factory which provides callbacks to run before and after VCS checkin operations.
   *
   * @param factory the factory to register.
   */
  public abstract void registerCheckinHandlerFactory(BaseCheckinHandlerFactory factory);
  /**
   * Unregisters a factory which provides callbacks to run before and after VCS checkin operations.
   *
   * @param factory the factory to unregister.
   */
  public abstract void unregisterCheckinHandlerFactory(BaseCheckinHandlerFactory handler);
}
