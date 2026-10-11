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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class UnifiedOptionsTreeModel implements TreeModel<Configurable> {
    private final Supplier<Configurable[]> myConfigurables;
    private final Predicate<Configurable> myFilter;
    private final Function<Configurable, @Nullable LocalizeValue> myProjectTooltip;

    public UnifiedOptionsTreeModel(
        Supplier<Configurable[]> configurables,
        Predicate<Configurable> filter,
        Function<Configurable, @Nullable LocalizeValue> projectTooltip
    ) {
        myConfigurables = configurables;
        myFilter = filter;
        myProjectTooltip = projectTooltip;
    }

    @Override
    public void buildChildren(Function<Configurable, TreeNode<Configurable>> nodeFactory, @Nullable Configurable parentValue) {
        boolean topLevel = parentValue == null;
        Configurable[] configurables = parentValue == null ? myConfigurables.get() : getKids(parentValue);

        for (Configurable configurable : flatten(configurables)) {
            if (!isShown(configurable)) {
                continue;
            }

            TreeNode<Configurable> node = nodeFactory.apply(configurable);
            node.setLeaf(!hasShownKids(configurable));

            LocalizeValue projectTooltip = myProjectTooltip.apply(configurable);
            node.setRenderer((value, presentation) -> render(value, presentation, topLevel, projectTooltip));
        }
    }

    @Override
    public @Nullable Comparator<TreeNode<Configurable>> getNodeComparator() {
        return UnifiedConfigurableComparator.INSTANCE;
    }

    private boolean isShown(Configurable configurable) {
        return myFilter.test(configurable) || hasShownKids(configurable);
    }

    private boolean hasShownKids(Configurable configurable) {
        for (Configurable kid : flatten(getKids(configurable))) {
            if (isShown(kid)) {
                return true;
            }
        }
        return false;
    }

    private static void render(
        Configurable configurable,
        TextItemPresentation presentation,
        boolean topLevel,
        @Nullable LocalizeValue projectTooltip
    ) {
        presentation.append(configurable.getDisplayName(), topLevel ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR);

        if (projectTooltip != null) {
            presentation.withSuffix(LocalizeValue.empty(), PlatformIconGroup.generalProjectconfigurable());
            presentation.withTooltip(projectTooltip);
        }
    }

    public static Configurable[] getKids(Configurable configurable) {
        return configurable instanceof Configurable.Composite composite ? composite.getConfigurables() : Configurable.EMPTY_ARRAY;
    }

    public static List<Configurable> flatten(Configurable[] configurables) {
        List<Configurable> result = new ArrayList<>(configurables.length);
        for (Configurable configurable : configurables) {
            if (isInvisible(configurable)) {
                result.addAll(flatten(getKids(configurable)));
            }
            else {
                result.add(configurable);
            }
        }
        return result;
    }

    public static boolean isInvisible(Configurable configurable) {
        return configurable instanceof SearchableConfigurable.Parent parent && !parent.isVisible();
    }
}
