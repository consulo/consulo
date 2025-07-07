/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.generation;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.language.editor.generation.PatternDescriptor;
import consulo.language.editor.generation.PatternProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ArrayUtil;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 */
@ActionImpl(id = "GeneratePattern")
public class GenerateByPatternAction extends AnAction {
    private final Application myApplication;

    @Inject
    public GenerateByPatternAction(Application application) {
        super(LocalizeValue.localizeTODO("Generate by Pattern..."));
        myApplication = application;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        ExtensionPoint<PatternProvider> point = myApplication.getExtensionPoint(PatternProvider.class);
        if (!point.hasAnyExtensions()) {
            e.getPresentation().setVisible(false);
            return;
        }

        e.getPresentation().setVisible(true);
        for (PatternProvider extension : point.getExtensionList()) {
            if (extension.isAvailable(e.getDataContext())) {
                return;
            }
        }
        e.getPresentation().setVisible(false);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        PatternDescriptor[] patterns = new PatternDescriptor[0];
        ExtensionPoint<PatternProvider> point = myApplication.getExtensionPoint(PatternProvider.class);
        for (PatternProvider extension : point.getExtensionList()) {
            if (extension.isAvailable(e.getDataContext())) {
                patterns = ArrayUtil.mergeArrays(patterns, extension.getDescriptors());
            }
        }
        GenerateByPatternDialog dialog = new GenerateByPatternDialog(e.getData(Project.KEY), patterns, e.getDataContext());
        dialog.show();
        if (dialog.isOK()) {
            dialog.getSelectedDescriptor().actionPerformed(e.getDataContext());
        }
    }
}
