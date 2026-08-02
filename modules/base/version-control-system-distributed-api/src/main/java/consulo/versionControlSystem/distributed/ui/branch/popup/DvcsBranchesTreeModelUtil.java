// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.distributed.ui.branch.popup;

import consulo.annotation.UsedInPlugin;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.SeparatorWithText;
import consulo.ui.ex.awt.tree.TreePathUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author VISTALL
 */
public final class DvcsBranchesTreeModelUtil {
    private DvcsBranchesTreeModelUtil() {
    }

    public static final Comparator<DvcsRef> EMPTY_BRANCH_COMPARATOR = (a, b) -> 0;

    /**
     * Weight added to the matching degree to prefer names starting with the pattern.
     */
    private static final int MATCH_OFFSET = 10000;

    static final class MatchResult<N> {
        final Collection<N> matchedNodes;
        @Nullable
        final N topMatch;

        MatchResult(Collection<N> matchedNodes, @Nullable N topMatch) {
            this.matchedNodes = matchedNodes;
            this.topMatch = topMatch;
        }
    }

    @UsedInPlugin
    public static List<Object> buildBranchTreeNodes(DvcsRefType branchType,
                                                    Map<String, Object> branchesMap,
                                                    List<String> path,
                                                    @Nullable DvcsRepositoryModel repository) {
        if (path.isEmpty()) {
            return mapToNodes(branchesMap, branchType, path, repository);
        }
        Map<String, Object> currentLevel = branchesMap;
        for (String prefixPart : path) {
            Object next = currentLevel.get(prefixPart);
            if (next instanceof Map<?, ?> map) {
                //noinspection unchecked
                currentLevel = (Map<String, Object>) map;
            }
            else {
                return List.of();
            }
        }
        return mapToNodes(currentLevel, branchType, path, repository);
    }

    private static List<Object> mapToNodes(Map<String, Object> map,
                                           DvcsRefType branchType,
                                           List<String> path,
                                           @Nullable DvcsRepositoryModel repository) {
        List<Object> result = new ArrayList<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof DvcsRef ref && repository != null) {
                result.add(new DvcsBranchesTreeModel.RefUnderRepository(repository, ref));
            }
            else if (value instanceof Map<?, ?>) {
                List<String> newPath = new ArrayList<>(path);
                newPath.add(entry.getKey());
                result.add(new DvcsBranchesTreeModel.BranchesPrefixGroup(branchType, newPath, repository));
            }
            else {
                result.add(value);
            }
        }
        return result;
    }

    @Nullable
    public static TreePath createTreePathFor(DvcsBranchesTreeModel model, Object value) {
        Object root = model.getRoot();

        if (value instanceof AnAction) {
            return TreePathUtil.convertArrayToTreePath(root, value);
        }

        if (value instanceof DvcsBranchesTreeModel.RepositoryNode repositoryNode) {
            return TreePathUtil.convertArrayToTreePath(root, repositoryNode);
        }

        if (value instanceof DvcsBranchesTreeModel.RefTypeUnderRepository typeUnderRepository) {
            return TreePathUtil.convertArrayToTreePath(
                root,
                new DvcsBranchesTreeModel.RepositoryNode(typeUnderRepository.repository(), false),
                typeUnderRepository);
        }

        DvcsBranchesTreeModel.RefUnderRepository refUnderRepository =
            value instanceof DvcsBranchesTreeModel.RefUnderRepository r ? r : null;
        DvcsRef reference = value instanceof DvcsRef r ? r : (refUnderRepository != null ? refUnderRepository.ref() : null);
        if (reference == null) {
            return null;
        }

        DvcsRefType refType = model.getRefType(reference);
        List<Object> path = new ArrayList<>();
        path.add(root);
        if (refUnderRepository != null) {
            path.add(new DvcsBranchesTreeModel.RepositoryNode(refUnderRepository.repository(), false));
            path.add(new DvcsBranchesTreeModel.RefTypeUnderRepository(refUnderRepository.repository(), refType));
        }
        else {
            path.add(refType);
        }

        List<String> nameParts = model.isDirectoryGrouping() ? List.of(reference.getName().split("/")) : List.of(reference.getName());
        DvcsRepositoryModel repo = refUnderRepository != null ? refUnderRepository.repository() : null;
        List<String> currentPrefix = new ArrayList<>();
        for (int i = 0; i < nameParts.size() - 1; i++) {
            currentPrefix.add(nameParts.get(i));
            path.add(new DvcsBranchesTreeModel.BranchesPrefixGroup(refType, new ArrayList<>(currentPrefix), repo));
        }

        if (refUnderRepository != null) {
            path.add(refUnderRepository);
        }
        else {
            path.add(reference);
        }
        return TreePathUtil.convertArrayToTreePath(path.toArray());
    }

    static <N> MatchResult<N> match(@Nullable MinusculeMatcher matcher,
                                    Collection<N> nodes,
                                    Function<N, String> nodeNameSupplier,
                                    Predicate<N> exceptFilter) {
        if (nodes.isEmpty() || matcher == null) {
            return new MatchResult<>(nodes, null);
        }

        List<N> result = new ArrayList<>();
        N topMatchNode = null;
        int topMatchDegree = Integer.MIN_VALUE;
        for (N node : nodes) {
            if (exceptFilter.test(node)) {
                continue;
            }
            String name = nodeNameSupplier.apply(node);
            var matchingFragments = matcher.matchingFragments(name);
            if (matchingFragments == null) {
                continue;
            }
            result.add(node);
            int matchingDegree = matcher.matchingDegree(name, false, matchingFragments);
            var firstFragment = matchingFragments.getHead();
            if (firstFragment != null) {
                matchingDegree += MATCH_OFFSET - firstFragment.getStartOffset();
            }
            if (topMatchNode == null || topMatchDegree < matchingDegree) {
                topMatchNode = node;
                topMatchDegree = matchingDegree;
            }
        }
        return new MatchResult<>(result, topMatchNode);
    }

    @UsedInPlugin
    public static List<Object> addSeparatorIfNeeded(Collection<Object> nodes, SeparatorWithText separator) {
        List<Object> result = new ArrayList<>(nodes);
        if (!result.isEmpty() && !(result.get(result.size() - 1) instanceof SeparatorWithText)) {
            result.add(separator);
        }
        return result;
    }

    @UsedInPlugin
    public static class LazyRepositoryHolder extends LazyHolder<DvcsBranchesTreeModel.RepositoryNode> {
        LazyRepositoryHolder(Project project,
                             List<DvcsRepositoryModel> repositories,
                             @Nullable MinusculeMatcher matcher,
                             boolean canHaveChildren,
                             BooleanSupplier needFilter) {
            super(map(repositories, repo -> new DvcsBranchesTreeModel.RepositoryNode(repo, !canHaveChildren)),
                matcher,
                node -> false,
                node -> node.repository().getShortName(),
                needFilter);
        }
    }

    @UsedInPlugin
    public static class LazyActionsHolder extends LazyHolder<Object> {
        LazyActionsHolder(Project project, List<Object> actions, @Nullable MinusculeMatcher matcher) {
            super(actions,
                matcher,
                node -> node instanceof SeparatorWithText,
                LazyActionsHolder::actionName,
                () -> matcher != null);
        }

        private static String actionName(Object node) {
            if (node instanceof AnAction action) {
                String text = action.getTemplatePresentation().getText();
                return text != null ? text : node.toString();
            }
            return node.toString();
        }
    }

    static class LazyHolder<N> {
        private final boolean myInitiallyEmpty;
        private final List<N> myNodes;
        @Nullable
        private final MinusculeMatcher myMatcher;
        private final Predicate<N> myExceptFilter;
        private final Function<N, String> myNodeNameSupplier;
        private final BooleanSupplier myNeedFilter;

        private MatchResult<N> myMatchingResult;

        LazyHolder(List<N> nodes,
                   @Nullable MinusculeMatcher matcher,
                   Predicate<N> exceptFilter,
                   Function<N, String> nodeNameSupplier,
                   BooleanSupplier needFilter) {
            myNodes = nodes;
            myInitiallyEmpty = nodes.isEmpty();
            myMatcher = matcher;
            myExceptFilter = exceptFilter;
            myNodeNameSupplier = nodeNameSupplier;
            myNeedFilter = needFilter;
        }

        private MatchResult<N> matchingResult() {
            if (myMatchingResult == null) {
                myMatchingResult = match(myNeedFilter.getAsBoolean() ? myMatcher : null, myNodes, myNodeNameSupplier, myExceptFilter);
            }
            return myMatchingResult;
        }

        @Nullable
        @UsedInPlugin
        public N getTopMatch() {
            return matchingResult().topMatch;
        }

        public Collection<N> getMatch() {
            return matchingResult().matchedNodes;
        }

        public boolean isEmpty() {
            return myInitiallyEmpty || !myNeedFilter.getAsBoolean() || getMatch().isEmpty();
        }
    }

    public static class LazyRefsSubtreeHolder {
        private final boolean myInitiallyEmpty;
        private final Collection<DvcsRef> myUnsortedRefs;
        @Nullable
        private final MinusculeMatcher myMatcher;
        private final BooleanSupplier myDirectoryGrouping;
        private final Predicate<DvcsRef> myExceptRefFilter;
        private final Supplier<Comparator<DvcsRef>> myRefComparatorGetter;

        private List<DvcsRef> mySortedValues;
        private MatchResult<DvcsRef> myMatchingResult;
        private Map<String, Object> myTree;

        LazyRefsSubtreeHolder(Collection<DvcsRef> unsortedRefs,
                              @Nullable MinusculeMatcher matcher,
                              BooleanSupplier directoryGrouping,
                              Predicate<DvcsRef> exceptRefFilter,
                              Supplier<Comparator<DvcsRef>> refComparatorGetter) {
            myUnsortedRefs = unsortedRefs;
            myInitiallyEmpty = unsortedRefs.isEmpty();
            myMatcher = matcher;
            myDirectoryGrouping = directoryGrouping;
            myExceptRefFilter = exceptRefFilter;
            myRefComparatorGetter = refComparatorGetter;
        }

        List<DvcsRef> getSortedValues() {
            if (mySortedValues == null) {
                List<DvcsRef> sorted = new ArrayList<>(myUnsortedRefs);
                sorted.sort(myRefComparatorGetter.get());
                mySortedValues = sorted;
            }
            return mySortedValues;
        }

        boolean isEmpty() {
            return myInitiallyEmpty || matchingResult().matchedNodes.isEmpty();
        }

        private MatchResult<DvcsRef> matchingResult() {
            if (myMatchingResult == null) {
                myMatchingResult = match(myMatcher, getSortedValues(), DvcsRef::getName, myExceptRefFilter);
            }
            return myMatchingResult;
        }

        Map<String, Object> getTree() {
            if (myTree == null) {
                List<Map.Entry<List<String>, DvcsRef>> pathAndRefs = new ArrayList<>();
                for (DvcsRef ref : matchingResult().matchedNodes) {
                    List<String> parts = myDirectoryGrouping.getAsBoolean()
                        ? List.of(ref.getName().split("/"))
                        : List.of(ref.getName());
                    pathAndRefs.add(Map.entry(parts, ref));
                }
                myTree = buildSubTree(pathAndRefs);
            }
            return myTree;
        }

        @Nullable
        @UsedInPlugin
        public DvcsRef getTopMatch() {
            return matchingResult().topMatch;
        }

        private Map<String, Object> buildSubTree(List<Map.Entry<List<String>, DvcsRef>> prevLevel) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            LinkedHashMap<String, List<Map.Entry<List<String>, DvcsRef>>> groups = new LinkedHashMap<>();
            for (Map.Entry<List<String>, DvcsRef> entry : prevLevel) {
                List<String> pathParts = entry.getKey();
                DvcsRef branch = entry.getValue();
                String firstPathPart = pathParts.get(0);
                List<String> restOfThePath = pathParts.subList(1, pathParts.size());
                if (restOfThePath.isEmpty()) {
                    result.put(firstPathPart, branch);
                }
                else {
                    List<Map.Entry<List<String>, DvcsRef>> groupChildren = groups.computeIfAbsent(firstPathPart, k -> {
                        // Preserve the order in the LinkedHashMap, it will be overwritten below.
                        result.put(firstPathPart, Map.of());
                        return new ArrayList<>();
                    });
                    groupChildren.add(Map.entry(restOfThePath, branch));
                }
            }

            for (Map.Entry<String, List<Map.Entry<List<String>, DvcsRef>>> group : groups.entrySet()) {
                result.put(group.getKey(), buildSubTree(group.getValue()));
            }

            return result;
        }

        static LazyRefsSubtreeHolder emptyHolder() {
            return new LazyRefsSubtreeHolder(List.of(), null, () -> false, ref -> false, () -> EMPTY_BRANCH_COMPARATOR);
        }
    }

    static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        List<R> result = new ArrayList<>(collection.size());
        for (T t : collection) {
            result.add(mapper.apply(t));
        }
        return result;
    }
}
