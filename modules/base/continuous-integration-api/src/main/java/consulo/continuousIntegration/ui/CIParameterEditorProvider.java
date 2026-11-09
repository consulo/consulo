/*
 * Copyright 2013-2026 consulo.io
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
package consulo.continuousIntegration.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIParameterDefinition;
import consulo.continuousIntegration.CIServer;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Editors for provider specific parameter types. The impl has editors for every {@link consulo.continuousIntegration.CIParameterKind}
 * and asks providers first.
 *
 * @since 2026-11-09
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CIParameterEditorProvider {
    boolean accepts(CIServer server, CIParameterDefinition definition);

    @RequiredUIAccess
    CIParameterEditor create(CIServer server, CIJob job, CIParameterDefinition definition, Disposable parentDisposable);
}
