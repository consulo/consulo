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

import consulo.application.util.matcher.MinusculeMatcher;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.tree.AbstractTreeModel;
import consulo.ui.ex.tree.PathElementIdProvider;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author VISTALL
 */
public abstract class DvcsBranchesTreeModel extends AbstractTreeModel {
    protected final Project myProject;
    private final List<Object> myActions;
    protected final List<DvcsRepositoryModel> myRepositories;

    protected DvcsBranchesTreeModelUtil.LazyActionsHolder myActionsTree;
    protected final Map<DvcsRefType, DvcsBranchesTreeModelUtil.LazyRefsSubtreeHolder> myRefTrees = new HashMap<>();

    protected final Map<Object, List<Object>> myBranchesTreeCache = new HashMap<>();

    @Nullable
    private MinusculeMatcher myNameMatcher = null;

    private boolean myDirectoryGrouping;

    protected DvcsBranchesTreeModel(Project project, List<Object> actions, List<DvcsRepositoryModel> repositories) {
        myProject = project;
        myActions = actions;
        myRepositories = repositories;
        myActionsTree = new DvcsBranchesTreeModelUtil.LazyActionsHolder(project, List.of(), null);
        myDirectoryGrouping = isDirectoryGroupingEnabledByDefault();
    }

    protected boolean isDirectoryGroupingEnabledByDefault() {
        return false;
    }

    public boolean isDirectoryGrouping() {
        return myDirectoryGrouping;
    }

    public void setDirectoryGrouping(boolean directoryGrouping) {
        if (myDirectoryGrouping != directoryGrouping) {
            myDirectoryGrouping = directoryGrouping;
            applyFilterAndRebuild(null);
        }
    }

    @Nullable
    public MinusculeMatcher getNameMatcher() {
        return myNameMatcher;
    }

    public void init() {
        applyFilterAndRebuild(null);
    }

    public void applyFilterAndRebuild(@Nullable MinusculeMatcher matcher) {
        myNameMatcher = matcher;
        rebuild(matcher);
        treeStructureChanged(new TreePath(new Object[]{getRoot()}), null, null);
    }

    protected void rebuild(@Nullable MinusculeMatcher matcher) {
        myBranchesTreeCache.clear();
        myActionsTree = new DvcsBranchesTreeModelUtil.LazyActionsHolder(myProject, myActions, matcher);
        myRefTrees.clear();
        for (DvcsRefType type : getSupportedRefTypes()) {
            myRefTrees.put(type, new DvcsBranchesTreeModelUtil.LazyRefsSubtreeHolder(
                getRefs(type),
                matcher,
                this::isDirectoryGrouping,
                exceptRefFilter(type),
                () -> getComparatorForType(type)));
        }
    }

    /**
     * Predicate marking refs that must be excluded from the given type's subtree (e.g. recent branches
     * excluded from the LOCAL subtree). Empty by default.
     */
    protected java.util.function.Predicate<DvcsRef> exceptRefFilter(DvcsRefType type) {
        return ref -> false;
    }

    /**
     * Comparator used to sort refs of the given type. Defaults to the standard current/favorite/directory/name
     * comparator; override for types (e.g. RECENT) that preserve insertion order.
     */
    protected Comparator<DvcsRef> getComparatorForType(DvcsRefType type) {
        return getRefComparator(myRepositories);
    }

    public abstract TreePath getPreferredSelection();

    /**
     * All reference groups this model can display, in tree display order.
     */
    protected abstract List<DvcsRefType> getSupportedRefTypes();

    /**
     * Unsorted references for the given type.
     */
    protected abstract java.util.Collection<DvcsRef> getRefs(DvcsRefType type);

    /**
     * Resolves the display ref type of a reference, used to build a tree path to it.
     */
    public abstract DvcsRefType getRefType(DvcsRef ref);

    @Override
    public final Object getRoot() {
        return TreeRoot.INSTANCE;
    }

    @Override
    public final Object getChild(Object parent, int index) {
        return getChildren(parent).get(index);
    }

    @Override
    public final int getChildCount(Object parent) {
        return getChildren(parent).size();
    }

    @Override
    public final int getIndexOfChild(Object parent, Object child) {
        return getChildren(parent).indexOf(child);
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof DvcsRef
            || node instanceof RefUnderRepository
            || (node instanceof DvcsRefType type && getCorrespondingTree(type).isEmpty());
    }

    protected abstract List<Object> getChildren(@Nullable Object parent);

    protected boolean areRefTreesEmpty() {
        for (DvcsRefType type : getSupportedRefTypes()) {
            if (!getCorrespondingTree(type).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    protected Map<String, Object> getCorrespondingTree(DvcsRefType refType) {
        DvcsBranchesTreeModelUtil.LazyRefsSubtreeHolder holder = myRefTrees.get(refType);
        return holder == null ? Map.of() : holder.getTree();
    }

    protected Comparator<DvcsRef> getRefComparator(List<DvcsRepositoryModel> affectedRepositories) {
        Comparator<DvcsRef> byCurrent = Comparator.comparing(ref -> !isCurrentRefInAny(ref, affectedRepositories));
        Comparator<DvcsRef> byFavorite = Comparator.comparing(ref -> !isFavoriteInAll(ref, affectedRepositories));
        Comparator<DvcsRef> byDirectory = Comparator.comparing(ref -> !(isDirectoryGrouping() && ref.getName().contains("/")));
        Comparator<DvcsRef> byName = (a, b) -> DvcsRef.REFS_NAMES_COMPARATOR.compare(a.getName(), b.getName());
        return byCurrent.thenComparing(byFavorite).thenComparing(byDirectory).thenComparing(byName);
    }

    protected Comparator<Object> getSubTreeComparator() {
        Comparator<Object> byRefNotHighlighted = Comparator.comparing(node ->
            node instanceof DvcsRef ref
                && !isCurrentRefInAny(ref, myRepositories)
                && !isFavoriteInAll(ref, myRepositories));
        Comparator<Object> byPrefixGroup = Comparator.comparing(node -> node instanceof BranchesPrefixGroup);
        return byRefNotHighlighted.thenComparing(byPrefixGroup);
    }

    private boolean isCurrentRefInAny(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        for (DvcsRepositoryModel repo : repositories) {
            if (repo.isCurrentRef(ref)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFavoriteInAll(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        for (DvcsRepositoryModel repo : repositories) {
            if (!repo.isFavorite(ref)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a given node is selectable. Such "selectable" nodes may have special
     * handling in renderers or navigation: e.g. custom icons or custom navigation.
     */
    public boolean isSelectable(@Nullable Object node) {
        if (node == null) {
            return false;
        }
        return (node instanceof RepositoryNode repoNode && repoNode.isLeaf())
            || node instanceof DvcsRef
            || node instanceof RefUnderRepository
            || (node instanceof AnAction action && action.getTemplatePresentation().isEnabled());
    }

    // === node types ===

    public enum TreeRoot implements PathElementIdProvider {
        INSTANCE;

        public static final String NAME = "TreeRoot";

        @Override
        public String getPathElementId() {
            return NAME;
        }
    }

    public record BranchesPrefixGroup(DvcsRefType type, List<String> prefix, @Nullable DvcsRepositoryModel repository)
        implements PathElementIdProvider {

        public BranchesPrefixGroup(DvcsRefType type, List<String> prefix) {
            this(type, prefix, null);
        }

        @Override
        public String getPathElementId() {
            return type.getName() + "/" + prefix.toString();
        }
    }

    public record RefTypeUnderRepository(DvcsRepositoryModel repository, DvcsRefType type) {
    }

    public record RepositoryNode(DvcsRepositoryModel repository, boolean isLeaf) implements PresentableNode {
        @Override
        public String getPresentableText() {
            return repository.getShortName();
        }
    }

    public record RefUnderRepository(DvcsRepositoryModel repository, DvcsRef ref) implements PresentableNode {
        @Override
        public String getPresentableText() {
            return ref.getName();
        }
    }

    public interface PresentableNode extends ItemPresentation {
        @Nullable
        @Override
        default String getLocationString() {
            return null;
        }

        @Nullable
        @Override
        default Image getIcon() {
            return null;
        }
    }
}
