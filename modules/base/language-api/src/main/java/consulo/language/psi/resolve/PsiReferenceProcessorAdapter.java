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
package consulo.language.psi.resolve;

import consulo.application.util.ReadActionProcessor;
import consulo.language.psi.PsiReference;
import consulo.annotation.access.RequiredReadAction;

/**
 * @author max
 */
public class PsiReferenceProcessorAdapter extends ReadActionProcessor<PsiReference> {
    private final PsiReferenceProcessor myProcessor;

    public PsiReferenceProcessorAdapter(PsiReferenceProcessor processor) {
        myProcessor = processor;
    }

    @RequiredReadAction
    @Override
    public boolean processInReadAction(PsiReference psiReference) {
        return myProcessor.execute(psiReference);
    }
}