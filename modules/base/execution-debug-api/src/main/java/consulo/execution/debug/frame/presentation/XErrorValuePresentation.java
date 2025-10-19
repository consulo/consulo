/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.frame.presentation;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * Renders a value as an error
 *
 * @author egor
 */
public class XErrorValuePresentation extends XValuePresentation {
    private final LocalizeValue myError;

    @Deprecated
    @DeprecationInfo("Use with localize value")
    public XErrorValuePresentation(@Nonnull String error) {
        this(LocalizeValue.of(error));
    }

    public XErrorValuePresentation(@Nonnull LocalizeValue error) {
        myError = error;
    }

    @Override
    public void renderValue(@Nonnull XValueTextRenderer renderer) {
        renderer.renderError(myError);
    }
}
