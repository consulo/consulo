// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff.model;

import com.intellij.collaboration.util.RefComparisonChange;
import com.intellij.collaboration.util.RefComparisonChangeKt;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;

@ApiStatus.Experimental
@FunctionalInterface
public interface RefComparisonChangesSorter {
    @Nonnull
    List<RefComparisonChange> sort(@Nonnull List<RefComparisonChange> changes);

    final class None implements RefComparisonChangesSorter {
        public static final None INSTANCE = new None();

        private None() {
        }

        @Override
        public @Nonnull List<RefComparisonChange> sort(@Nonnull List<RefComparisonChange> changes) {
            return changes;
        }
    }

    @ApiStatus.Experimental
    final class Grouping implements RefComparisonChangesSorter {
        private final @Nonnull Project project;
        private final @Nonnull Set<String> groupings;

        public Grouping(@Nonnull Project project, @Nonnull Set<String> groupings) {
            this.project = project;
            this.groupings = groupings;
        }

        // TODO: don't build the tree, implement a comparator
        @Override
        public @Nonnull List<RefComparisonChange> sort(@Nonnull List<RefComparisonChange> changes) {
            ChangesGroupingSupport groupingSupport = new ChangesGroupingSupport(project, Unit.INSTANCE, false);
            groupingSupport.setGroupingKeysOrSkip(groupings);
            var grouping = groupingSupport.getGrouping();
            TreeModelBuilder builder = new TreeModelBuilder(project, grouping);
            for (RefComparisonChange change : changes) {
                builder.insertChangeNode(RefComparisonChangeKt.getFilePath(change), builder.getMyRoot(), new Node(change));
            }
            var model = builder.build();
            return VcsTreeModelData.allUnder((ChangesBrowserNode<?>) model.getRoot())
                .iterateUserObjects(RefComparisonChange.class)
                .toList();
        }

        private static final class Node extends AbstractChangesBrowserFilePathNode<RefComparisonChange> {
            Node(@Nonnull RefComparisonChange change) {
                super(change, RefComparisonChangeKt.getFileStatus(change));
            }

            @Override
            protected @Nonnull FilePath filePath(@Nonnull RefComparisonChange userObject) {
                return RefComparisonChangeKt.getFilePath(userObject);
            }

            @Override
            protected @Nullable FilePath originPath(@Nonnull RefComparisonChange userObject) {
                return userObject.getFilePathBefore();
            }
        }
    }
}
