// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.util;

import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * Default non-language impl of CachedValuesFactory, which can be used in non-language tests
 * @author Dmitry Avdeev
 */
public class DefaultCachedValuesFactory implements CachedValuesFactory {
  private final Project myProject;

  public DefaultCachedValuesFactory(@Nonnull Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public <T> CachedValue<T> createCachedValue(@Nonnull CachedValueProvider<T> provider, boolean trackValue) {
    return new CachedValueImpl<T>(provider, trackValue, this) {
      @Override
      public boolean isFromMyProject(@Nonnull Project project) {
        return myProject == project;
      }
    };
  }

  @Nonnull
  @Override
  public <T, P> ParameterizedCachedValue<T, P> createParameterizedCachedValue(@Nonnull ParameterizedCachedValueProvider<T, P> provider, boolean trackValue) {
    return new ParameterizedCachedValueImpl<>(myProject, provider, trackValue, this);
  }
}
