/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.roots.ui.configuration.classpath;

import consulo.ide.setting.module.AddModuleDependencyActionProvider;
import consulo.ide.setting.module.AddModuleDependencyContext;
import consulo.logging.Logger;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author VISTALL
 * @since 2020-09-05
 */
public class AddModuleDependencyListPopupStep extends BaseListPopupStep<Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext>> {
    private static final Logger LOG = Logger.getInstance(AddModuleDependencyListPopupStep.class);

    private final ModifiableRootModel myRootModel;

    public AddModuleDependencyListPopupStep(
        ModifiableRootModel rootModel,
        Set<Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext>> values
    ) {
        super(ProjectLocalize.classpathAddDependencyPopupTitle().get(), new ArrayList<>(values));
        myRootModel = rootModel;
    }

    @Override
    public String getTextFor(Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> value) {
        return value.getKey().getActionName(myRootModel).get();
    }

    @Override
    public Image getIconFor(Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> value) {
        return value.getKey().getIcon(myRootModel);
    }

    @Override
    public PopupStep onChosen(Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> selectedValue, boolean finalChoice) {
        return doFinalStep(() -> providerSelected(selectedValue));
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public static void providerSelected(Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> entry) {
        AddModuleDependencyActionProvider provider = entry.getKey();
        AddModuleDependencyContext context = entry.getValue();

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<?> result = provider.invoke(context);
        result.whenCompleteAsync((value, error) -> {
            if (error != null) {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                if (!(cause instanceof CancellationException)) {
                    LOG.error(cause);
                }
                return;
            }

            if (value != null) {
                context.processAddOrderEntries(value);
            }
        }, uiAccess);
    }
}
