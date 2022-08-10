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
package consulo.ide.impl.project.ui.view.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.function.ThrowableComputable;
import consulo.language.impl.internal.psi.AstLoadingFilter;
import consulo.project.ui.view.internal.ProjectViewInternalHelper;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 10-Aug-22
 */
@Singleton
@ServiceImpl
public class ProjectViewInternalHelperImpl implements ProjectViewInternalHelper {
  @Override
  public <T, E extends Throwable> T disallowTreeLoading(@Nonnull ThrowableComputable<? extends T, E> computable, @Nonnull Supplier<String> debugInfo) throws E {
    return AstLoadingFilter.disallowTreeLoading(computable, debugInfo);
  }
}
