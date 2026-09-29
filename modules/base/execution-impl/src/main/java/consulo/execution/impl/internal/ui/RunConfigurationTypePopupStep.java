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
package consulo.execution.impl.internal.ui;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.impl.internal.configuration.ConfigurationTypeSelector;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-09-29
 */
final class RunConfigurationTypePopupStep extends BaseListPopupStep<ConfigurationType> {
    private static final ConfigurationType HIDDEN_ITEMS_STUB = new ConfigurationType() {
        @Override
        public LocalizeValue getDisplayName() {
            return LocalizeValue.empty();
        }

        @Override
        public Image getIcon() {
            return Image.empty(Image.DEFAULT_ICON_SIZE);
        }

        @Override
        public String getId() {
            return "";
        }

        @Override
        public ConfigurationFactory[] getConfigurationFactories() {
            return new ConfigurationFactory[0];
        }
    };

    private final List<ConfigurationType> myConfigurationTypes;
    private final int myHiddenCount;
    private final @Nullable ConfigurationType mySelectedType;
    private final Consumer<ConfigurationFactory> myFactoryChosen;
    private final Runnable myShowAllTypes;

    private RunConfigurationTypePopupStep(
        List<ConfigurationType> configurationTypes,
        int hiddenCount,
        @Nullable ConfigurationType selectedType,
        Consumer<ConfigurationFactory> factoryChosen,
        Runnable showAllTypes
    ) {
        super(ExecutionLocalize.addNewRunConfigurationAction2Name().get(), configurationTypes);
        myConfigurationTypes = configurationTypes;
        myHiddenCount = hiddenCount;
        mySelectedType = selectedType;
        myFactoryChosen = factoryChosen;
        myShowAllTypes = showAllTypes;
    }

    static RunConfigurationTypePopupStep create(
        Project project,
        RunManagerImpl runManager,
        boolean showApplicableTypesOnly,
        @Nullable ConfigurationType selectedType,
        Consumer<ConfigurationFactory> factoryChosen,
        Runnable showAllTypes
    ) {
        List<ConfigurationType> allTypes = runManager.getConfigurationFactories(false);
        List<ConfigurationType> configurationTypes = ConfigurationTypeSelector.getTypesToShow(project, showApplicableTypesOnly, allTypes);
        Collections.sort(configurationTypes, ConfigurationType.DISPLAY_NAME_COMPARATOR);
        int hiddenCount = allTypes.size() - configurationTypes.size();
        if (hiddenCount > 0) {
            configurationTypes.add(HIDDEN_ITEMS_STUB);
        }
        return new RunConfigurationTypePopupStep(configurationTypes, hiddenCount, selectedType, factoryChosen, showAllTypes);
    }

    @Override
    public String getTextFor(ConfigurationType type) {
        if (type == HIDDEN_ITEMS_STUB) {
            return myHiddenCount + " items more (irrelevant)...";
        }
        return type.getDisplayName().get();
    }

    @Override
    public boolean isSpeedSearchEnabled() {
        return true;
    }

    @Override
    public boolean canBeHidden(ConfigurationType value) {
        return true;
    }

    @Override
    public Image getIconFor(ConfigurationType type) {
        return type.getIcon();
    }

    @Override
    public PopupStep onChosen(ConfigurationType type, boolean finalChoice) {
        if (hasSubstep(type)) {
            return getSupStep(type);
        }
        if (type == HIDDEN_ITEMS_STUB) {
            return doFinalStep(myShowAllTypes);
        }

        ConfigurationFactory[] factories = type.getConfigurationFactories();
        if (factories.length > 0) {
            myFactoryChosen.accept(factories[0]);
        }
        return FINAL_CHOICE;
    }

    @Override
    public int getDefaultOptionIndex() {
        return mySelectedType != null ? myConfigurationTypes.indexOf(mySelectedType) : super.getDefaultOptionIndex();
    }

    private ListPopupStep getSupStep(ConfigurationType type) {
        ConfigurationFactory[] factories = type.getConfigurationFactories();
        Arrays.sort(factories, ConfigurationFactory.DISPLAY_NAME_COMPARATOR);
        return new BaseListPopupStep<ConfigurationFactory>(
            ExecutionLocalize.addNewRunConfigurationActionName(type.getDisplayName()).get(),
            factories
        ) {
            @Override
            public String getTextFor(ConfigurationFactory value) {
                return value.getDisplayName().get();
            }

            @Override
            public Image getIconFor(ConfigurationFactory factory) {
                return factory.getIcon();
            }

            @Override
            public PopupStep onChosen(ConfigurationFactory factory, boolean finalChoice) {
                myFactoryChosen.accept(factory);
                return FINAL_CHOICE;
            }
        };
    }

    @Override
    public boolean hasSubstep(ConfigurationType type) {
        return type.getConfigurationFactories().length > 1;
    }
}
