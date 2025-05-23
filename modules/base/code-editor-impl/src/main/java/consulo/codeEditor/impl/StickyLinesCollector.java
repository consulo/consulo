// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.stickyLine.StickyLine;
import consulo.codeEditor.internal.stickyLine.StickyLineInfo;
import consulo.codeEditor.internal.stickyLine.StickyLinesModel;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import java.util.*;

public class StickyLinesCollector {
    private final Project project;
    private final Document document;

    private static final Logger LOG = Logger.getInstance(StickyLinesCollector.class);

    public StickyLinesCollector(Project project, Document document) {
        this.project = project;
        this.document = document;
    }

    public static class ModStamp {
        private static final Key<Long> STICKY_LINES_MOD_STAMP_KEY = Key.create("editor.sticky.lines.mod.stamp");
        private static final Key<Boolean> STICKY_LINES_FIRST_PASS_FOR_EDITOR = Key.create("editor.sticky.lines.first.pass");

        public static boolean isChanged(Editor editor, PsiFile psiFile) {
            boolean isFirstPass = editor.getUserData(STICKY_LINES_FIRST_PASS_FOR_EDITOR) == null;
            if (isFirstPass) {
                LOG.trace("first pass for editor " + debugPsiFile(psiFile));
                editor.putUserData(STICKY_LINES_FIRST_PASS_FOR_EDITOR, false);
                if (psiFile.getUserData(STICKY_LINES_MOD_STAMP_KEY) != null) {
                    reset(psiFile);
                }
                return true;
            }
            Long prevModStamp = psiFile.getUserData(STICKY_LINES_MOD_STAMP_KEY);
            long currModStamp = modStamp(psiFile);
            LOG.trace("checking modStamp: " + traceStampChanged(psiFile, prevModStamp, currModStamp));
            return !Objects.equals(prevModStamp, currModStamp);
        }

        public static void update(PsiFile psiFile) {
            long modStamp = modStamp(psiFile);
            psiFile.putUserData(STICKY_LINES_MOD_STAMP_KEY, modStamp);
            LOG.trace("updating modStamp=" + modStamp + " for " + debugPsiFile(psiFile));
        }

        public static void reset(PsiFile psiFile) {
            psiFile.putUserData(STICKY_LINES_MOD_STAMP_KEY, null);
            LOG.trace("resetting modStamp for " + debugPsiFile(psiFile));
        }

        private static long modStamp(PsiFile psiFile) {
            return psiFile.getModificationStamp() + psiFile.getViewProvider().getDocument().getModificationStamp();
        }

        private static String traceStampChanged(PsiFile psiFile, Long prevModStamp, long currModStamp) {
            boolean isChanged = !Objects.equals(prevModStamp, currModStamp);
            String stamp = isChanged
                ? "prevStamp=" + prevModStamp + ", currStamp=" + currModStamp
                : "stamp=" + currModStamp;
            return "isChange=" + isChanged + ", " + stamp + ", " + debugPsiFile(psiFile);
        }

        private static String debugPsiFile(PsiFile psiFile) {
            String fileName = psiFile.getName();
            String psiFileId = Integer.toHexString(System.identityHashCode(psiFile));
            String documentId = Integer.toHexString(System.identityHashCode(psiFile.getViewProvider().getDocument()));
            long psiFileStamp = psiFile.getModificationStamp();
            long documentStamp = psiFile.getViewProvider().getDocument().getModificationStamp();
            return fileName + "[psiId=@" + psiFileId + ", psiStamp=" + psiFileStamp + ", docId=@" + documentId + ", docStamp=" + documentStamp + "]";
        }
    }

    @RequiredReadAction
    public void forceCollectPass() {
        Application.get().assertReadAccessAllowed();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getCachedPsiFile(document);
        if (psiFile != null) {
            ModStamp.reset(psiFile);
        }
        else if (LOG.isDebugEnabled()) {
            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            LOG.debug("cannot find psi file for " + (file != null ? file.getName() : "UNKNOWN"));
        }
    }

    @RequiredUIAccess
    public void applyLines(PsiFile psiFile, Collection<StickyLineInfo> lines) {
        UIAccess.assertIsUIThread();

        ModStamp.update(psiFile);
        StickyLinesModel stickyModel = stickyLinesModel(psiFile);
        if (stickyModel == null) return;

        Set<StickyLineInfo> linesToAdd = new HashSet<>(lines);
        List<StickyLine> outdatedLines = mergeWithExistingLines(stickyModel, linesToAdd);

        for (StickyLine toRemove : outdatedLines) {
            stickyModel.removeStickyLine(toRemove);
        }

        for (StickyLineInfo toAdd : linesToAdd) {
            stickyModel.addStickyLine(toAdd.textOffset(), toAdd.endOffset(), toAdd.debugText());
        }

        LOG.debug("total lines applied: " + lines.size() +
            ", new added: " + linesToAdd.size() +
            ", old removed: " + outdatedLines.size() +
            ", " + ModStamp.debugPsiFile(psiFile));

        stickyModel.notifyLinesUpdate();
    }

    private StickyLinesModel stickyLinesModel(PsiFile psiFile) {
        StickyLinesModel stickyModel = StickyLinesModel.getModel(project, document);
        if (stickyModel == null) {
            ModStamp.reset(psiFile);
            LOG.error("sticky lines model does not exist while applying collected lines for " + ModStamp.debugPsiFile(psiFile));
            return null;
        }
        return stickyModel;
    }

    private List<StickyLine> mergeWithExistingLines(StickyLinesModel stickyModel, Set<StickyLineInfo> linesToAdd) {
        List<StickyLine> outdatedLines = new ArrayList<>();
        stickyModel.processStickyLines(StickyLinesModel.SourceID.IJ, existingLine -> {
            StickyLineInfo existing = new StickyLineInfo(existingLine.textRange());
            if (!linesToAdd.remove(existing)) {
                outdatedLines.add(existingLine);
            }
            return true;
        });
        return outdatedLines;
    }

    private String fileName(VirtualFile vFile) {
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        return psiFile != null ? ModStamp.debugPsiFile(psiFile) : vFile.getName();
    }

    private String debugText(PsiElement element) {
        return Registry.is("editor.show.sticky.lines.debug") ? element.toString() : null;
    }
}
