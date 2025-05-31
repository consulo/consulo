// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.project.ui.view.impl.internal.nesting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.extension.ExtensionPointName;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.util.ModificationTracker;
import consulo.logging.Logger;
import consulo.project.ui.view.tree.ProjectViewNestingRulesProvider;
import consulo.util.collection.Lists;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Holds data used by {@link NestingTreeStructureProvider} and {@link FileNestingInProjectViewDialog}.
 */
@State(
    name = "ProjectViewFileNesting",
    storages = @Storage("ui.lnf.xml")
)
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public final class ProjectViewFileNestingService implements PersistentStateComponent<ProjectViewFileNestingService.MyState>, ModificationTracker {
    private static final Logger LOG = Logger.getInstance(ProjectViewFileNestingService.class);

    private static final ExtensionPointName<ProjectViewNestingRulesProvider> EP_NAME =
        ExtensionPointName.create(ProjectViewNestingRulesProvider.class);

    private MyState myState = new MyState();
    private long myModCount;

    public static @Nonnull ProjectViewFileNestingService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectViewFileNestingService.class);
    }

    static @Nonnull List<NestingRule> loadDefaultNestingRules() {
        List<NestingRule> result = new ArrayList<>();

        final ProjectViewNestingRulesProvider.Consumer consumer = (parentFileSuffix, childFileSuffix) -> {
            LOG.assertTrue(!parentFileSuffix.isEmpty() && !childFileSuffix.isEmpty(), "file suffix must not be empty");
            LOG.assertTrue(!parentFileSuffix.equals(childFileSuffix), "parent and child suffixes must be different: " + parentFileSuffix);
            result.add(new NestingRule(parentFileSuffix, childFileSuffix));
        };

        for (ProjectViewNestingRulesProvider provider : EP_NAME.getExtensionList()) {
            provider.addFileNestingRules(consumer);
        }

        return result;
    }

    @Override
    public MyState getState() {
        return myState;
    }

    @Override
    public void loadState(final @Nonnull MyState state) {
        myState = state;
        myModCount++;
    }

    /**
     * This list of rules is used for serialization and for UI.
     * See also {@link NestingTreeStructureProvider}, it adjusts this list of rules to match its needs
     */
    public @Nonnull List<NestingRule> getRules() {
        return myState.myRules;
    }

    public void setRules(final @Nonnull List<NestingRule> rules) {
        myState.myRules.clear();
        myState.myRules.addAll(rules);
        myModCount++;
    }

    @Override
    public long getModificationCount() {
        return myModCount;
    }

    public static final class MyState {
        @AbstractCollection
        public List<NestingRule> myRules = Lists.newSortedList(Comparator.comparing(o -> o.getParentFileSuffix()));

        public MyState() {
            myRules.addAll(loadDefaultNestingRules());
        }
    }

    public static final class NestingRule {
        private @Nonnull String myParentFileSuffix;

        private @Nonnull String myChildFileSuffix;

        @SuppressWarnings("unused") // used by serializer
        public NestingRule() {
            this("", "");
        }

        public NestingRule(@Nonnull String parentFileSuffix, @Nonnull String childFileSuffix) {
            myParentFileSuffix = parentFileSuffix;
            myChildFileSuffix = childFileSuffix;
        }

        @Attribute("parent-file-suffix")
        public @Nonnull String getParentFileSuffix() {
            return myParentFileSuffix;
        }

        public void setParentFileSuffix(final @Nonnull String parentFileSuffix) {
            myParentFileSuffix = parentFileSuffix;
        }

        @Attribute("child-file-suffix")
        public @Nonnull String getChildFileSuffix() {
            return myChildFileSuffix;
        }

        public void setChildFileSuffix(final @Nonnull String childFileSuffix) {
            myChildFileSuffix = childFileSuffix;
        }

        @Override
        public String toString() {
            return myParentFileSuffix + "->" + myChildFileSuffix;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof NestingRule &&
                myParentFileSuffix.equals(((NestingRule) o).myParentFileSuffix) &&
                myChildFileSuffix.equals(((NestingRule) o).myChildFileSuffix);
        }

        @Override
        public int hashCode() {
            return myParentFileSuffix.hashCode() + 239 * myChildFileSuffix.hashCode();
        }
    }
}
