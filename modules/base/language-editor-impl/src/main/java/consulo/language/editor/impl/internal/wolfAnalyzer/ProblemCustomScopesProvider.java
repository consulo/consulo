/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.impl.internal.wolfAnalyzer;

import consulo.annotation.component.ExtensionImpl;
import consulo.content.internal.scope.CustomScopesProvider;
import consulo.content.scope.NamedScope;
import consulo.language.editor.wolfAnalyzer.ProblemScopeHolder;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-04-12
 */
@ExtensionImpl
public class ProblemCustomScopesProvider implements CustomScopesProvider {
    private final ProblemScopeHolder myProblemScopeHolder;

    @Inject
    public ProblemCustomScopesProvider(ProblemScopeHolder problemScopeHolder) {
        myProblemScopeHolder = problemScopeHolder;
    }

    @Override
    public void acceptScopes(@Nonnull Consumer<NamedScope> consumer) {
        consumer.accept(myProblemScopeHolder.getProblemsScope());
    }
}
