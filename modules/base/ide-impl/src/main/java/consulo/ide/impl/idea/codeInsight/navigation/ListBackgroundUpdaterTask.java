/*
 * Copyright 2000-2019 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.navigation;

import consulo.language.editor.ui.navigation.BackgroundUpdaterTask;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.ui.JBListUpdater;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.lang.ref.Ref;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.JBList;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.usage.UsageView;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

import java.util.Comparator;

/**
 * @deprecated please use {@link BackgroundUpdaterTask}
 */
@Deprecated
public abstract class ListBackgroundUpdaterTask extends BackgroundUpdaterTask {
    protected AbstractPopup myPopup;

    /**
     * @deprecated Use {@link #ListBackgroundUpdaterTask(Project, String, Comparator)}
     */
    @Deprecated
    public ListBackgroundUpdaterTask(@Nullable Project project, @Nonnull String title) {
        this(project, title, null);
    }

    public ListBackgroundUpdaterTask(
        @Nullable Project project,
        @Nonnull String title,
        @Nullable Comparator<PsiElement> comparator
    ) {
        super(project, title, comparator);
    }

    /**
     * @deprecated please use {@link BackgroundUpdaterTask}
     */
    @Deprecated
    public void init(@Nonnull AbstractPopup popup, @Nonnull Object component, @Nonnull SimpleReference<UsageView> usageView) {
        myPopup = popup;
        if (component instanceof JBList) {
            init((JBPopup)myPopup, new JBListUpdater((JBList)component), usageView);
        }
    }
}
