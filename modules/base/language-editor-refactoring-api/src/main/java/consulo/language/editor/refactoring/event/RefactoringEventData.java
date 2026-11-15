/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.event;

import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageInfo;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RefactoringEventData extends UserDataHolderBase {
    public static final Key<Collection<? extends LocalizeValue>> CONFLICTS_KEY = Key.create("conflicts");
    public static final Key<PsiElement> PSI_ELEMENT_KEY = Key.create("element");
    public static final Key<String[]> STRING_PROPERTIES = Key.create("stringProperties");
    public static final Key<PsiElement[]> PSI_ELEMENT_ARRAY_KEY = Key.create("elementArray");
    public static final Key<Collection<UsageInfo>> USAGE_INFOS_KEY = Key.create("usageInfos");
    /**
     * The target of the refactoring, e.g., the new parent directory of moved files and directories.
     */
    public static final Key<PsiElement> TARGET_ELEMENT_KEY = Key.create("targetElement");
    /**
     * All elements of a rename mapped to their new names, the primary element first, then the automatic renames.
     */
    public static final Key<Map<PsiElement, String>> RENAMES_KEY = Key.create("renames");

    public void addElement(PsiElement element) {
        putUserData(PSI_ELEMENT_KEY, element);
    }

    public <T> void addMembers(T[] members, Function<T, PsiElement> converter) {
        List<PsiElement> elements = new ArrayList<>();
        for (T info : members) {
            elements.add(converter.apply(info));
        }
        addElements(elements);
    }

    public void addElements(Collection<PsiElement> elements) {
        putUserData(PSI_ELEMENT_ARRAY_KEY, elements.toArray(new PsiElement[elements.size()]));
    }

    public void addElements(PsiElement[] elements) {
        putUserData(PSI_ELEMENT_ARRAY_KEY, elements);
    }

    public void addUsages(Collection<UsageInfo> usageInfos) {
        putUserData(USAGE_INFOS_KEY, usageInfos);
    }

    public void addStringProperties(String... properties) {
        putUserData(STRING_PROPERTIES, properties);
    }

    public void addTargetElement(PsiElement target) {
        putUserData(TARGET_ELEMENT_KEY, target);
    }

    /**
     * Stores an unmodifiable copy of {@code renames}, in the order of the given map.
     */
    public void addRenames(Map<? extends PsiElement, String> renames) {
        putUserData(RENAMES_KEY, Collections.unmodifiableMap(new LinkedHashMap<>(renames)));
    }
}
