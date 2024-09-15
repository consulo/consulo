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

package consulo.language.editor.template;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.editor.template.event.TemplateEditingListener;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.function.PairProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class TemplateManager {
    public static TemplateManager getInstance(Project project) {
        return project.getInstance(TemplateManager.class);
    }

    public abstract void startTemplate(@Nonnull Editor editor, @Nonnull Template template);

    public abstract void startTemplate(@Nonnull Editor editor, String selectionString, @Nonnull Template template);

    public abstract void startTemplate(@Nonnull Editor editor, @Nonnull Template template, TemplateEditingListener listener);

    public abstract void startTemplate(@Nonnull final Editor editor,
                                       @Nonnull final Template template,
                                       boolean inSeparateCommand,
                                       Map<String, String> predefinedVarValues,
                                       @Nullable TemplateEditingListener listener);

    public abstract void startTemplate(@Nonnull Editor editor, @Nonnull Template template, TemplateEditingListener listener, final BiPredicate<String, String> callback);

    public abstract boolean startTemplate(@Nonnull Editor editor, char shortcutChar);

    @Deprecated
    @DeprecationInfo("use TemplateBuilderFactory")
    public Template createTemplate(@Nonnull String key, String group) {
        return TemplateBuilderFactory.getInstance().createRawTemplate(key, group);
    }

    @Deprecated
    @DeprecationInfo("use TemplateBuilderFactory")
    public Template createTemplate(@Nonnull String key, String group, String text) {
        return TemplateBuilderFactory.getInstance().createRawTemplate(key, group, text);
    }

    @Nullable
    public abstract Template getActiveTemplate(@Nonnull Editor editor);

    /**
     * Finished a live template in the given editor, if it's present
     *
     * @return whether a live template was present
     */
    public abstract boolean finishTemplate(@Nonnull Editor editor);

    @Nullable
    public abstract TemplateState getTemplateState(@Nonnull Editor editor);

    public abstract boolean isApplicable(Template template, Set<TemplateContextType> contextTypes);

    public abstract List<? extends Template> listApplicableTemplates(@Nonnull TemplateActionContext templateActionContext);

    public abstract List<? extends Template> listApplicableTemplateWithInsertingDummyIdentifier(@Nonnull TemplateActionContext templateActionContext);

    @RequiredReadAction
    @Nonnull
    public abstract Set<TemplateContextType> getApplicableContextTypes(@Nonnull TemplateActionContext templateActionContext);

    public abstract Map<Template, String> findMatchingTemplates(final PsiFile file, Editor editor, @Nullable Character shortcutChar, TemplateSettings templateSettings);

    @Nullable
    public abstract Runnable startNonCustomTemplates(final Map<Template, String> template2argument, final Editor editor, @Nullable final PairProcessor<String, String> processor);

    public boolean isApplicable(Template template, @Nonnull TemplateActionContext templateActionContext) {
        return isApplicable(template, getApplicableContextTypes(templateActionContext));
    }

    /**
     * @deprecated use {@link #isApplicable(Template, TemplateActionContext)}
     */
    @Deprecated(forRemoval = true)
    public boolean isApplicable(PsiFile file, int offset, Template template) {
        return isApplicable(template, TemplateActionContext.expanding(file, offset));
    }
}
