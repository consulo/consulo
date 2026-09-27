/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.it;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.progress.ProgressManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.it.daemon.ExpectedHighlightingData;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.impl.internal.daemon.LineMarkersPass;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An opened project with one module, {@code main}, whose content root is a fresh directory and whose sources live in
 * {@link #SOURCE_ROOT}. A test writes files with their expected highlights, line markers and caret marked up, and asks
 * for highlighting, references and inspection results against that markup. Nothing here opens an editor: the caret
 * offset comes out of the markup.
 * <p>
 * Everything that has to settle before it can be asked about - the project model after a module change, the index
 * after a file is written - is waited for before the call returns.
 *
 * @author VISTALL
 */
public final class CodeInsightTestFixture {
    public static final String MODULE_NAME = "main";
    public static final String SOURCE_ROOT = "src";

    private final Application myApplication;
    private final Project myProject;
    private final VirtualFile myProjectRoot;
    private final Module myModule;

    private @Nullable PsiFile myFile;
    private @Nullable ExpectedHighlightingData myExpected;

    private CodeInsightTestFixture(Application application, Project project, VirtualFile projectRoot, Module module) {
        myApplication = application;
        myProject = project;
        myProjectRoot = projectRoot;
        myModule = module;
    }

    public static CodeInsightTestFixture create(Application application, HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();
        Files.createDirectories(directory.resolve(SOURCE_ROOT));

        Project project = projects.open(directory);

        VirtualFile projectRoot = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        if (projectRoot == null) {
            throw new AssertionError(directory + " is not visible in the VFS");
        }
        VirtualFile sourceRoot = projectRoot.findChild(SOURCE_ROOT);
        if (sourceRoot == null) {
            throw new AssertionError(SOURCE_ROOT + " is not visible under " + projectRoot);
        }

        Module module = HeadlessModules.createModule(project, MODULE_NAME, projectRoot);
        HeadlessModules.addSourceRoot(module, sourceRoot);
        HeadlessProjects.awaitIdle(project);

        return new CodeInsightTestFixture(application, project, projectRoot, module);
    }

    public Project getProject() {
        return myProject;
    }

    public Module getModule() {
        return myModule;
    }

    /**
     * The content root of {@link #getModule()}; a test lays out further modules or roots under it.
     */
    public VirtualFile getProjectRoot() {
        return myProjectRoot;
    }

    /**
     * Writes {@code text} as it is, markup included, at {@code relativePath} under the project root.
     */
    public PsiFile addFileToProject(String relativePath, String text) throws Exception {
        return psiFile(writeFile(relativePath, text));
    }

    /**
     * Writes a file named {@code fileName} into the source root with its markup stripped, and makes it the file the
     * other calls act on. The markup - expected highlights, line markers, the caret - is what those calls check
     * against.
     */
    public PsiFile configureByText(String fileName, String markedUpText) throws Exception {
        ExpectedHighlightingData expected = ExpectedHighlightingData.parse(markedUpText);
        PsiFile file = psiFile(writeFile(SOURCE_ROOT + "/" + fileName, expected.getText()));
        myExpected = expected;
        myFile = file;
        return file;
    }

    public PsiFile getFile() {
        if (myFile == null) {
            throw new IllegalStateException("nothing was configured - call configureByText first");
        }
        return myFile;
    }

    public Document getDocument() {
        PsiFile file = getFile();
        Document document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(file.getVirtualFile()));
        if (document == null) {
            throw new AssertionError(file + " has no document");
        }
        return document;
    }

    public ExpectedHighlightingData getExpectedHighlightingData() {
        if (myExpected == null) {
            throw new IllegalStateException("nothing was configured - call configureByText first");
        }
        return myExpected;
    }

    /**
     * Leaves exactly the given inspections enabled, so what a test checks is their doing and not that of whatever
     * else is registered and on by default.
     */
    @SafeVarargs
    public final void enableInspections(Class<? extends InspectionTool>... toolClasses) {
        enableInspections(List.of(toolClasses));
    }

    public void enableInspections(Collection<Class<? extends InspectionTool>> toolClasses) {
        Set<String> wanted = new HashSet<>();
        for (Class<? extends InspectionTool> toolClass : toolClasses) {
            wanted.add(tool(toolClass).getShortName());
        }

        InspectionProfileImpl profile = profile();
        List<String> others = ReadAction.compute(() -> {
            List<String> shortNames = new ArrayList<>();
            for (InspectionToolWrapper wrapper : profile.getInspectionTools(null)) {
                if (!wanted.contains(wrapper.getShortName())) {
                    shortNames.add(wrapper.getShortName());
                }
            }
            return shortNames;
        });

        profile.disableToolByDefault(others, myProject);
        profile.enableToolsByDefault(new ArrayList<>(wanted), myProject);
    }

    /**
     * The profile's own wrapper of an inspection, which is what the highlighting passes run.
     */
    public InspectionToolWrapper getInspectionToolWrapper(Class<? extends InspectionTool> toolClass) {
        String shortName = tool(toolClass).getShortName();
        InspectionToolWrapper wrapper = profile().getInspectionTool(shortName, myProject);
        if (wrapper == null) {
            throw new IllegalStateException(shortName + " is not in the profile");
        }
        return wrapper;
    }

    /**
     * The live settings object of an inspection - the one the highlighting passes read, not a fresh copy.
     */
    @SuppressWarnings("unchecked")
    public <S> S getInspectionState(Class<? extends InspectionTool> toolClass) {
        return (S) getInspectionToolWrapper(toolClass).getToolState().getState();
    }

    /**
     * Runs the daemon's main passes over the configured file and returns what they produced. Needs no editor and does
     * not wait for the daemon's restart timer.
     */
    public List<HighlightInfo> doHighlighting() throws Exception {
        PsiFile file = getFile();
        Document document = getDocument();
        DaemonCodeAnalyzerInternal daemon = DaemonCodeAnalyzerInternal.getInstanceEx(myProject);

        SimpleReference<List<HighlightInfo>> result = SimpleReference.create();
        SimpleReference<Throwable> failure = SimpleReference.create();

        myApplication.executeOnPooledThread(() -> {
            DaemonProgressIndicator indicator = new DaemonProgressIndicator();
            try {
                ProgressManager.getInstance().runProcess(
                    () -> result.set(ReadAction.compute(() -> daemon.runMainPasses(file, document, indicator))),
                    indicator
                );
            }
            catch (Throwable e) {
                failure.set(e);
            }
        }).get();

        if (failure.get() != null) {
            throw new AssertionError("the main passes did not complete", failure.get());
        }
        return result.get();
    }

    public void checkHighlighting() throws Exception {
        getExpectedHighlightingData().checkResult(doHighlighting());
    }

    /**
     * The gutter markers the line marker pass collects for the configured file from the registered providers.
     */
    public List<LineMarkerInfo<?>> getLineMarkers() {
        PsiFile file = getFile();
        Document document = getDocument();
        return ReadAction.compute(() -> List.copyOf(LineMarkersPass.queryLineMarkers(file, document)));
    }

    public void checkLineMarkers() {
        List<LineMarkerInfo<?>> markers = getLineMarkers();
        ReadAction.run(() -> getExpectedHighlightingData().checkLineMarkers(markers));
    }

    public @Nullable PsiReference getReferenceAtCaretPosition() {
        PsiFile file = getFile();
        int offset = getExpectedHighlightingData().getCaretOffset();
        return ReadAction.compute(() -> file.findReferenceAt(offset));
    }

    public PsiReference getReferenceAtCaretPositionWithAssertion() {
        PsiReference reference = getReferenceAtCaretPosition();
        if (reference == null) {
            throw new AssertionError("no reference at the caret in " + getFile().getName());
        }
        return reference;
    }

    private VirtualFile writeFile(String relativePath, String text) throws Exception {
        VirtualFile file = WriteAction.compute(() -> {
            int slash = relativePath.lastIndexOf('/');
            VirtualFile parent = slash < 0
                ? myProjectRoot
                : VirtualFileUtil.createDirectoryIfMissing(myProjectRoot, relativePath.substring(0, slash));
            String name = relativePath.substring(slash + 1);
            VirtualFile child = parent.findChild(name);
            if (child == null) {
                child = parent.createChildData(this, name);
            }
            VirtualFileUtil.saveText(child, text);
            return child;
        });
        HeadlessProjects.awaitIdle(myProject);
        return file;
    }

    private PsiFile psiFile(VirtualFile file) {
        PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(myProject).findFile(file));
        if (psiFile == null) {
            throw new AssertionError(file + " has no psi");
        }
        return psiFile;
    }

    private InspectionTool tool(Class<? extends InspectionTool> toolClass) {
        return myApplication.getExtensionPoint(InspectionTool.class).findExtensionOrFail(toolClass);
    }

    private InspectionProfileImpl profile() {
        return (InspectionProfileImpl) InspectionProjectProfileManager.getInstance(myProject).getCurrentProfile();
    }
}
