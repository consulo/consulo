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
package com.intellij.openapi.vcs.impl;

import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.checkin.BaseCheckinHandlerFactory;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.checkin.VcsCheckinHandlerFactory;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;

import javax.annotation.Nonnull;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author irengrig
 * Date: 1/28/11
 * Time: 5:21 PM
 */
@Singleton
public class CheckinHandlersManagerImpl extends CheckinHandlersManager {
  private final List<BaseCheckinHandlerFactory> myRegisteredBeforeCheckinHandlers;
  private final MultiMap<VcsKey, VcsCheckinHandlerFactory> myVcsMap;

  public CheckinHandlersManagerImpl() {
    myVcsMap = new MultiMap<>();
    myRegisteredBeforeCheckinHandlers = new ArrayList<>();

    myRegisteredBeforeCheckinHandlers.addAll(CheckinHandlerFactory.EP_NAME.getExtensionList());
    for (VcsCheckinHandlerFactory factory : VcsCheckinHandlerFactory.EP_NAME.getExtensionList()) {
      myVcsMap.putValue(factory.getKey(), factory);
    }
  }

  @Override
  public List<BaseCheckinHandlerFactory> getRegisteredCheckinHandlerFactories(AbstractVcs[] allActiveVcss) {
    final ArrayList<BaseCheckinHandlerFactory> list = new ArrayList<>(myRegisteredBeforeCheckinHandlers.size() + allActiveVcss.length);
    list.addAll(myRegisteredBeforeCheckinHandlers);
    for (AbstractVcs vcs : allActiveVcss) {
      final Collection<VcsCheckinHandlerFactory> factories = myVcsMap.get(vcs.getKeyInstanceMethod());
      if (!factories.isEmpty()) {
        list.addAll(factories);
      }
    }
    return list;
  }

  @Override
  public List<VcsCheckinHandlerFactory> getMatchingVcsFactories(@Nonnull List<AbstractVcs> vcsList) {
    final SmartList<VcsCheckinHandlerFactory> result = new SmartList<>();
    for (AbstractVcs vcs : vcsList) {
      final Collection<VcsCheckinHandlerFactory> factories = myVcsMap.get(vcs.getKeyInstanceMethod());
      if (!factories.isEmpty()) {
        result.addAll(factories);
      }
    }
    return result;
  }

  @Override
  public void registerCheckinHandlerFactory(BaseCheckinHandlerFactory factory) {
    myRegisteredBeforeCheckinHandlers.add(factory);
  }

  @Override
  public void unregisterCheckinHandlerFactory(BaseCheckinHandlerFactory handler) {
    myRegisteredBeforeCheckinHandlers.remove(handler);
  }
}
