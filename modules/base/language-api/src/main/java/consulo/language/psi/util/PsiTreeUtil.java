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
package consulo.language.psi.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.stub.StubElement;
import consulo.language.version.LanguageVersion;
import consulo.language.version.LanguageVersionResolver;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class PsiTreeUtil {
    private static final Logger LOG = Logger.getInstance(PsiTreeUtil.class);

    private static final Key<Integer> INDEX = Key.create("PsiTreeUtil.copyElements.INDEX");
    private static final Key<Object> MARKER = Key.create("PsiTreeUtil.copyElements.MARKER");

    @SuppressWarnings("unchecked")
    private static final Class<? extends PsiElement>[] WS = new Class[]{PsiWhiteSpace.class};
    @SuppressWarnings("unchecked")
    private static final Class<? extends PsiElement>[] WS_COMMENTS = new Class[]{PsiWhiteSpace.class, PsiComment.class};

    /**
     * Checks whether one element in the psi tree is under another.
     *
     * @param ancestor parent candidate. <code>false</code> will be returned if ancestor is null.
     * @param element  child candidate
     * @param strict   whether return true if ancestor and parent are PsiUtilCorethe same.
     * @return true if element has ancestor as its parent somewhere in the hierarchy and false otherwise.
     */
    @Contract("null, _, _ -> false")
    public static boolean isAncestor(@Nullable PsiElement ancestor, @Nonnull PsiElement element, boolean strict) {
        if (ancestor == null) {
            return false;
        }
        // fast path to avoid loading tree
        if (ancestor instanceof StubBasedPsiElement ancestorStub && ancestorStub.getStub() != null
            || element instanceof StubBasedPsiElement elementStub && elementStub.getStub() != null) {
            if (ancestor.getContainingFile() != element.getContainingFile()) {
                return false;
            }
        }

        boolean stopAtFileLevel = !(ancestor instanceof PsiFile || ancestor instanceof PsiDirectory);

        PsiElement parent = strict ? element.getParent() : element;
        while (true) {
            if (parent == null) {
                return false;
            }
            if (parent.equals(ancestor)) {
                return true;
            }
            if (stopAtFileLevel && parent instanceof PsiFile) {
                return false;
            }
            parent = parent.getParent();
        }
    }

    /**
     * Checks whether one element in the psi tree is under another in {@link PsiElement#getContext()}  hierarchy.
     *
     * @param ancestor parent candidate. <code>false</code> will be returned if ancestor is null.
     * @param element  child candidate
     * @param strict   whether return true if ancestor and parent are the same.
     * @return true if element has ancestor as its parent somewhere in the hierarchy and false otherwise.
     */
    @Contract("null, _, _ -> false")
    public static boolean isContextAncestor(@Nullable PsiElement ancestor, @Nonnull PsiElement element, boolean strict) {
        if (ancestor == null) {
            return false;
        }
        boolean stopAtFileLevel = !(ancestor instanceof PsiFile || ancestor instanceof PsiDirectory);
        PsiElement parent = strict ? element.getContext() : element;
        while (true) {
            if (parent == null) {
                return false;
            }
            if (parent.equals(ancestor)) {
                return true;
            }
            if (stopAtFileLevel && parent instanceof PsiFile) {
                final PsiElement context = parent.getContext();
                if (context == null) {
                    return false;
                }
            }
            parent = parent.getContext();
        }
    }

    @Nullable
    public static PsiElement findCommonParent(@Nonnull List<? extends PsiElement> elements) {
        if (elements.isEmpty()) {
            return null;
        }
        PsiElement toReturn = null;
        for (PsiElement element : elements) {
            if (element == null) {
                continue;
            }
            toReturn = toReturn == null ? element : findCommonParent(toReturn, element);
            if (toReturn == null) {
                return null;
            }
        }

        return toReturn;
    }

    @Nullable
    public static PsiElement findCommonParent(@Nonnull PsiElement... elements) {
        if (elements.length == 0) {
            return null;
        }
        PsiElement toReturn = null;
        for (PsiElement element : elements) {
            if (element == null) {
                continue;
            }
            toReturn = toReturn == null ? element : findCommonParent(toReturn, element);
            if (toReturn == null) {
                return null;
            }
        }

        return toReturn;
    }

    @Nullable
    public static PsiElement findCommonParent(@Nonnull PsiElement element1, @Nonnull PsiElement element2) {
        // optimization
        if (element1 == element2) {
            return element1;
        }
        final PsiFile containingFile = element1.getContainingFile();
        final PsiElement topLevel = containingFile == element2.getContainingFile() ? containingFile : null;

        ArrayList<PsiElement> parents1 = getParents(element1, topLevel);
        ArrayList<PsiElement> parents2 = getParents(element2, topLevel);
        int size = Math.min(parents1.size(), parents2.size());
        PsiElement parent = topLevel;
        for (int i = 1; i <= size; i++) {
            PsiElement parent1 = parents1.get(parents1.size() - i);
            PsiElement parent2 = parents2.get(parents2.size() - i);
            if (!parent1.equals(parent2)) {
                break;
            }
            parent = parent1;
        }
        return parent;
    }

    @Nonnull
    private static ArrayList<PsiElement> getParents(@Nonnull PsiElement element, @Nullable PsiElement topLevel) {
        ArrayList<PsiElement> parents = new ArrayList<>();
        PsiElement parent = element;
        while (parent != topLevel && parent != null) {
            parents.add(parent);
            parent = parent.getParent();
        }
        return parents;
    }

    @Nullable
    public static PsiElement findCommonContext(@Nonnull PsiElement... elements) {
        return findCommonContext(Arrays.asList(elements));
    }

    @Nullable
    public static PsiElement findCommonContext(@Nonnull Collection<? extends PsiElement> elements) {
        if (elements.isEmpty()) {
            return null;
        }
        PsiElement toReturn = null;
        for (PsiElement element : elements) {
            if (element == null) {
                continue;
            }
            toReturn = toReturn == null ? element : findCommonContext(toReturn, element);
            if (toReturn == null) {
                return null;
            }
        }
        return toReturn;
    }

    @Nullable
    public static PsiElement findCommonContext(@Nonnull PsiElement element1, @Nonnull PsiElement element2) {
        // optimization
        if (element1 == element2) {
            return element1;
        }
        final PsiFile containingFile = element1.getContainingFile();
        final PsiElement topLevel = containingFile == element2.getContainingFile() ? containingFile : null;

        ArrayList<PsiElement> parents1 = getContexts(element1, topLevel);
        ArrayList<PsiElement> parents2 = getContexts(element2, topLevel);
        int size = Math.min(parents1.size(), parents2.size());
        PsiElement parent = topLevel;
        for (int i = 1; i <= size; i++) {
            PsiElement parent1 = parents1.get(parents1.size() - i);
            PsiElement parent2 = parents2.get(parents2.size() - i);
            if (!parent1.equals(parent2)) {
                break;
            }
            parent = parent1;
        }
        return parent;
    }

    @Nonnull
    private static ArrayList<PsiElement> getContexts(@Nonnull PsiElement element, @Nullable PsiElement topLevel) {
        ArrayList<PsiElement> parents = new ArrayList<>();
        PsiElement parent = element;
        while (parent != topLevel && parent != null) {
            parents.add(parent);
            parent = parent.getContext();
        }
        return parents;
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T findChildOfType(@Nullable final PsiElement element, @Nonnull final Class<T> aClass) {
        return findChildOfAnyType(element, true, aClass);
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T findChildOfType(
        @Nullable final PsiElement element,
        @Nonnull final Class<T> aClass,
        final boolean strict
    ) {
        return findChildOfAnyType(element, strict, aClass);
    }

    /**
     * Recursive (depth first) strict({@code element} isn't included) search for first element of any of given {@code classes}.
     *
     * @param element a PSI element to start search from.
     * @param classes element types to search for.
     * @param <T>     type to cast found element to.
     * @return first found element, or null if nothing found.
     */
    @Nullable
    @Contract("null, _ -> null")
    @RequiredReadAction
    @SafeVarargs
    public static <T extends PsiElement> T findChildOfAnyType(
        @Nullable final PsiElement element,
        @Nonnull final Class<? extends T>... classes
    ) {
        return findChildOfAnyType(element, true, classes);
    }

    /**
     * Recursive (depth first) search for first element of any of given {@code classes}.
     *
     * @param element a PSI element to start search from.
     * @param strict  if false the {@code element} is also included in the search.
     * @param classes element types to search for.
     * @param <T>     type to cast found element to.
     * @return first found element, or null if nothing found.
     */
    @Nullable
    @Contract("null, _, _ -> null")
    @RequiredReadAction
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T findChildOfAnyType(
        @Nullable final PsiElement element,
        final boolean strict,
        @Nonnull final Class<? extends T>... classes
    ) {
        PsiElementProcessor.FindElement<PsiElement> processor = new PsiElementProcessor.FindElement<>() {
            @Override
            public boolean execute(@Nonnull PsiElement each) {
                return strict && each == element || !instanceOf(each, classes) || setFound(each);
            }
        };

        processElements(element, processor);
        return (T)processor.getFoundElement();
    }

    @Nonnull
    @RequiredReadAction
    public static <T extends PsiElement> Collection<T> findChildrenOfType(
        @Nullable PsiElement element,
        @Nonnull Class<? extends T> aClass
    ) {
        return findChildrenOfAnyType(element, aClass);
    }

    @Nonnull
    @SafeVarargs
    @RequiredReadAction
    public static <T extends PsiElement> Collection<T> findChildrenOfAnyType(
        @Nullable final PsiElement element,
        @Nonnull final Class<? extends T>... classes
    ) {
        if (element == null) {
            return List.of();
        }

        PsiElementProcessor.CollectElements<T> processor = new PsiElementProcessor.CollectElements<>() {
            @Override
            public boolean execute(@Nonnull T each) {
                return each == element || !instanceOf(each, classes) || super.execute(each);
            }
        };
        processElements(element, processor);
        return processor.getCollection();
    }

    /**
     * Non-recursive search for element of type T amongst given {@code element} children.
     *
     * @param element a PSI element to start search from.
     * @param aClass  element type to search for.
     * @param <T>     element type to search for.
     * @return first found element, or null if nothing found.
     */
    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getChildOfType(@Nullable PsiElement element, @Nonnull Class<T> aClass) {
        if (element == null) {
            return null;
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (aClass.isInstance(child)) {
                return (T)child;
            }
        }
        return null;
    }

    @Nullable
    public static PsiElement findFirstParent(@Nullable PsiElement element, Predicate<PsiElement> condition) {
        return findFirstParent(element, false, condition);
    }

    @Nullable
    public static PsiElement findFirstParent(@Nullable PsiElement element, boolean strict, Predicate<PsiElement> condition) {
        if (strict && element != null) {
            element = element.getParent();
        }

        while (element != null) {
            if (condition.test(element)) {
                return element;
            }
            element = element.getParent();
        }
        return null;
    }

    @Nonnull
    @RequiredReadAction
    public static <T extends PsiElement> T getRequiredChildOfType(@Nonnull PsiElement element, @Nonnull Class<T> aClass) {
        final T child = getChildOfType(element, aClass);
        assert child != null : "Missing required child of type " + aClass.getName();
        return child;
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T[] getChildrenOfType(@Nullable PsiElement element, @Nonnull Class<T> aClass) {
        if (element == null) {
            return null;
        }

        List<T> result = null;
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (aClass.isInstance(child)) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add((T)child);
            }
        }
        return result == null ? null : ArrayUtil.toObjectArray(result, aClass);
    }

    @Nonnull
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> List<T> getChildrenOfTypeAsList(@Nullable PsiElement element, @Nonnull Class<T> aClass) {
        if (element == null) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (aClass.isInstance(child)) {
                result.add((T)child);
            }
        }
        return result;
    }

    @Nullable
    @RequiredReadAction
    public static <T extends PsiElement> T getStubChildOfType(@Nullable PsiElement element, @Nonnull Class<T> aClass) {
        if (element == null) {
            return null;
        }
        StubElement<?> stub = element instanceof StubBasedPsiElement ? ((StubBasedPsiElement<?>)element).getStub() : null;
        if (stub == null) {
            return getChildOfType(element, aClass);
        }
        for (StubElement<?> childStub : stub.getChildrenStubs()) {
            PsiElement child = childStub.getPsi();
            if (aClass.isInstance(child)) {
                return aClass.cast(child);
            }
        }
        return null;
    }

    @Nonnull
    @RequiredReadAction
    public static <T extends PsiElement> List<T> getStubChildrenOfTypeAsList(@Nullable PsiElement element, @Nonnull Class<T> aClass) {
        if (element == null) {
            return Collections.emptyList();
        }
        StubElement<?> stub = element instanceof StubBasedPsiElement elementStub ? elementStub.getStub() : null;
        if (stub == null) {
            return getChildrenOfTypeAsList(element, aClass);
        }

        List<T> result = new ArrayList<>();
        for (StubElement childStub : stub.getChildrenStubs()) {
            PsiElement child = childStub.getPsi();
            if (aClass.isInstance(child)) {
                result.add(aClass.cast(child));
            }
        }
        return result;
    }

    public static boolean instanceOf(final Object object, final Class<?>... classes) {
        if (classes != null) {
            for (final Class<?> c : classes) {
                if (c.isInstance(object)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a direct child of the specified element which has any of the specified classes.
     *
     * @param element the element to get the child for.
     * @param classes the array of classes.
     * @return the element, or null if none was found.
     * @since 5.1
     */
    @Nullable
    @Contract("null, _ -> null")
    @RequiredReadAction
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getChildOfAnyType(@Nullable PsiElement element, @Nonnull Class<? extends T>... classes) {
        if (element == null) {
            return null;
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            for (Class<? extends T> aClass : classes) {
                if (aClass.isInstance(child)) {
                    return (T)child;
                }
            }
        }
        return null;
    }

    @Nullable
    @Contract("null, _ -> null")
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getNextSiblingOfType(@Nullable PsiElement sibling, @Nonnull Class<T> aClass) {
        if (sibling == null) {
            return null;
        }
        for (PsiElement child = sibling.getNextSibling(); child != null; child = child.getNextSibling()) {
            if (aClass.isInstance(child)) {
                return (T)child;
            }
        }
        return null;
    }

    @Nullable
    @Contract("null, _ -> null")
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getPrevSiblingOfType(@Nullable PsiElement sibling, @Nonnull Class<T> aClass) {
        if (sibling == null) {
            return null;
        }
        for (PsiElement child = sibling.getPrevSibling(); child != null; child = child.getPrevSibling()) {
            if (aClass.isInstance(child)) {
                return (T)child;
            }
        }
        return null;
    }

    @Nullable
    @Contract("null, _ -> null")
    public static <T extends PsiElement> T getTopmostParentOfType(@Nullable PsiElement element, @Nonnull Class<T> aClass) {
        T answer = getParentOfType(element, aClass);

        do {
            T next = getParentOfType(answer, aClass);
            if (next == null) {
                break;
            }
            answer = next;
        }
        while (true);

        return answer;
    }

    @Nullable
    @Contract("null, _ -> null")
    public static <T extends PsiElement> T getParentOfType(@Nullable PsiElement element, @Nonnull Class<T> aClass) {
        return getParentOfType(element, aClass, true);
    }

    @Nullable
    @Contract("null -> null")
    @SuppressWarnings("unchecked")
    public static PsiElement getStubOrPsiParent(@Nullable PsiElement element) {
        if (element instanceof StubBasedPsiElement elementStub) {
            StubElement stub = elementStub.getStub();
            if (stub != null) {
                final StubElement parentStub = stub.getParentStub();
                return parentStub != null ? parentStub.getPsi() : null;
            }
        }
        return element != null ? element.getParent() : null;
    }

    @Nullable
    @Contract("null, _ -> null")
    @SuppressWarnings("unchecked")
    public static <E extends PsiElement> E getStubOrPsiParentOfType(@Nullable PsiElement element, @Nonnull Class<E> parentClass) {
        if (element instanceof StubBasedPsiElement elementStub) {
            StubElement stub = elementStub.getStub();
            if (stub != null) {
                return (E)stub.getParentStubOfType(parentClass);
            }
        }
        return getParentOfType(element, parentClass);
    }

    @Nullable
    @Contract("null, _, _, _ -> null")
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getContextOfType(
        @Nullable PsiElement element,
        @Nonnull Class<T> aClass,
        boolean strict,
        Class<? extends PsiElement>... stopAt
    ) {
        if (element == null) {
            return null;
        }
        if (strict) {
            element = element.getContext();
        }

        while (element != null && !aClass.isInstance(element)) {
            if (instanceOf(element, stopAt)) {
                return null;
            }
            element = element.getContext();
        }

        return (T)element;
    }

    @Nullable
    @Contract("null, _, _ -> null")
    public static <T extends PsiElement> T getContextOfType(
        @Nullable PsiElement element,
        @Nonnull Class<? extends T> aClass,
        boolean strict
    ) {
        return getContextOfType(element, strict, aClass);
    }

    @Nullable
    @SafeVarargs
    public static <T extends PsiElement> T getContextOfType(@Nullable PsiElement element, @Nonnull Class<? extends T>... classes) {
        return getContextOfType(element, true, classes);
    }

    @Nullable
    @Contract("null, _, _ -> null")
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getContextOfType(
        @Nullable PsiElement element,
        boolean strict,
        @Nonnull Class<? extends T>... classes
    ) {
        if (element == null) {
            return null;
        }
        if (strict) {
            element = element.getContext();
        }

        while (element != null && !instanceOf(element, classes)) {
            element = element.getContext();
        }

        return (T)element;
    }

    @Nullable
    @Contract("null, _, _ -> null")
    public static <T extends PsiElement> T getParentOfType(@Nullable PsiElement element, @Nonnull Class<T> aClass, boolean strict) {
        return getParentOfType(element, aClass, strict, -1);
    }

    @Contract("null, _, _, _ -> null")
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getParentOfType(
        @Nullable PsiElement element,
        @Nonnull Class<T> aClass,
        boolean strict,
        int minStartOffset
    ) {
        if (element == null) {
            return null;
        }

        if (strict) {
            if (element instanceof PsiFile) {
                return null;
            }
            element = element.getParent();
        }

        while (element != null && (minStartOffset == -1 || element.getNode().getStartOffset() >= minStartOffset)) {
            if (aClass.isInstance(element)) {
                return (T)element;
            }
            if (element instanceof PsiFile) {
                return null;
            }
            element = element.getParent();
        }

        return null;
    }

    @Nullable
    @Contract("null, _, _, _ -> null")
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getParentOfType(
        @Nullable PsiElement element,
        @Nonnull Class<T> aClass,
        boolean strict,
        @Nonnull Class<? extends PsiElement>... stopAt
    ) {
        if (element == null) {
            return null;
        }
        if (strict) {
            if (element instanceof PsiFile) {
                return null;
            }
            element = element.getParent();
        }

        while (element != null && !aClass.isInstance(element)) {
            if (instanceOf(element, stopAt) || element instanceof PsiFile) {
                return null;
            }
            element = element.getParent();
        }

        return (T)element;
    }

    @Nonnull
    public static <T extends PsiElement> List<T> collectParents(
        @Nonnull PsiElement element,
        @Nonnull Class<? extends T> parent,
        boolean includeMyself,
        @Nonnull Predicate<? super PsiElement> stopCondition
    ) {
        if (!includeMyself) {
            element = element.getParent();
        }
        List<T> parents = new ArrayList<>();
        while (element != null) {
            if (stopCondition.test(element)) {
                break;
            }
            if (parent.isInstance(element)) {
                parents.add(parent.cast(element));
            }
            element = element.getParent();
        }
        return parents;
    }

    @Nullable
    @Contract("null, _ -> null")
    public static PsiElement skipParentsOfType(@Nullable PsiElement element, @Nonnull Class... parentClasses) {
        if (element == null) {
            return null;
        }
        for (PsiElement e = element.getParent(); e != null; e = e.getParent()) {
            if (!instanceOf(e, parentClasses)) {
                return e;
            }
        }
        return null;
    }

    @Nullable
    @Contract("null, _ -> null")
    @SafeVarargs
    public static <T extends PsiElement> T getParentOfType(
        @Nullable final PsiElement element,
        @Nonnull final Class<? extends T>... classes
    ) {
        if (element == null || element instanceof PsiFile) {
            return null;
        }
        PsiElement parent = element.getParent();
        if (parent == null) {
            return null;
        }
        return getNonStrictParentOfType(parent, classes);
    }

    @Nullable
    @Contract("null, _ -> null")
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getNonStrictParentOfType(
        @Nullable final PsiElement element,
        @Nonnull final Class<? extends T>... classes
    ) {
        PsiElement run = element;
        while (run != null) {
            if (instanceOf(run, classes)) {
                return (T)run;
            }
            if (run instanceof PsiFile) {
                break;
            }
            run = run.getParent();
        }

        return null;
    }

    @Nonnull
    @RequiredReadAction
    public static PsiElement[] collectElements(@Nullable PsiElement element, @Nonnull PsiElementFilter filter) {
        PsiElementProcessor.CollectFilteredElements<PsiElement> processor =
            new PsiElementProcessor.CollectFilteredElements<>(filter);
        processElements(element, processor);
        return processor.toArray();
    }

    @Nonnull
    @RequiredReadAction
    @SafeVarargs
    public static <T extends PsiElement> Collection<T> collectElementsOfType(
        @Nullable final PsiElement element,
        @Nonnull final Class<T>... classes
    ) {
        PsiElementProcessor.CollectFilteredElements<T> processor =
            new PsiElementProcessor.CollectFilteredElements<>((PsiElementFilter)psiElement -> {
                for (Class<T> clazz : classes) {
                    if (clazz.isInstance(psiElement)) {
                        return true;
                    }
                }

                return false;
            });
        processElements(element, processor);
        return processor.getCollection();
    }

    @Contract("null, _ -> true")
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static boolean processElements(@Nullable PsiElement element, @Nonnull final PsiElementProcessor processor) {
        if (element == null) {
            return true;
        }
        if (element instanceof PsiCompiledElement || !element.isPhysical()) {
            // DummyHolders cannot be visited by walking visitors because children/parent relationship is broken there
            if (!processor.execute(element)) {
                return false;
            }
            for (PsiElement child : element.getChildren()) {
                if (!processElements(child, processor)) {
                    return false;
                }
            }
            return true;
        }
        final boolean[] result = {true};
        element.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (processor.execute(element)) {
                    super.visitElement(element);
                }
                else {
                    stopWalking();
                    result[0] = false;
                }
            }
        });

        return result[0];
    }

    @RequiredReadAction
    public static boolean processElements(@Nonnull PsiElementProcessor processor, @Nullable PsiElement... elements) {
        if (elements == null || elements.length == 0) {
            return true;
        }
        for (PsiElement element : elements) {
            if (!processElements(element, processor)) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @RequiredReadAction
    public static PsiElement[] copyElements(@Nonnull PsiElement[] elements) {
        ArrayList<PsiElement> roots = new ArrayList<>();
        for (int i = 0; i < elements.length; i++) {
            PsiElement rootCandidate = elements[i];
            boolean failed = false;
            for (int j = 0; j < elements.length; j++) {
                PsiElement element = elements[j];
                if (i != j && isAncestor(element, rootCandidate, true)) {
                    failed = true;
                    break;
                }
            }
            if (!failed) {
                roots.add(rootCandidate);
            }
        }
        for (int i = 0; i < elements.length; i++) {
            PsiElement element = elements[i];
            element.putCopyableUserData(INDEX, i);
        }
        PsiElement[] newRoots = new PsiElement[roots.size()];
        for (int i = 0; i < roots.size(); i++) {
            PsiElement root = roots.get(i);
            newRoots[i] = root.copy();
        }

        final PsiElement[] result = new PsiElement[elements.length];
        for (PsiElement newRoot : newRoots) {
            decodeIndices(newRoot, result);
        }
        return result;
    }

    @RequiredReadAction
    private static void decodeIndices(@Nonnull PsiElement element, @Nonnull PsiElement[] result) {
        final Integer data = element.getCopyableUserData(INDEX);
        if (data != null) {
            element.putCopyableUserData(INDEX, null);
            int index = data;
            result[index] = element;
        }
        PsiElement child = element.getFirstChild();
        while (child != null) {
            decodeIndices(child, result);
            child = child.getNextSibling();
        }
    }

    public static void mark(@Nonnull PsiElement element, @Nonnull Object marker) {
        element.putCopyableUserData(MARKER, marker);
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement releaseMark(@Nonnull PsiElement root, @Nonnull Object marker) {
        if (marker.equals(root.getCopyableUserData(MARKER))) {
            root.putCopyableUserData(MARKER, null);
            return root;
        }
        else {
            PsiElement child = root.getFirstChild();
            while (child != null) {
                final PsiElement result = releaseMark(child, marker);
                if (result != null) {
                    return result;
                }
                child = child.getNextSibling();
            }
            return null;
        }
    }

    @Nullable
    @RequiredReadAction
    public static <T extends PsiElement> T findElementOfClassAtOffset(
        @Nonnull PsiFile file,
        int offset,
        @Nonnull Class<T> clazz,
        boolean strictStart
    ) {
        final List<PsiFile> psiRoots = file.getViewProvider().getAllFiles();
        T result = null;
        for (PsiElement root : psiRoots) {
            final PsiElement elementAt = root.findElementAt(offset);
            if (elementAt != null) {
                final T parent = getParentOfType(elementAt, clazz, strictStart);
                if (parent != null) {
                    final TextRange range = parent.getTextRange();
                    if (!strictStart || range.getStartOffset() == offset) {
                        if (result == null || result.getTextRange().getEndOffset() > range.getEndOffset()) {
                            result = parent;
                        }
                    }
                }
            }
        }

        return result;
    }

    @Nullable
    @RequiredReadAction
    @SafeVarargs
    public static <T extends PsiElement> T findElementOfClassAtOffsetWithStopSet(
        @Nonnull PsiFile file,
        int offset,
        @Nonnull Class<T> clazz,
        boolean strictStart,
        @Nonnull Class<? extends PsiElement>... stopAt
    ) {
        final List<PsiFile> psiRoots = file.getViewProvider().getAllFiles();
        T result = null;
        for (PsiElement root : psiRoots) {
            final PsiElement elementAt = root.findElementAt(offset);
            if (elementAt != null) {
                final T parent = getParentOfType(elementAt, clazz, strictStart, stopAt);
                if (parent != null) {
                    final TextRange range = parent.getTextRange();
                    if (!strictStart || range.getStartOffset() == offset) {
                        if (result == null || result.getTextRange().getEndOffset() > range.getEndOffset()) {
                            result = parent;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * @return maximal element of specified Class starting at startOffset exactly and ending not farther than endOffset
     */
    @Nullable
    @RequiredReadAction
    public static <T extends PsiElement> T findElementOfClassAtRange(
        @Nonnull PsiFile file,
        int startOffset,
        int endOffset,
        @Nonnull Class<T> clazz
    ) {
        final FileViewProvider viewProvider = file.getViewProvider();
        T result = null;
        for (Language lang : viewProvider.getLanguages()) {
            PsiElement elementAt = viewProvider.findElementAt(startOffset, lang);
            T run = getParentOfType(elementAt, clazz, false);
            T prev = run;
            while (run != null && run.getTextRange().getStartOffset() == startOffset && run.getTextRange().getEndOffset() <= endOffset) {
                prev = run;
                run = getParentOfType(run, clazz);
            }

            if (prev == null) {
                continue;
            }
            final int elementStartOffset = prev.getTextRange().getStartOffset();
            final int elementEndOffset = prev.getTextRange().getEndOffset();
            if (elementStartOffset != startOffset || elementEndOffset > endOffset) {
                continue;
            }

            if (result == null || result.getTextRange().getEndOffset() < elementEndOffset) {
                result = prev;
            }
        }

        return result;
    }

    @Nonnull
    @RequiredReadAction
    public static PsiElement getDeepestFirst(@Nonnull PsiElement elt) {
        @Nonnull PsiElement res = elt;
        do {
            final PsiElement firstChild = res.getFirstChild();
            if (firstChild == null) {
                return res;
            }
            res = firstChild;
        }
        while (true);
    }

    @Nonnull
    @RequiredReadAction
    public static PsiElement getDeepestLast(@Nonnull PsiElement elt) {
        @Nonnull PsiElement res = elt;
        do {
            final PsiElement lastChild = res.getLastChild();
            if (lastChild == null) {
                return res;
            }
            res = lastChild;
        }
        while (true);
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement prevLeaf(@Nonnull PsiElement current) {
        final PsiElement prevSibling = current.getPrevSibling();
        if (prevSibling != null) {
            return lastChild(prevSibling);
        }
        final PsiElement parent = current.getParent();
        if (parent == null || parent instanceof PsiFile) {
            return null;
        }
        return prevLeaf(parent);
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement nextLeaf(@Nonnull PsiElement current) {
        final PsiElement nextSibling = current.getNextSibling();
        if (nextSibling != null) {
            return firstChild(nextSibling);
        }
        final PsiElement parent = current.getParent();
        if (parent == null || parent instanceof PsiFile) {
            return null;
        }
        return nextLeaf(parent);
    }

    @RequiredReadAction
    public static PsiElement lastChild(@Nonnull PsiElement element) {
        PsiElement lastChild = element.getLastChild();
        if (lastChild != null) {
            return lastChild(lastChild);
        }
        return element;
    }

    @RequiredReadAction
    public static PsiElement firstChild(@Nonnull final PsiElement element) {
        PsiElement child = element.getFirstChild();
        if (child != null) {
            return firstChild(child);
        }
        return element;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement prevLeaf(@Nonnull final PsiElement element, final boolean skipEmptyElements) {
        PsiElement prevLeaf = prevLeaf(element);
        while (skipEmptyElements && prevLeaf != null && prevLeaf.getTextLength() == 0) prevLeaf = prevLeaf(prevLeaf);
        return prevLeaf;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement prevVisibleLeaf(@Nonnull final PsiElement element) {
        PsiElement prevLeaf = prevLeaf(element, true);
        while (prevLeaf != null && StringUtil.isEmptyOrSpaces(prevLeaf.getText())) prevLeaf = prevLeaf(prevLeaf, true);
        return prevLeaf;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement nextVisibleLeaf(@Nonnull final PsiElement element) {
        PsiElement nextLeaf = nextLeaf(element, true);
        while (nextLeaf != null && StringUtil.isEmptyOrSpaces(nextLeaf.getText())) nextLeaf = nextLeaf(nextLeaf, true);
        return nextLeaf;
    }

    @Nullable
    @RequiredReadAction
    public static PsiElement nextLeaf(final PsiElement element, final boolean skipEmptyElements) {
        PsiElement nextLeaf = nextLeaf(element);
        while (skipEmptyElements && nextLeaf != null && nextLeaf.getTextLength() == 0) nextLeaf = nextLeaf(nextLeaf);
        return nextLeaf;
    }

    @RequiredReadAction
    public static boolean hasErrorElements(@Nonnull final PsiElement element) {
        if (element instanceof PsiErrorElement) {
            return true;
        }

        for (PsiElement child : element.getChildren()) {
            if (hasErrorElements(child)) {
                return true;
            }
        }

        return false;
    }

    @Nonnull
    public static PsiElement[] filterAncestors(@Nonnull PsiElement[] elements) {
        if (LOG.isDebugEnabled()) {
            for (PsiElement element : elements) {
                LOG.debug("element = " + element);
            }
        }

        ArrayList<PsiElement> filteredElements = new ArrayList<>();
        ContainerUtil.addAll(filteredElements, elements);

        int previousSize;
        do {
            previousSize = filteredElements.size();
            outer:
            for (PsiElement element : filteredElements) {
                for (PsiElement element2 : filteredElements) {
                    if (element == element2) {
                        continue;
                    }
                    if (isAncestor(element, element2, false)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("removing " + element2);
                        }
                        filteredElements.remove(element2);
                        break outer;
                    }
                }
            }
        }
        while (filteredElements.size() != previousSize);

        if (LOG.isDebugEnabled()) {
            for (PsiElement element : filteredElements) {
                LOG.debug("filtered element = " + element);
            }
        }

        return PsiUtilCore.toPsiElementArray(filteredElements);
    }

    public static boolean treeWalkUp(
        @Nonnull final PsiScopeProcessor processor,
        @Nonnull final PsiElement entrance,
        @Nullable final PsiElement maxScope,
        @Nonnull final ResolveState state
    ) {
        PsiElement prevParent = entrance;
        PsiElement scope = entrance;

        while (scope != null) {
            if (!scope.processDeclarations(processor, state, prevParent, entrance)) {
                return false;
            }

            if (scope == maxScope) {
                break;
            }
            prevParent = scope;
            scope = prevParent.getContext();
        }

        return true;
    }

    public static boolean treeWalkUp(
        @Nonnull final PsiElement entrance,
        @Nullable final PsiElement maxScope,
        BiPredicate<PsiElement, PsiElement> eachScopeAndLastParent
    ) {
        PsiElement prevParent = null;
        PsiElement scope = entrance;

        while (scope != null) {
            if (!eachScopeAndLastParent.test(scope, prevParent)) {
                return false;
            }

            if (scope == maxScope) {
                break;
            }
            prevParent = scope;
            scope = prevParent.getContext();
        }

        return true;
    }

    @Nonnull
    public static PsiElement findPrevParent(@Nonnull PsiElement ancestor, @Nonnull PsiElement descendant) {
        PsiElement cur = descendant;
        while (cur != null) {
            final PsiElement parent = cur.getParent();
            if (parent == ancestor) {
                return cur;
            }
            cur = parent;
        }
        throw new AssertionError(descendant + " is not a descendant of " + ancestor);
    }

    @RequiredReadAction
    public static List<PsiElement> getInjectedElements(@Nonnull OuterLanguageElement outerLanguageElement) {
        PsiElement psi = outerLanguageElement.getContainingFile().getViewProvider().getPsi(outerLanguageElement.getLanguage());
        TextRange injectionRange = outerLanguageElement.getTextRange();
        List<PsiElement> res = new ArrayList<>();

        assert psi != null : outerLanguageElement;
        for (PsiElement element = psi.findElementAt(injectionRange.getStartOffset());
             element != null && injectionRange.intersectsStrict(element.getTextRange());
             element = element.getNextSibling()) {
            res.add(element);
        }

        return res;
    }

    @Nonnull
    @RequiredReadAction
    public static LanguageVersion getLanguageVersion(@Nonnull PsiElement element) {
        LanguageVersion languageVersion = element.getUserData(LanguageVersion.KEY);
        if (languageVersion != null) {
            return languageVersion;
        }

        Language language = element.getLanguage();
        PsiFile containingFile = element.getContainingFile();
        if (containingFile != null && containingFile != element && containingFile.getLanguage() == language) {
            return containingFile.getLanguageVersion();
        }

        return LanguageCachedValueUtil.getCachedValue(
            element,
            () -> {
                final LanguageVersionResolver versionResolver = LanguageVersionResolver.forLanguage(language);
                //noinspection RequiredXAction
                return CachedValueProvider.Result.create(
                    versionResolver.getLanguageVersion(language, element),
                    PsiModificationTracker.MODIFICATION_COUNT
                );
            }
        );
    }

    @Nullable
    @Contract("null -> null")
    @RequiredReadAction
    public static PsiElement skipWhitespacesForward(@Nullable PsiElement element) {
        return skipSiblingsForward(element, WS);
    }

    @Nullable
    @Contract("null -> null")
    @RequiredReadAction
    public static PsiElement skipWhitespacesAndCommentsForward(@Nullable PsiElement element) {
        return skipSiblingsForward(element, WS_COMMENTS);
    }


    @SafeVarargs
    @Nullable
    @Contract("null, _ -> null")
    @RequiredReadAction
    public static PsiElement skipSiblingsForward(@Nullable PsiElement element, @Nonnull Class<? extends PsiElement>... elementClasses) {
        if (element == null) {
            return null;
        }
        for (PsiElement e = element.getNextSibling(); e != null; e = e.getNextSibling()) {
            if (!PsiTreeUtil.instanceOf(e, elementClasses)) {
                return e;
            }
        }
        return null;
    }

    @Nullable
    @Contract("null -> null")
    @RequiredReadAction
    public static PsiElement skipWhitespacesBackward(@Nullable PsiElement element) {
        return skipSiblingsBackward(element, WS);
    }

    @Nullable
    @Contract("null -> null")
    @RequiredReadAction
    public static PsiElement skipWhitespacesAndCommentsBackward(@Nullable PsiElement element) {
        return skipSiblingsBackward(element, WS_COMMENTS);
    }

    @SafeVarargs
    @Nullable
    @Contract("null, _ -> null")
    @RequiredReadAction
    public static PsiElement skipSiblingsBackward(@Nullable PsiElement element, @Nonnull Class<? extends PsiElement>... elementClasses) {
        if (element == null) {
            return null;
        }
        for (PsiElement e = element.getPrevSibling(); e != null; e = e.getPrevSibling()) {
            if (!PsiTreeUtil.instanceOf(e, elementClasses)) {
                return e;
            }
        }
        return null;
    }

    @Nonnull
    @RequiredReadAction
    public static <T extends PsiElement> Iterator<T> childIterator(@Nonnull final PsiElement element, @Nonnull final Class<T> aClass) {
        return new Iterator<>() {
            private T next = getChildOfType(element, aClass);

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            @RequiredReadAction
            public T next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                T current = this.next;
                next = getNextSiblingOfType(current, aClass);
                return current;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns the same element in the file copy.
     *
     * @param element an element to find
     * @param copy    file that must be a copy of {@code element.getContainingFile()}
     * @return found element; null if input element is null
     * @throws IllegalStateException if it's detected that the supplied file is not exact copy of original file.
     *                               The exception is thrown on a best-effort basis, so you cannot rely on it.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T findSameElementInCopy(@Nullable T element, @Nonnull PsiFile copy) throws IllegalStateException {
        if (element == null) {
            return null;
        }
        IntList offsets = IntLists.newArrayList();
        PsiElement cur = element;
        while (!cur.getClass().equals(copy.getClass())) {
            int pos = 0;
            for (PsiElement sibling = cur.getPrevSibling(); sibling != null; sibling = sibling.getPrevSibling()) {
                pos++;
            }
            offsets.add(pos);
            cur = cur.getParent();
            if (cur == null) {
                throw new IllegalStateException("Cannot find parent file; element class: " + element.getClass());
            }
        }
        cur = copy;
        for (int level = offsets.size() - 1; level >= 0; level--) {
            int pos = offsets.get(level);
            cur = cur.getFirstChild();
            if (cur == null) {
                throw new IllegalStateException("File structure differs: no child");
            }
            for (int i = 0; i < pos; i++) {
                cur = cur.getNextSibling();
                if (cur == null) {
                    throw new IllegalStateException("File structure differs: number of siblings is less than " + pos);
                }
            }
        }
        if (!cur.getClass().equals(element.getClass())) {
            throw new IllegalStateException("File structure differs: " + cur.getClass() + " != " + element.getClass());
        }
        return (T)cur;
    }

    /**
     * @return closest leaf (not necessarily a sibling) before the given element
     * which has non-empty range and is neither a whitespace nor a comment
     */
    @Nullable
    @RequiredReadAction
    public static PsiElement prevCodeLeaf(@Nonnull PsiElement element) {
        PsiElement prevLeaf = prevLeaf(element, true);
        while (prevLeaf != null && isNonCodeLeaf(prevLeaf)) prevLeaf = prevLeaf(prevLeaf, true);
        return prevLeaf;
    }

    /**
     * @return closest leaf (not necessarily a sibling) after the given element
     * which has non-empty range and is neither a whitespace nor a comment
     */
    @Nullable
    @RequiredReadAction
    public static PsiElement nextCodeLeaf(@Nonnull PsiElement element) {
        PsiElement nextLeaf = nextLeaf(element, true);
        while (nextLeaf != null && isNonCodeLeaf(nextLeaf)) nextLeaf = nextLeaf(nextLeaf, true);
        return nextLeaf;
    }

    @RequiredReadAction
    private static boolean isNonCodeLeaf(PsiElement leaf) {
        return StringUtil.isEmptyOrSpaces(leaf.getText()) || getNonStrictParentOfType(leaf, PsiComment.class) != null;
    }
}