// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.ui.view.impl.internal.nesting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.project.ui.view.internal.ProjectViewSharedSettings;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.function.Function;

/**
 * Helper component that stores nesting rules and apply them to files
 *
 * @see NestingTreeStructureProvider
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public final class FileNestingBuilder {
    public static FileNestingBuilder getInstance() {
        return ApplicationManager.getApplication().getService(FileNestingBuilder.class);
    }

    private long myBaseListModCount = -1;
    private Set<ProjectViewFileNestingService.NestingRule> myNestingRules;

    /**
     * Returns all possible nesting rules, including transitive rules
     */
    public synchronized @Nonnull Collection<ProjectViewFileNestingService.NestingRule> getNestingRules() {
        final ProjectViewFileNestingService fileNestingService = ProjectViewFileNestingService.getInstance();
        final List<ProjectViewFileNestingService.NestingRule> baseRules = fileNestingService.getRules();
        final long modCount = fileNestingService.getModificationCount();

        if (myNestingRules == null || myBaseListModCount != modCount) {
            myNestingRules = new HashSet<>();
            myBaseListModCount = modCount;

            final MultiMap<String, String> childToParentSuffix = new MultiMap<>();
            final MultiMap<String, String> parentToChildSuffix = new MultiMap<>();

            for (ProjectViewFileNestingService.NestingRule rule : baseRules) {
                final String parentFileSuffix = rule.getParentFileSuffix();
                final String childFileSuffix = rule.getChildFileSuffix();
                if (parentFileSuffix.isEmpty() || childFileSuffix.isEmpty()) {
                    continue; // shouldn't happen, checked on component loading and in UI
                }
                if (parentFileSuffix.equals(childFileSuffix)) {
                    continue; // shouldn't happen, checked on component loading and in UI
                }

                myNestingRules.add(rule);
                childToParentSuffix.putValue(childFileSuffix, parentFileSuffix);
                parentToChildSuffix.putValue(parentFileSuffix, childFileSuffix);

                // for all cases like A -> B -> C we also add a rule A -> C
                for (String s : parentToChildSuffix.get(childFileSuffix)) {
                    myNestingRules.add(new ProjectViewFileNestingService.NestingRule(parentFileSuffix, s));
                    parentToChildSuffix.putValue(parentFileSuffix, s);
                    childToParentSuffix.putValue(s, parentFileSuffix);
                }

                for (String s : childToParentSuffix.get(parentFileSuffix)) {
                    myNestingRules.add(new ProjectViewFileNestingService.NestingRule(s, childFileSuffix));
                    parentToChildSuffix.putValue(s, childFileSuffix);
                    childToParentSuffix.putValue(childFileSuffix, s);
                }
            }
        }

        return myNestingRules;
    }

    public boolean isNestedFile(Project project, VirtualFile file) {
        ProjectViewSharedSettings settings = project.getApplication().getInstance(ProjectViewSharedSettings.class);

        if (!settings.getViewOption(ShowNestedProjectViewPaneOptionProvider.SHOW_NESTED_FILES_KEY)) {
            return false;
        }

        String fileName = file.getName();
        for (ProjectViewFileNestingService.NestingRule rule : getNestingRules()) {
            if (!StringUtil.endsWithIgnoreCase(fileName, rule.getChildFileSuffix())) {
                continue;
            }
            VirtualFile directory = file.getParent();
            if (directory == null || !directory.isDirectory()) {
                return false;
            }
            String parentName = StringUtil.trimEnd(fileName, rule.getChildFileSuffix()) + rule.getParentFileSuffix();
            if (directory.findChild(parentName) != null) {
                return true;
            }
        }
        return false;
    }

    /*
      This is a graph theory problem. T is a graph node that represents a file. fileNameFunc should return appropriate file name for a T node.
      Edges go from parent file to child file according to NestingRules, for example foo.js->foo.min.js.
      Parent may have several children. Child may have several parents.
      There may be cycles with 3 or more nodes, but cycle with 2 nodes (A->B and B->A) is impossible because parentFileSuffix != childFileSuffix
      For each child its outbound edges are removed. For example in case of a cycle all edges that form it are removed. In case of A->B->C only A->B remains.
      As a result we get a number of separated parent-to-many-children sub-graphs, and use them to nest child files under parent file in Project View.
      One child still may have more than one parent. For real use cases it is not expected to happen, but anyway it's not a big problem, it will be shown as a subnode more than once.
     */
    public @Nonnull <T> MultiMap<T, T> mapParentToChildren(final @Nonnull Collection<? extends T> nodes,
                                                           final @Nonnull Function<? super T, String> fileNameFunc) {

        final Collection<ProjectViewFileNestingService.NestingRule> rules = getNestingRules();
        if (rules.isEmpty()) {
            return MultiMap.empty();
        }

        // result that will contain number of separated parent-to-many-children sub-graphs
        MultiMap<T, T> parentToChildren = null;

        Set<T> allChildNodes = null; // helps to remove all outbound edges of a node that has inbound edge itself
        Map<Pair<String, ProjectViewFileNestingService.NestingRule>, Edge<T>> baseNameAndRuleToEdge = null; // temporary map for building edges

        for (T node : nodes) {
            final String fileName = fileNameFunc.apply(node);
            if (fileName == null) {
                continue;
            }

            for (ProjectViewFileNestingService.NestingRule rule : rules) {
                final Couple<Boolean> c = checkMatchingAsParentOrChild(rule, fileName);
                final boolean matchesParent = c.first;
                final boolean matchesChild = c.second;

                if (!matchesChild && !matchesParent) {
                    continue;
                }

                if (baseNameAndRuleToEdge == null) {
                    baseNameAndRuleToEdge = new HashMap<>();
                    parentToChildren = new MultiMap<>();
                    allChildNodes = new HashSet<>();
                }

                if (matchesParent) {
                    final String baseName = fileName.substring(0, fileName.length() - rule.getParentFileSuffix().length());
                    final Edge<T> edge = getOrCreateEdge(baseNameAndRuleToEdge, baseName, rule);
                    edge.from = node;
                    updateInfoIfEdgeComplete(parentToChildren, allChildNodes, edge);
                }

                if (matchesChild) {
                    final String baseName = fileName.substring(0, fileName.length() - rule.getChildFileSuffix().length());
                    final Edge<T> edge = getOrCreateEdge(baseNameAndRuleToEdge, baseName, rule);
                    edge.to = node;
                    updateInfoIfEdgeComplete(parentToChildren, allChildNodes, edge);
                }
            }
        }

        return parentToChildren == null ? MultiMap.empty() : parentToChildren;
    }

    /**
     * Returns [matching parent; matching child] pair
     */
    public static Couple<Boolean> checkMatchingAsParentOrChild(final @Nonnull ProjectViewFileNestingService.NestingRule rule,
                                                               final @Nonnull String fileName) {
        String parentFileSuffix = rule.getParentFileSuffix();
        String childFileSuffix = rule.getChildFileSuffix();

        boolean matchesParent = /*!fileName.equalsIgnoreCase(parentFileSuffix) &&*/ StringUtil.endsWithIgnoreCase(fileName, parentFileSuffix);
        boolean matchesChild = /*!fileName.equalsIgnoreCase(childFileSuffix) &&*/ StringUtil.endsWithIgnoreCase(fileName, childFileSuffix);

        if (matchesParent && matchesChild) {
            if (parentFileSuffix.length() > childFileSuffix.length()) {
                matchesChild = false;
            }
            else {
                matchesParent = false;
            }
        }

        return Couple.of(matchesParent, matchesChild);
    }

    private static @Nonnull <T> Edge<T> getOrCreateEdge(final @Nonnull Map<Pair<String, ProjectViewFileNestingService.NestingRule>, Edge<T>> baseNameAndRuleToEdge,
                                                        final @Nonnull String baseName,
                                                        final @Nonnull ProjectViewFileNestingService.NestingRule rule) {
        final Pair<String, ProjectViewFileNestingService.NestingRule> baseNameAndRule = Pair.create(baseName, rule);

        Edge<T> edge = baseNameAndRuleToEdge.get(baseNameAndRule);
        if (edge == null) {
            edge = new Edge<>();
            baseNameAndRuleToEdge.put(baseNameAndRule, edge);
        }
        return edge;
    }

    private static <T> void updateInfoIfEdgeComplete(final @Nonnull MultiMap<T, T> parentToChildren,
                                                     final @Nonnull Set<? super T> allChildNodes,
                                                     final @Nonnull Edge<? extends T> edge) {
        if (edge.from != null && edge.to != null) { // if edge complete
            allChildNodes.add(edge.to);
            parentToChildren.remove(edge.to); // nodes that appear as a child shouldn't be a parent of another edge, corresponding edges removed
            if (!allChildNodes.contains(edge.from)) {
                parentToChildren.putValue(edge.from, edge.to);
            }
        }
    }

    private static final class Edge<T> {
        private @Nullable T from;
        private @Nullable T to;
    }
}
