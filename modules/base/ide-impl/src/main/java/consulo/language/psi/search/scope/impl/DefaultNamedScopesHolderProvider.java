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
package consulo.language.psi.search.scope.impl;

import consulo.language.psi.search.scope.NamedScopesHolder;
import consulo.language.psi.search.scope.NamedScopesHolderProvider;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 13-Feb-22
 */
public class DefaultNamedScopesHolderProvider implements NamedScopesHolderProvider {
  private final NamedScopeManager myNamedScopeManager;

  @Inject
  public DefaultNamedScopesHolderProvider(NamedScopeManager namedScopeManager) {
    myNamedScopeManager = namedScopeManager;
  }

  @Nonnull
  @Override
  public NamedScopesHolder getScopesHolder() {
    return myNamedScopeManager;
  }
}
