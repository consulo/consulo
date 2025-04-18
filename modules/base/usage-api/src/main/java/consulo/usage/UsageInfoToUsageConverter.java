/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.usage;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.highlight.ReadWriteAccessDetector;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author Eugene Zhuravlev
 * @since 2005-01-17
 */
public class UsageInfoToUsageConverter {
    private UsageInfoToUsageConverter() {
    }

    public static class TargetElementsDescriptor {
        private final List<SmartPsiElementPointer<PsiElement>> myPrimarySearchedElements;
        private final List<SmartPsiElementPointer<PsiElement>> myAdditionalSearchedElements;

        public TargetElementsDescriptor(@Nonnull PsiElement element) {
            this(new PsiElement[]{element});
        }

        public TargetElementsDescriptor(@Nonnull PsiElement[] primarySearchedElements) {
            this(primarySearchedElements, PsiElement.EMPTY_ARRAY);
        }

        public TargetElementsDescriptor(@Nonnull PsiElement[] primarySearchedElements, @Nonnull PsiElement[] additionalSearchedElements) {
            myPrimarySearchedElements = convertToSmartPointers(primarySearchedElements);
            myAdditionalSearchedElements = convertToSmartPointers(additionalSearchedElements);
        }

        private static final Function<SmartPsiElementPointer<PsiElement>, PsiElement> SMARTPOINTER_TO_ELEMENT_MAPPER =
            SmartPsiElementPointer::getElement;

        @Nonnull
        private static PsiElement[] convertToPsiElements(@Nonnull List<SmartPsiElementPointer<PsiElement>> primary) {
            return ContainerUtil.map2Array(primary, PsiElement.class, SMARTPOINTER_TO_ELEMENT_MAPPER);
        }

        @Nonnull
        private static List<SmartPsiElementPointer<PsiElement>> convertToSmartPointers(@Nonnull PsiElement[] primaryElements) {
            if (primaryElements.length == 0) {
                return Collections.emptyList();
            }

            SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(primaryElements[0].getProject());
            return ContainerUtil.mapNotNull(primaryElements, smartPointerManager::createSmartPsiElementPointer);
        }

        /**
         * A read-only attribute describing the target as a "primary" target.
         * A primary target is a target that was the main purpose of the search.
         * All usages of a non-primary target should be considered as a special case of usages of the corresponding primary target.
         * Example: searching field and its getter and setter methods -
         * the field searched is a primary target, and its accessor methods are non-primary targets, because
         * for this particular search usages of getter/setter methods are to be considered as a usages of the corresponding field.
         */
        @Nonnull
        public PsiElement[] getPrimaryElements() {
            return convertToPsiElements(myPrimarySearchedElements);
        }

        @Nonnull
        public PsiElement[] getAdditionalElements() {
            return convertToPsiElements(myAdditionalSearchedElements);
        }

        @Nonnull
        @RequiredReadAction
        public List<PsiElement> getAllElements() {
            List<PsiElement> result = new ArrayList<>(myPrimarySearchedElements.size() + myAdditionalSearchedElements.size());
            for (SmartPsiElementPointer pointer : myPrimarySearchedElements) {
                PsiElement element = pointer.getElement();
                if (element != null) {
                    result.add(element);
                }
            }
            for (SmartPsiElementPointer pointer : myAdditionalSearchedElements) {
                PsiElement element = pointer.getElement();
                if (element != null) {
                    result.add(element);
                }
            }
            return result;
        }

        @Nonnull
        public List<SmartPsiElementPointer<PsiElement>> getAllElementPointers() {
            List<SmartPsiElementPointer<PsiElement>> result =
                new ArrayList<>(myPrimarySearchedElements.size() + myAdditionalSearchedElements.size());
            result.addAll(myPrimarySearchedElements);
            result.addAll(myAdditionalSearchedElements);
            return result;
        }
    }

    @Nonnull
    @RequiredReadAction
    public static Usage convert(@Nonnull TargetElementsDescriptor descriptor, @Nonnull UsageInfo usageInfo) {
        PsiElement[] primaryElements = descriptor.getPrimaryElements();

        return convert(primaryElements, usageInfo);
    }

    @Nonnull
    @RequiredReadAction
    public static Usage convert(@Nonnull PsiElement[] primaryElements, @Nonnull UsageInfo usageInfo) {
        PsiElement usageElement = usageInfo.getElement();
        for (ReadWriteAccessDetector detector : ReadWriteAccessDetector.EP_NAME.getExtensionList()) {
            if (isReadWriteAccessibleElements(primaryElements, detector)) {
                ReadWriteAccessDetector.Access rwAccess = detector.getExpressionAccess(usageElement);
                return new ReadWriteAccessUsageInfo2UsageAdapter(
                    usageInfo,
                    rwAccess != ReadWriteAccessDetector.Access.Write,
                    rwAccess != ReadWriteAccessDetector.Access.Read
                );
            }
        }
        return new UsageInfo2UsageAdapter(usageInfo);
    }

    @Nonnull
    @RequiredReadAction
    public static Usage[] convert(@Nonnull TargetElementsDescriptor descriptor, @Nonnull UsageInfo[] usageInfos) {
        Usage[] usages = new Usage[usageInfos.length];
        for (int i = 0; i < usages.length; i++) {
            usages[i] = convert(descriptor, usageInfos[i]);
        }
        return usages;
    }

    @Nonnull
    @RequiredReadAction
    public static Usage[] convert(@Nonnull PsiElement[] primaryElements, @Nonnull UsageInfo[] usageInfos) {
        return ContainerUtil.map(usageInfos, info -> convert(primaryElements, info), new Usage[usageInfos.length]);
    }

    private static boolean isReadWriteAccessibleElements(@Nonnull PsiElement[] elements, @Nonnull ReadWriteAccessDetector detector) {
        if (elements.length == 0) {
            return false;
        }
        for (PsiElement element : elements) {
            if (!detector.isReadWriteAccessible(element)) {
                return false;
            }
        }
        return true;
    }
}
