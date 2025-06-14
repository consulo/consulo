// File: InlayProviderSettingsModel.java
// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.inlay.setting;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

import javax.swing.*;
import java.util.List;

public abstract class InlayProviderSettingsModel {
    private boolean isEnabled;
    private final String id;
    private final Language language;
    protected ChangeListener onChangeListener;
    private boolean isMergedNode = false;

    public InlayProviderSettingsModel(boolean isEnabled, String id, Language language) {
        this.isEnabled = isEnabled;
        this.id = id;
        this.language = language;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public String getId() {
        return id;
    }

    public Language getLanguage() {
        return language;
    }

    public ChangeListener getOnChangeListener() {
        return onChangeListener;
    }

    public void setOnChangeListener(ChangeListener listener) {
        this.onChangeListener = listener;
    }

    public abstract String getName();

    public InlayGroup getGroup() {
        return InlayGroup.OTHER_GROUP;
    }

    public abstract JComponent getComponent();

    public Runnable collectData(Editor editor, PsiFile file) {
        return () -> collectAndApply(editor, file);
    }

    public void collectAndApply(Editor editor, PsiFile file) {
    }

    public PsiFile createFile(Project project, FileType fileType, Document document, String caseId) {
        return createFile(project, fileType, document);
    }

    public PsiFile createFile(Project project, FileType fileType, Document document) {
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        return factory.createFileFromText("dummy", fileType, document.getText());
    }

    public abstract String getDescription();

    public abstract String getPreviewText();

    public abstract String getCasePreview(ImmediateConfigurable.Case caze);

    public abstract Language getCasePreviewLanguage(ImmediateConfigurable.Case caze);

    public abstract String getCaseDescription(ImmediateConfigurable.Case caze);

    public abstract void apply();

    public abstract boolean isModified();

    public abstract void reset();

    public boolean isMergedNode() {
        return isMergedNode;
    }

    public void setMergedNode(boolean mergedNode) {
        this.isMergedNode = mergedNode;
    }

    public abstract List<ImmediateConfigurable.Case> getCases();

    @Override
    public String toString() {
        return "InlayProviderSettingsModel[" + language.getDisplayName() + ", " + id + ", isEnabled=" + isEnabled + "]";
    }
}
