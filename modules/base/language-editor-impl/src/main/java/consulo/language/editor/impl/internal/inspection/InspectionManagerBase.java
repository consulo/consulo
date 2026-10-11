/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.language.editor.impl.internal.inspection.setting.InspectionToolsConfigurable;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.internal.InspectionCache;
import consulo.language.editor.internal.InspectionCacheService;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public abstract class InspectionManagerBase extends InspectionManager {
    private static final Logger LOG = Logger.getInstance(InspectionManagerBase.class);

    private static final Pattern HTML_PATTERN = Pattern.compile("<[^<>]*>");

    private final AtomicBoolean myToolsAreInitialized = new AtomicBoolean(false);

    private final Project myProject;
    protected String myCurrentProfileName;

    public InspectionManagerBase(Project project) {
        myProject = project;
    }

    
    @Override
    @RequiredReadAction
    public List<ProblemDescriptor> runLocalToolLocaly(LocalInspectionTool tool, PsiFile file, Object state) {
        ProblemsHolder holder = new ProblemsHolderImpl(this, file, false);
        LocalInspectionToolSession session = new LocalInspectionToolSession(file, 0, file.getTextLength());
        final PsiElementVisitor customVisitor = tool.buildVisitor(holder, false, session, state);
        LOG.assertTrue(!(customVisitor instanceof PsiRecursiveVisitor),
            "The visitor returned from LocalInspectionTool.buildVisitor() must not be recursive: " + customVisitor);

        tool.inspectionStarted(session, false, state);

        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.accept(customVisitor);
                super.visitElement(element);
            }
        });

        tool.inspectionFinished(session, holder, state);

        return holder.getResults();
    }

    
    @Override
    public ProblemsHolder createProblemsHolder(PsiFile file, boolean onTheFly) {
        return new ProblemsHolderImpl(this, file, onTheFly);
    }

    
    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public ProblemDescriptorBuilder newProblemDescriptor(LocalizeValue descriptionTemplate) {
        return new ProblemDescriptorBuilderImpl(descriptionTemplate);
    }

    @Override
    
    public CommonProblemDescriptor createProblemDescriptor(String descriptionTemplate, QuickFix... fixes) {
        return new CommonProblemDescriptorBase(fixes, LocalizeValue.of(descriptionTemplate));
    }

    @Override
    public ModuleProblemDescriptor createProblemDescriptor(
        String descriptionTemplate,
        Module module,
        QuickFix<?>... fixes
    ) {
        return new ModuleProblemDescriptorImpl(module, LocalizeValue.of(descriptionTemplate), fixes);
    }

    @Override
    public String getCurrentProfile() {
        if (myCurrentProfileName == null) {
            InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(getProject());
            myCurrentProfileName = profileManager.getProjectProfile();
            if (myCurrentProfileName == null) {
                myCurrentProfileName = InspectionProfileManager.getInstance().getRootProfile().getName();
            }
        }
        return myCurrentProfileName;
    }

    public void buildInspectionSearchIndexIfNecessary() {
        if (myToolsAreInitialized.getAndSet(true)) {
            return;
        }

        Application application = myProject.getApplication();
        if (application.isUnitTestMode() || application.isHeadlessEnvironment()) {
            return;
        }

        application.executeOnPooledThread(() -> {
            SearchableOptionsRegistrar optionsRegistrar = SearchableOptionsRegistrar.getInstance();

            InspectionCache cache = application.getInstance(InspectionCacheService.class).get();
            for (InspectionToolWrapper toolWrapper : cache.wrapTools()) {
                processText(application, toolWrapper.getDisplayName().toLowerCase().get(), toolWrapper, optionsRegistrar);

                String description = toolWrapper.loadDescription();
                if (description != null) {
                    String descriptionText = HTML_PATTERN.matcher(description).replaceAll(" ");
                    processText(application, descriptionText, toolWrapper, optionsRegistrar);
                }
            }
        });
    }

    private static void processText(
        Application application,
        String text,
        InspectionToolWrapper tool,
        SearchableOptionsRegistrar optionsRegistrar
    ) {
        if (application.isDisposed()) {
            return;
        }

        Set<String> words = optionsRegistrar.getProcessedWordsWithoutStemming(text);
        for (String word : words) {
            optionsRegistrar.addOption(word, tool.getShortName(), tool.getDisplayName().get(), InspectionToolsConfigurable.ID, "Inspections");
        }
    }
}
