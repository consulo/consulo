/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.codeEditor.action;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.codeEditor.util.SoftWrapUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides common functionality for <code>'toggle soft wraps usage'</code> actions.
 *
 * @author Denis Zhdanov
 * @since 2010-08-23
 */
public abstract class AbstractToggleUseSoftWrapsAction extends ToggleAction implements DumbAware {
    private final SoftWrapAppliancePlaces myAppliancePlace;
    private final boolean myGlobal;

    /**
     * Creates new <code>AbstractToggleUseSoftWrapsAction</code> object.
     *
     * @param appliancePlace defines type of the place where soft wraps are applied
     * @param global         indicates if soft wraps should be changed for the current editor only or for the all editors
     *                       used at the target appliance place
     */
    public AbstractToggleUseSoftWrapsAction(@Nonnull SoftWrapAppliancePlaces appliancePlace, boolean global) {
        myAppliancePlace = appliancePlace;
        myGlobal = global;
    }

    protected AbstractToggleUseSoftWrapsAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon,
        SoftWrapAppliancePlaces appliancePlace,
        boolean global
    ) {
        super(text, description, icon);
        myAppliancePlace = appliancePlace;
        myGlobal = global;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        if (myGlobal) {
            Editor editor = getEditor(e);
            if (editor != null && editor.getSettings().getSoftWrapAppliancePlace() != myAppliancePlace) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
        }
        super.update(e);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        if (myGlobal) {
            return PersistentEditorSettings.getInstance().isUseSoftWraps(myAppliancePlace);
        }
        Editor editor = getEditor(e);
        return editor != null && editor.getSettings().isUseSoftWraps();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        Editor editor = getEditor(e);
        if (editor == null) {
            return;
        }

        SoftWrapUtil.toggleSoftWraps(editor, myGlobal ? myAppliancePlace : null, state);
    }

    @Nullable
    protected Editor getEditor(AnActionEvent e) {
        return e.getData(Editor.KEY);
    }
}
