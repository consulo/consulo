package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.util.collection.SmartList;
import consulo.ide.impl.idea.util.StringBuilderSpinAllocator;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ProjectStructureProblemsHolderImpl implements ProjectStructureProblemsHolder {
    private List<ProjectStructureProblemDescription> myProblemDescriptions;

    @Override
    public void registerProblem(
        @Nonnull String message, @Nullable String description,
        @Nonnull ProjectStructureProblemType problemType,
        @Nonnull PlaceInProjectStructure place,
        @Nullable ConfigurationErrorQuickFix fix
    ) {
        List<ConfigurationErrorQuickFix> fixes =
            fix != null ? Collections.singletonList(fix) : Collections.<ConfigurationErrorQuickFix>emptyList();
        registerProblem(new ProjectStructureProblemDescription(message, description, place, problemType, fixes));
    }

    @Override
    public void registerProblem(@Nonnull ProjectStructureProblemDescription description) {
        if (myProblemDescriptions == null) {
            myProblemDescriptions = new SmartList<>();
        }
        myProblemDescriptions.add(description);
    }

    public String composeTooltipMessage() {
        StringBuilder buf = StringBuilderSpinAllocator.alloc();
        try {
            buf.append("<html><body>");
            if (myProblemDescriptions != null) {
                int problems = 0;
                for (ProjectStructureProblemDescription problemDescription : myProblemDescriptions) {
                    XmlStringUtil.escapeText(problemDescription.getMessage(false), buf);
                    buf.append("<br/>");
                    problems++;
                    if (problems >= 10 && myProblemDescriptions.size() > 12) {
                        buf.append(myProblemDescriptions.size() - problems).append(" more problems...<br>");
                        break;
                    }
                }
            }
            buf.append("</body></html>");
            return buf.toString();
        }
        finally {
            StringBuilderSpinAllocator.dispose(buf);
        }
    }

    public boolean containsProblems() {
        return myProblemDescriptions != null && !myProblemDescriptions.isEmpty();
    }

    public boolean containsProblems(ProjectStructureProblemType.Severity severity) {
        if (myProblemDescriptions != null) {
            for (ProjectStructureProblemDescription description : myProblemDescriptions) {
                if (description.getSeverity() == severity) {
                    return true;
                }
            }
        }
        return false;
    }

    public void removeProblem(@Nonnull ProjectStructureProblemDescription description) {
        if (myProblemDescriptions != null) {
            myProblemDescriptions.remove(description);
        }
    }

    @Nullable
    public List<ProjectStructureProblemDescription> getProblemDescriptions() {
        return myProblemDescriptions;
    }
}
