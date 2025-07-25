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
package consulo.language.psi.stub;

import consulo.language.psi.PsiElement;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.KeyDescriptor;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public abstract class IntStubIndexExtension<Psi extends PsiElement> extends AbstractStubIndex<Integer, Psi> {
    public int getVersion() {
        return 1;
    }

    @Nonnull
    public KeyDescriptor<Integer> getKeyDescriptor() {
        return EnumeratorIntegerDescriptor.INSTANCE;
    }
}