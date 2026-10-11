/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.roots.ui.configuration.projectRoot.moduleLayerActions;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2014-07-30
 */
public class NewLayerAction extends DumbAwareAction {
    private final ModuleEditor myModuleEditor;
    private final boolean myCopy;
    private final Supplier<@Nullable Component> myParent;

    public NewLayerAction(ModuleEditor moduleEditor, boolean copy, Supplier<@Nullable Component> parent) {
        super(
            copy ? ProjectLocalize.moduleLayerCopyAction() : ProjectLocalize.moduleLayerNewAction(),
            LocalizeValue.empty(),
            copy ? PlatformIconGroup.actionsCopy() : PlatformIconGroup.generalAdd()
        );
        myModuleEditor = moduleEditor;
        myCopy = copy;
        myParent = parent;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        ModifiableRootModel modifiableRootModel = myModuleEditor.getModifiableRootModelProxy();
        String copyName = myCopy ? modifiableRootModel.getCurrentLayerName() : null;

        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(myCopy ? ProjectLocalize.moduleLayerCopyAction() : ProjectLocalize.moduleLayerNewAction())
            .text(ProjectLocalize.moduleLayerNamePrompt())
            .value(createUniqueLayerName(copyName, modifiableRootModel))
            .validator(name -> {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) {
                    return InputProblem.error(ProjectLocalize.moduleLayerNameEmptyError());
                }
                if (modifiableRootModel.findLayerByName(trimmed) != null) {
                    return InputProblem.error(ProjectLocalize.moduleLayerNameAlreadyExistsError(trimmed));
                }
                return null;
            })
            .showAsync(myParent.get())
            .whenCompleteAsync((newName, error) -> {
                if (error != null || newName == null) {
                    return;
                }
                modifiableRootModel.addLayer(newName.trim(), copyName, true);
                String moduleDirUrl = modifiableRootModel.getModule().getModuleDirUrl();
                if (moduleDirUrl != null) {
                    modifiableRootModel.addContentEntry(moduleDirUrl);
                }
             }, uiAccess);
    }

    private static String createUniqueLayerName(@Nullable String suggestedName, ModifiableRootModel modifiableRootModel) {
        String baseName = suggestedName == null ? ModifiableRootModel.DEFAULT_LAYER_NAME : suggestedName;
        Set<String> names = modifiableRootModel.getLayers().keySet();
        String newName = baseName;
        int i = 0;
        while (names.contains(newName)) {
            newName = baseName + (++i);
        }
        return newName;
    }
}
