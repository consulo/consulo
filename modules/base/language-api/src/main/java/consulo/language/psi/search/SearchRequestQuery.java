// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.application.util.function.Processor;
import consulo.application.util.query.AbstractQuery;
import consulo.language.psi.PsiReference;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class SearchRequestQuery extends AbstractQuery<PsiReference> {
    private final Project myProject;
    private final SearchRequestCollector myRequests;

    public SearchRequestQuery(@Nonnull Project project, @Nonnull SearchRequestCollector requests) {
        myProject = project;
        myRequests = requests;
    }

    @Override
    protected boolean processResults(@Nonnull Processor<? super PsiReference> consumer) {
        return PsiSearchHelper.getInstance(myProject).processRequests(myRequests, consumer);
    }

    @Override
    public String toString() {
        return myRequests.toString();
    }
}
