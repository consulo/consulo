/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.psi.event;

import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class PsiTreeAnyChangeAbstractAdapter extends PsiTreeChangeAdapter {
    @Override
    public void childAdded(@Nonnull PsiTreeChangeEvent event) {
        onChange(event.getFile());
    }

    @Override
    public void childRemoved(@Nonnull PsiTreeChangeEvent event) {
        onChange(event.getFile());
    }

    @Override
    public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
        onChange(event.getFile());
    }

    @Override
    public void childMoved(@Nonnull PsiTreeChangeEvent event) {
        onChange(event.getFile());
    }

    @Override
    public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
        onChange(event.getFile());
    }

    @Override
    public void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
        onChange(event.getFile());
    }

    protected abstract void onChange(@Nullable PsiFile file);
}
