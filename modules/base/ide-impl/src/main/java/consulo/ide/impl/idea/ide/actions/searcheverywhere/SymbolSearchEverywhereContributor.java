// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.ide.impl.idea.ide.util.gotoByName.FilteringGotoByModel;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoSymbolModel2;
import consulo.ide.localize.IdeLocalize;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class SymbolSearchEverywhereContributor extends AbstractGotoSEContributor {
    private final PersistentSearchEverywhereContributorFilter<Language> myFilter;

    public SymbolSearchEverywhereContributor(@Nullable Project project, @Nullable PsiElement context) {
        super(project, context);
        myFilter = project == null ? null : ClassSearchEverywhereContributor.createLanguageFilter(project);
    }

    @Nonnull
    @Override
    public String getGroupName() {
        return "Symbols";
    }

    @Nonnull
    public LocalizeValue includeNonProjectItemsText() {
        return IdeLocalize.checkboxIncludeNonProjectSymbols();
    }

    @Override
    public int getSortWeight() {
        return 300;
    }

    @Nonnull
    @Override
    protected FilteringGotoByModel<Language> createModel(@Nonnull Project project) {
        GotoSymbolModel2 model = new GotoSymbolModel2(project);
        if (myFilter != null) {
            model.setFilterItems(myFilter.getSelectedElements());
        }
        return model;
    }

    @Nonnull
    @Override
    public List<AnAction> getActions(@Nonnull Runnable onChanged) {
        return doGetActions(includeNonProjectItemsText(), myFilter, onChanged);
    }
}
