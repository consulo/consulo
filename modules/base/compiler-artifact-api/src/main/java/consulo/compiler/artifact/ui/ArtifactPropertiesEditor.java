/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.artifact.ui;

import consulo.compiler.localize.CompilerLocalize;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author nik
 */
public abstract class ArtifactPropertiesEditor {
    public static final LocalizeValue VALIDATION_TAB = CompilerLocalize.artifactPropertiesTabValidation();
    public static final LocalizeValue POST_PROCESSING_TAB = CompilerLocalize.artifactPropertiesTabPostProcessing();
    public static final LocalizeValue PRE_PROCESSING_TAB = CompilerLocalize.artifactPropertiesTabPreProcessing();

    public abstract LocalizeValue getTabName();

    @RequiredUIAccess
    public abstract Component createUIComponent(Disposable uiDisposable);

    @RequiredUIAccess
    public abstract boolean isModified();

    @RequiredUIAccess
    public abstract void apply();

    @RequiredUIAccess
    public abstract void reset();

    @RequiredUIAccess
    public void disposeUIResources() {
    }
}
