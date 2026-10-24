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

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.distributed.branch.DvcsInOutCountersInProject;
import consulo.versionControlSystem.distributed.branch.DvcsInOutCountersInRepo;
import consulo.versionControlSystem.distributed.ui.DvcsIncomingOutgoingUi;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public final class DvcsBranchesTreeNodePresentation {
    private final Supplier<DvcsBranchesTreeModel> myTreeModel;
    private final @Nullable DvcsRepositoryModel mySelectedRepository;
    private final List<DvcsRepositoryModel> myRepositories;
    private final List<DvcsRepositoryModel> myAffectedRepositories;

    public DvcsBranchesTreeNodePresentation(Supplier<DvcsBranchesTreeModel> treeModel,
                                            @Nullable DvcsRepositoryModel selectedRepository,
                                            List<DvcsRepositoryModel> repositories) {
        myTreeModel = treeModel;
        mySelectedRepository = selectedRepository;
        myRepositories = repositories;
        myAffectedRepositories = selectedRepository != null ? List.of(selectedRepository) : repositories;
    }

    public void render(@Nullable Object node, TextItemPresentation presentation) {
        presentation.withIcon(getIcon(node, false, false));
        presentation.append(StringUtil.notNullize(getText(node)), getTextAttribute(node));
        renderInOut(node, presentation);

        String secondaryText = getSecondaryText(node);
        if (secondaryText != null) {
            presentation.append("  " + secondaryText, TextAttribute.GRAYED);
        }
    }

    public void renderInOut(@Nullable Object node, TextItemPresentation presentation) {
        DvcsInOutCountersInProject counters = getInOutCounters(node);
        if (counters.isEmpty()) {
            return;
        }
        DvcsIncomingOutgoingUi.appendCounters(presentation, counters);
        LocalizeValue tooltip = counters.calcTooltip(getLastFetchTime());
        if (tooltip != LocalizeValue.empty()) {
            presentation.withTooltip(tooltip);
        }
    }

    public DvcsInOutCountersInProject getInOutCounters(@Nullable Object node) {
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getInOutCounters(refUnderRepository.ref(), List.of(refUnderRepository.repository()));
        }
        if (node instanceof DvcsRef ref) {
            return getInOutCounters(ref, myAffectedRepositories);
        }
        return DvcsInOutCountersInProject.EMPTY;
    }

    private @Nullable Instant getLastFetchTime() {
        for (DvcsRepositoryModel repository : myRepositories) {
            Instant lastFetchTime = repository.getLastFetchTime();
            if (lastFetchTime != null) {
                return lastFetchTime;
            }
        }
        return null;
    }

    public @Nullable String getText(@Nullable Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof DvcsRefType type) {
            if (mySelectedRepository != null) {
                return type.getInRepoText(mySelectedRepository.getShortName()).get();
            }
            else if (myRepositories.size() > 1) {
                return type.getCommonText().get();
            }
            else {
                return type.getText().get();
            }
        }
        if (node instanceof DvcsBranchesTreeModel.BranchesPrefixGroup group) {
            List<String> prefix = group.prefix();
            return prefix.get(prefix.size() - 1);
        }
        if (node instanceof DvcsBranchesTreeModel.RefTypeUnderRepository typeUnderRepository) {
            return typeUnderRepository.type().getText().get();
        }
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getRefText(refUnderRepository.ref(), myTreeModel.get().isDirectoryGrouping());
        }
        if (node instanceof DvcsRef ref) {
            return getRefText(ref, myTreeModel.get().isDirectoryGrouping());
        }
        if (node instanceof DvcsBranchesTreeModel.PresentableNode presentableNode) {
            return presentableNode.getPresentableText().getNullIfEmpty();
        }
        return null;
    }

    public TextAttribute getTextAttribute(@Nullable Object node) {
        return isCurrentRef(node) ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR;
    }

    public @Nullable Image getIcon(@Nullable Object node, boolean selected, boolean favoriteToggleOnClick) {
        if (node instanceof DvcsBranchesTreeModel.BranchesPrefixGroup) {
            return DvcsBranchesTreeIconProvider.forGroup();
        }
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getBranchIcon(refUnderRepository.ref(), List.of(refUnderRepository.repository()), selected, favoriteToggleOnClick);
        }
        if (node instanceof DvcsRef ref) {
            return getBranchIcon(ref, myAffectedRepositories, selected, favoriteToggleOnClick);
        }
        if (node instanceof DvcsBranchesTreeModel.RepositoryNode repositoryNode) {
            return repositoryNode.repository().getIcon();
        }
        return null;
    }

    public @Nullable String getSecondaryText(@Nullable Object node) {
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return getCommonTrackedBranchName(refUnderRepository.ref(), List.of(refUnderRepository.repository()));
        }
        if (node instanceof DvcsRef ref) {
            return getCommonTrackedBranchName(ref, myAffectedRepositories);
        }
        return null;
    }

    private boolean isCurrentRef(@Nullable Object node) {
        if (node instanceof DvcsBranchesTreeModel.RefUnderRepository refUnderRepository) {
            return allCurrent(refUnderRepository.ref(), List.of(refUnderRepository.repository()));
        }
        if (node instanceof DvcsRef ref) {
            return allCurrent(ref, myAffectedRepositories);
        }
        return false;
    }

    private static Image getBranchIcon(DvcsRef ref,
                                       List<DvcsRepositoryModel> repositories,
                                       boolean selected,
                                       boolean favoriteToggleOnClick) {
        boolean current = allCurrent(ref, repositories);
        boolean favorite = allFavorite(ref, repositories);
        return DvcsBranchesTreeIconProvider.forRef(ref, current, favorite, selected, favoriteToggleOnClick);
    }

    private static DvcsInOutCountersInProject getInOutCounters(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        Map<FilePath, DvcsInOutCountersInRepo> reposState = new LinkedHashMap<>();
        for (DvcsRepositoryModel repository : repositories) {
            DvcsInOutCountersInRepo counters = repository.getInOutCounters(ref);
            if (counters != null && (counters.hasIncoming() || counters.hasOutgoing())) {
                reposState.put(repository.getRoot(), counters);
            }
        }
        return reposState.isEmpty() ? DvcsInOutCountersInProject.EMPTY : new DvcsInOutCountersInProject(reposState);
    }

    private static @Nullable String getCommonTrackedBranchName(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        String commonTrackedBranch = null;
        for (DvcsRepositoryModel repository : repositories) {
            String trackedBranch = repository.getTrackedBranchName(ref);
            if (trackedBranch == null) {
                return null;
            }
            if (commonTrackedBranch == null) {
                commonTrackedBranch = trackedBranch;
            }
            else if (!commonTrackedBranch.equals(trackedBranch)) {
                return null;
            }
        }
        return commonTrackedBranch;
    }

    private static boolean allCurrent(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        if (repositories.isEmpty()) {
            return false;
        }
        for (DvcsRepositoryModel repository : repositories) {
            if (!repository.isCurrentRef(ref)) {
                return false;
            }
        }
        return true;
    }

    private static boolean allFavorite(DvcsRef ref, List<DvcsRepositoryModel> repositories) {
        if (repositories.isEmpty()) {
            return false;
        }
        for (DvcsRepositoryModel repository : repositories) {
            if (!repository.isFavorite(ref)) {
                return false;
            }
        }
        return true;
    }

    private static String getRefText(DvcsRef value, boolean prefixGrouping) {
        String name = value.getName();
        if (prefixGrouping) {
            int idx = name.lastIndexOf('/');
            return idx >= 0 ? name.substring(idx + 1) : name;
        }
        return name;
    }
}
