package consulo.language.editor.problemView;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;

@ServiceAPI(ComponentScope.PROJECT)
public interface ProblemsCollector extends ProblemsListener {
    @Nonnull
    static ProblemsCollector getInstance(@Nonnull Project project) {
        return project.getInstance(ProblemsCollector.class);
    }

    int getProblemCount();

    @Nonnull
    Collection<VirtualFile> getProblemFiles();

    int getFileProblemCount(@Nonnull VirtualFile file);

    @Nonnull
    Collection<Problem> getFileProblems(@Nonnull VirtualFile file);

    int getOtherProblemCount();

    @Nonnull
    Collection<Problem> getOtherProblems();
}
