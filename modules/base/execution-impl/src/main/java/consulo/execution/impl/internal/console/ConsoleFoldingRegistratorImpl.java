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
package consulo.execution.impl.internal.console;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.execution.ui.console.ConsoleFoldingContributor;
import consulo.execution.ui.console.ConsoleFoldingRegistrator;

import jakarta.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 01-Aug-22
 */
public class ConsoleFoldingRegistratorImpl implements ConsoleFoldingRegistrator {
  private static final ExtensionPointCacheKey<ConsoleFoldingContributor, ConsoleFoldingRegistratorImpl> REG_KEY = ExtensionPointCacheKey.create("ConsoleFoldingRegistratorImpl", walker -> {
    ConsoleFoldingRegistratorImpl impl = new ConsoleFoldingRegistratorImpl();
    walker.walk(contributor -> contributor.register(impl));
    return impl;
  });

  @Nonnull
  public static ConsoleFoldingRegistratorImpl last() {
    return Application.get().getExtensionPoint(ConsoleFoldingContributor.class).getOrBuildCache(REG_KEY);
  }

  private final Set<String> myAddSet = new LinkedHashSet<>();
  private final Set<String> myRemoveSet = new LinkedHashSet<>();

  @Override
  public void addFolding(@Nonnull String line) {
    myAddSet.add(line);
  }

  @Override
  public void removeFolding(@Nonnull String line) {
    myRemoveSet.add(line);
  }

  public Set<String> getAddSet() {
    return myAddSet;
  }

  public Set<String> getRemoveSet() {
    return myRemoveSet;
  }
}
