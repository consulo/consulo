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
package consulo.ide.impl.idea.ide.actions;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.BasePresentationFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author peter
 */
public abstract class WeighingActionGroup extends ActionGroup {
    public static final Key<Double> WEIGHT_KEY = Key.create("WeighingActionGroup.WEIGHT");

    public static final double DEFAULT_WEIGHT = 0;
    public static final double HIGHER_WEIGHT = 42;
    public static final double EVEN_HIGHER_WEIGHT = 239;

    private final BasePresentationFactory myPresentationFactory = new BasePresentationFactory();

    @Override
    public void update(@Nonnull AnActionEvent e) {
        getDelegate().update(e);
    }

    protected abstract ActionGroup getDelegate();

    @Override
    @Nonnull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return getDelegate().getChildren(e);
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public List<AnAction> postProcessVisibleChildren(@Nonnull List<AnAction> visibleActions) {
        Set<AnAction> heaviest = null;
        double maxWeight = DEFAULT_WEIGHT;

        for (AnAction action : visibleActions) {
            Presentation presentation = myPresentationFactory.getPresentation(action);
            if (presentation.isEnabled() && presentation.isVisible()) {
                double weight = Objects.requireNonNullElse(presentation.getClientProperty(WEIGHT_KEY), DEFAULT_WEIGHT);
                if (weight > maxWeight) {
                    maxWeight = weight;
                    heaviest = new LinkedHashSet<>();
                }

                if (weight == maxWeight && heaviest != null) {
                    heaviest.add(action);
                }
            }
        }

        if (heaviest == null) {
            return visibleActions;
        }

        ActionGroup.Builder chosen = ActionGroup.newImmutableBuilder();
        boolean prevSeparator = true;
        for (AnAction action : visibleActions) {
            boolean separator = action instanceof AnSeparator;
            if (separator && !prevSeparator) {
                chosen.add(action);
            }
            prevSeparator = separator;

            if (shouldBeChosenAnyway(action)) {
                heaviest.add(action);
            }

            if (heaviest.contains(action)) {
                chosen.add(action);
            }
        }

        ActionGroup other = new ExcludingActionGroup(getDelegate(), heaviest);
        other.setPopup(true);
        other.getTemplatePresentation().setTextValue(LocalizeValue.localizeTODO("Other..."));
        return List.of(chosen.build(), AnSeparator.getInstance(), other);
    }

    protected boolean shouldBeChosenAnyway(AnAction action) {
        return false;
    }
}
