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

package consulo.language.editor.testIntegration;

import consulo.application.util.matcher.NameUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TestFinderHelper {
    public static PsiElement findSourceElement(PsiElement from) {
        for (TestFinder each : getFinders()) {
            PsiElement result = each.findSourceElement(from);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public static List<PsiElement> findTestsForClass(PsiElement element) {
        List<PsiElement> result = new ArrayList<>();
        for (TestFinder each : getFinders()) {
            result.addAll(each.findTestsForClass(element));
        }
        return result;
    }

    public static List<PsiElement> findClassesForTest(PsiElement element) {
        List<PsiElement> result = new ArrayList<>();
        for (TestFinder each : getFinders()) {
            result.addAll(each.findClassesForTest(element));
        }
        return result;
    }

    public static boolean isTest(PsiElement element) {
        if (element == null) {
            return false;
        }
        for (TestFinder each : getFinders()) {
            if (each.isTest(element)) {
                return true;
            }
        }
        return false;
    }

    public static List<TestFinder> getFinders() {
        return TestFinder.EP_NAME.getExtensionList();
    }

    public static Integer calcTestNameProximity(final String className, final String testName) {
        int posProximity = testName.indexOf(className);
        int sizeProximity = testName.length() - className.length();

        return posProximity + sizeProximity;
    }

    public static List<PsiElement> getSortedElements(
        final List<Pair<? extends PsiNamedElement, Integer>> elementsWithWeights,
        final boolean weightsAscending
    ) {
        return getSortedElements(elementsWithWeights, weightsAscending, null);
    }

    public static List<PsiElement> getSortedElements(
        final List<Pair<? extends PsiNamedElement, Integer>> elementsWithWeights,
        final boolean weightsAscending,
        @Nullable final Comparator<PsiElement> sameNameComparator
    ) {
        Collections.sort(elementsWithWeights, new Comparator<>() {
            @Override
            public int compare(Pair<? extends PsiNamedElement, Integer> o1, Pair<? extends PsiNamedElement, Integer> o2) {
                int result = weightsAscending ? o1.second.compareTo(o2.second) : o2.second.compareTo(o1.second);
                if (result == 0) {
                    result = Comparing.compare(o1.first.getName(), o2.first.getName());
                }
                if (result == 0 && sameNameComparator != null) {
                    result = sameNameComparator.compare(o1.first, o2.first);
                }

                return result;
            }
        });

        final List<PsiElement> result = new ArrayList<>(elementsWithWeights.size());
        for (Pair<? extends PsiNamedElement, Integer> each : elementsWithWeights) {
            result.add(each.first);
        }

        return result;
    }

    public static List<Pair<String, Integer>> collectPossibleClassNamesWithWeights(String testName) {
        String[] words = NameUtil.splitNameIntoWords(testName);
        List<Pair<String, Integer>> result = new ArrayList<>();

        for (int from = 0; from < words.length; from++) {
            for (int to = from; to < words.length; to++) {
                result.add(new Pair<>(StringUtil.join(words, from, to + 1, ""), words.length - from + to));
            }
        }

        return result;
    }
}
