/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.util;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.ElementDescriptionUtil;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author dsl
 */
public class DeleteUtil {
    private DeleteUtil() {
    }

    @Deprecated
    public static String generateWarningMessage(String messageTemplate, PsiElement[] elements) {
        if (elements.length == 1) {
            String name = ElementDescriptionUtil.getElementDescription(elements[0], DeleteNameDescriptionLocation.INSTANCE);
            String type = ElementDescriptionUtil.getElementDescription(elements[0], DeleteTypeDescriptionLocation.SINGULAR);
            return MessageFormat.format(messageTemplate, type + " \"" + name + "\"");
        }

        Map<String, Integer> countMap = new HashMap<>();

        Map<String, String> pluralToSingular = new HashMap<>();
        int directoryCount = 0;
        String containerType = null;

        for (PsiElement elementToDelete : elements) {
            String type = ElementDescriptionUtil.getElementDescription(elementToDelete, DeleteTypeDescriptionLocation.PLURAL);
            pluralToSingular.put(
                type,
                ElementDescriptionUtil.getElementDescription(elementToDelete, DeleteTypeDescriptionLocation.SINGULAR)
            );
            int oldCount = countMap.getOrDefault(type, 0);
            countMap.put(type, oldCount + 1);
            if (elementToDelete instanceof PsiDirectoryContainer directoryContainer) {
                containerType = type;
                directoryCount += directoryContainer.getDirectories().length;
            }
        }

        StringBuilder buffer = new StringBuilder();
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            if (buffer.length() > 0) {
                if (buffer.length() > 0) {
                    buffer.append(" ").append(RefactoringLocalize.promptDeleteAnd().get()).append(" ");
                }
            }
            int count = entry.getValue();

            buffer.append(count).append(" ");
            if (count == 1) {
                buffer.append(pluralToSingular.get(entry.getKey()));
            }
            else {
                buffer.append(entry.getKey());
            }

            if (entry.getKey().equals(containerType)) {
                buffer.append(" ").append(RefactoringLocalize.promptDeleteDirectoryParen(directoryCount).get());
            }
        }
        return MessageFormat.format(messageTemplate, buffer.toString());
    }

    @Nonnull
    public static LocalizeValue generateWarningMessage(Function<Object, LocalizeValue> messageTemplate, PsiElement[] elements) {
        if (elements.length == 1) {
            String name = ElementDescriptionUtil.getElementDescription(elements[0], DeleteNameDescriptionLocation.INSTANCE);
            String type = ElementDescriptionUtil.getElementDescription(elements[0], DeleteTypeDescriptionLocation.SINGULAR);
            return messageTemplate.apply(type + " \"" + name + "\"");
        }

        Map<String, Integer> countMap = new HashMap<>();

        Map<String, String> pluralToSingular = new HashMap<>();
        int directoryCount = 0;
        String containerType = null;

        for (PsiElement elementToDelete : elements) {
            String type = ElementDescriptionUtil.getElementDescription(elementToDelete, DeleteTypeDescriptionLocation.PLURAL);
            pluralToSingular.put(
                type,
                ElementDescriptionUtil.getElementDescription(elementToDelete, DeleteTypeDescriptionLocation.SINGULAR)
            );
            int oldCount = countMap.getOrDefault(type, 0);
            countMap.put(type, oldCount + 1);
            if (elementToDelete instanceof PsiDirectoryContainer directoryContainer) {
                containerType = type;
                directoryCount += directoryContainer.getDirectories().length;
            }
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            if (builder.length() > 0) {
                if (builder.length() > 0) {
                    builder.append(" ").append(RefactoringLocalize.promptDeleteAnd().get()).append(" ");
                }
            }
            int count = entry.getValue();

            builder.append(count).append(" ");
            if (count == 1) {
                builder.append(pluralToSingular.get(entry.getKey()));
            }
            else {
                builder.append(entry.getKey());
            }

            if (entry.getKey().equals(containerType)) {
                builder.append(" ").append(RefactoringLocalize.promptDeleteDirectoryParen(directoryCount).get());
            }
        }
        return messageTemplate.apply(builder.toString());
    }
}
