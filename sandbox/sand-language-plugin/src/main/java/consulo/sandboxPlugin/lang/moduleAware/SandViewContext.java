package consulo.sandboxPlugin.lang.moduleAware;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.navigation.NavigationContext;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.sandboxPlugin.lang.psi.SandElements;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SandViewContext(Set<String> environment, VirtualFile navigationSource) implements NavigationContext {
    @Override
    public VirtualFile getNavigationSource() {
        return navigationSource;
    }

    @Override
    public LocalizeValue getPresentableText() {
        List<String> sorted = new ArrayList<>(environment);
        sorted.sort(null);
        return LocalizeValue.of(sorted.isEmpty() ? "<no flags>" : String.join(", ", sorted));
    }

    @RequiredReadAction
    @Override
    public @Nullable Map<String, IndexOption> getViewOptions(Project project, VirtualFile file) {
        IndexOption option = SandSeedEnv.optionsSeenFrom(project, navigationSource, file);
        return option == null ? null : Map.of(SandModuleAwareIndexOptionProvider.ID, option);
    }

    @RequiredReadAction
    @Override
    public boolean isEffectiveFor(Project project, VirtualFile file) {
        if (file.getFileType() != SandFileType.INSTANCE || environment.equals(SandFlagEnv.moduleFlags(project, file))) {
            return false;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return psiFile != null
            && (psiFile.getNode().findChildByType(SandElements.IF_DIRECTIVE) != null
            || psiFile.getNode().findChildByType(SandElements.IFNDEF_DIRECTIVE) != null);
    }
}
