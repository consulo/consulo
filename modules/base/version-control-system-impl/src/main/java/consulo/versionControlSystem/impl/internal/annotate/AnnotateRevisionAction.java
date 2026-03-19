package consulo.versionControlSystem.impl.internal.annotate;

import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.PlatformDataKeys;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.plain.PlainTextFileType;
import consulo.application.dumb.DumbAware;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.action.UpToDateLineNumberListener;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsFileRevisionEx;
import consulo.versionControlSystem.virtualFileSystem.VcsFileSystem;
import consulo.versionControlSystem.virtualFileSystem.VcsVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

import java.util.List;

abstract class AnnotateRevisionAction extends AnnotateRevisionActionBase implements DumbAware, UpToDateLineNumberListener {
    
    private final FileAnnotation myAnnotation;
    
    private final AbstractVcs myVcs;

    private int currentLine;

    public AnnotateRevisionAction(
        @Nullable String text,
        @Nullable String description,
        @Nullable Image icon,
        FileAnnotation annotation,
        AbstractVcs vcs
    ) {
        super(LocalizeValue.ofNullable(text), LocalizeValue.ofNullable(description), icon);
        myAnnotation = annotation;
        myVcs = vcs;
    }

    @Override
    public void update(AnActionEvent e) {
        if (Boolean.TRUE.equals(e.getData(PlatformDataKeys.IS_MODAL_CONTEXT))) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        if (myAnnotation.getFile() == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        if (getRevisions() == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        e.getPresentation().setVisible(true);

        super.update(e);
    }

    protected abstract @Nullable List<VcsFileRevision> getRevisions();

    protected @Nullable AbstractVcs getVcs(AnActionEvent e) {
        return myVcs;
    }

    @Nullable
    @Override
    protected VirtualFile getFile(AnActionEvent e) {
        VcsFileRevision revision = getFileRevision(e);
        if (revision == null) {
            return null;
        }

        final FileType currentFileType = myAnnotation.getFile().getFileType();
        FilePath filePath =
            (revision instanceof VcsFileRevisionEx ? ((VcsFileRevisionEx) revision).getPath() : VcsUtil.getFilePath(myAnnotation.getFile()));
        return new VcsVirtualFile(filePath.getPath(), revision, VcsFileSystem.getInstance()) {
            
            @Override
            public FileType getFileType() {
                FileType type = super.getFileType();
                if (!type.isBinary()) {
                    return type;
                }
                if (!currentFileType.isBinary()) {
                    return currentFileType;
                }
                return PlainTextFileType.INSTANCE;
            }
        };
    }

    @Nullable
    @Override
    protected VcsFileRevision getFileRevision(AnActionEvent e) {
        List<VcsFileRevision> revisions = getRevisions();
        assert revisions != null;

        if (currentLine < 0 || currentLine >= revisions.size()) {
            return null;
        }
        return revisions.get(currentLine);
    }

    @Override
    public void accept(int integer) {
        currentLine = integer;
    }
}
