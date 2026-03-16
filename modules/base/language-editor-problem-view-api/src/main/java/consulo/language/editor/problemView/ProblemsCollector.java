package consulo.language.editor.problemView;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;

@ServiceAPI(ComponentScope.PROJECT)
public interface ProblemsCollector extends ProblemsListener {
    
    static ProblemsCollector getInstance(Project project) {
        return project.getInstance(ProblemsCollector.class);
    }

    int getProblemCount();

    
    Collection<VirtualFile> getProblemFiles();

    int getFileProblemCount(VirtualFile file);

    
    Collection<Problem> getFileProblems(VirtualFile file);

    int getOtherProblemCount();

    
    Collection<Problem> getOtherProblems();
}
