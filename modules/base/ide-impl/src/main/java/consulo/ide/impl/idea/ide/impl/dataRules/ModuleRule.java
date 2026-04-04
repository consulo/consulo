package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.dataContext.DataSnapshot;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public final class ModuleRule {
    static @Nullable Module getData(DataSnapshot dataProvider) {
        Module moduleContext = dataProvider.get(LangDataKeys.MODULE_CONTEXT);
        if (moduleContext != null) {
            return moduleContext;
        }
        Project project = dataProvider.get(Project.KEY);
        if (project == null) {
            PsiElement element = dataProvider.get(PsiElement.KEY);
            if (element == null || !element.isValid()) return null;
            project = element.getProject();
        }

        VirtualFile virtualFile = dataProvider.get(VirtualFile.KEY);
        if (virtualFile == null) {
            virtualFile = VirtualFileRule.getData(dataProvider);
        }

        if (virtualFile == null) {
            return null;
        }

        return ModuleUtil.findModuleForFile(virtualFile, project);
    }
}
