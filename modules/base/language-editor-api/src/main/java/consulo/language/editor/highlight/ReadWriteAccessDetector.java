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

package consulo.language.editor.highlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;

import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ReadWriteAccessDetector {
    public static final ExtensionPointName<ReadWriteAccessDetector> EP_NAME = ExtensionPointName.create(ReadWriteAccessDetector.class);

    @Nullable
    public static ReadWriteAccessDetector findDetector(final PsiElement element) {
        ReadWriteAccessDetector detector = null;
        for (ReadWriteAccessDetector accessDetector : EP_NAME.getExtensionList()) {
            if (accessDetector.isReadWriteAccessible(element)) {
                detector = accessDetector;
                break;
            }
        }
        return detector;
    }

    public enum Access {
        Read,
        Write,
        ReadWrite;

        public boolean isReferencedForRead() {
            return this == Read || this == ReadWrite;
        }

        public boolean isReferencedForWrite() {
            return this == Write || this == ReadWrite;
        }
    }

    public abstract boolean isReadWriteAccessible(PsiElement element);

    public abstract boolean isDeclarationWriteAccess(PsiElement element);

    public abstract Access getReferenceAccess(final PsiElement referencedElement, PsiReference reference);

    public abstract Access getExpressionAccess(PsiElement expression);
}
