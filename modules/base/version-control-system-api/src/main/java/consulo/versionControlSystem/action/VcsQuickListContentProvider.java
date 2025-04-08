package consulo.versionControlSystem.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.ex.action.AnAction;
import consulo.dataContext.DataContext;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Roman.Chernyatchik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface VcsQuickListContentProvider {
    ExtensionPointName<VcsQuickListContentProvider> EP_NAME = ExtensionPointName.create(VcsQuickListContentProvider.class);

    /**
     * Allows to customise VCS actions popup for both custom VCS and general list
     *
     * @param project     Project
     * @param activeVcs   Active vcs for current file. Null if context doesn't contain file or vcs is unknown
     * @param dataContext Context
     * @return actions list or null if do nothing
     */
    @Nullable
    List<AnAction> getVcsActions(@Nullable Project project, @Nullable AbstractVcs activeVcs, @Nullable DataContext dataContext);

    /**
     * Allows to customise VCS actions popup if project isn't in VCS
     *
     * @param project     Project
     * @param dataContext Context
     * @return actions list or null if do nothing
     */
    @Nullable
    List<AnAction> getNotInVcsActions(@Nullable Project project, @Nullable DataContext dataContext);

    /**
     * @param activeVcs   Active vcs for current file
     * @param dataContext Context
     * @return True if replace general actions with actions specified in getVcsActions() method. Otherwise
     * custom actions will be inserted in general popup. Usually should be false.
     */
    boolean replaceVcsActionsFor(@Nonnull AbstractVcs activeVcs, @Nullable DataContext dataContext);
}
