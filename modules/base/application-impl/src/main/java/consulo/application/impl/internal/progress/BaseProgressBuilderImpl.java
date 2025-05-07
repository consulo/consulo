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
package consulo.application.impl.internal.progress;

import consulo.application.progress.ProgressBuilder;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-05-06
 */
public abstract class BaseProgressBuilderImpl implements ProgressBuilder {
    protected final ComponentManager myProject;
    protected final LocalizeValue myTitle;
    protected boolean myCancelable;
    protected boolean myModal;

    private boolean myCreated;

    public BaseProgressBuilderImpl(ComponentManager project, LocalizeValue title) {
        myProject = project;
        myTitle = title;
    }

    protected void assertCreated() {
        if (!myCreated) {
            myCreated = true;
            return;
        }

        throw new IllegalArgumentException("Duplicate using of builder");
    }

    @Nonnull
    @Override
    public ProgressBuilder modal() {
        myModal = true;
        return this;
    }

    @Nonnull
    @Override
    public ProgressBuilder cancelable() {
        myCancelable = true;
        return this;
    }
}
