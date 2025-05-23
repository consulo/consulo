/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.inline;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.logging.Logger;
import consulo.usage.UsageInfo;
import consulo.util.collection.MultiMap;

import java.util.*;

/**
 * @author ven
 */
public class GenericInlineHandler {
    private static final Logger LOG = Logger.getInstance(GenericInlineHandler.class);

    @RequiredReadAction
    public static Map<Language, InlineHandler.Inliner> initializeInliners(
        PsiElement element,
        InlineHandler.Settings settings,
        Collection<? extends PsiReference> allReferences
    ) {
        final Map<Language, InlineHandler.Inliner> inliners = new HashMap<>();
        for (PsiReference ref : allReferences) {
            if (ref == null) {
                LOG.error("element: " + element.getClass() + ", allReferences contains null!");
                continue;
            }
            PsiElement refElement = ref.getElement();
            LOG.assertTrue(refElement != null, ref.getClass().getName());

            final Language language = refElement.getLanguage();
            if (inliners.containsKey(language)) {
                continue;
            }

            final List<InlineHandler> handlers = InlineHandler.forLanguage(language);
            for (InlineHandler handler : handlers) {
                InlineHandler.Inliner inliner = handler.createInliner(element, settings);
                if (inliner != null) {
                    inliners.put(language, inliner);
                    break;
                }
            }
        }
        return inliners;
    }

    @RequiredReadAction
    public static void collectConflicts(
        PsiReference reference,
        PsiElement element,
        Map<Language, InlineHandler.Inliner> inliners,
        MultiMap<PsiElement, String> conflicts
    ) {
        PsiElement referenceElement = reference.getElement();
        if (referenceElement == null) {
            return;
        }
        Language language = referenceElement.getLanguage();
        InlineHandler.Inliner inliner = inliners.get(language);
        if (inliner != null) {
            MultiMap<PsiElement, String> refConflicts = inliner.getConflicts(reference, element);
            if (refConflicts != null) {
                for (PsiElement psiElement : refConflicts.keySet()) {
                    conflicts.putValues(psiElement, refConflicts.get(psiElement));
                }
            }
        }
        else {
            conflicts.putValue(referenceElement, "Cannot inline reference from " + language.getID());
        }
    }

    @RequiredWriteAction
    public static void inlineReference(UsageInfo usage, PsiElement element, Map<Language, InlineHandler.Inliner> inliners) {
        PsiElement usageElement = usage.getElement();
        if (usageElement == null) {
            return;
        }
        Language language = usageElement.getLanguage();
        InlineHandler.Inliner inliner = inliners.get(language);
        if (inliner != null) {
            inliner.inlineUsage(usage, element);
        }
    }

    //order of usages across different files is irrelevant
    @RequiredReadAction
    public static PsiReference[] sortDepthFirstRightLeftOrder(Collection<? extends PsiReference> allReferences) {
        PsiReference[] usages = allReferences.toArray(new PsiReference[allReferences.size()]);
        Arrays.sort(
            usages,
            (usage1, usage2) -> {
                final PsiElement element1 = usage1.getElement();
                final PsiElement element2 = usage2.getElement();
                if (element1 == null || element2 == null) {
                    return 0;
                }
                return element2.getTextRange().getStartOffset() - element1.getTextRange().getStartOffset();
            }
        );
        return usages;
    }
}
