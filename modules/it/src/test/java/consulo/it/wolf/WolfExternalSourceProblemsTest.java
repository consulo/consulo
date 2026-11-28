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
package consulo.it.wolf;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.it.internal.HeadlessWolfFileProblemFilter;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Problems reported for a file by external sources: the file is a problem file while at least one source reports problems.
 *
 * @author VISTALL
 * @since 2026-11-15
 */
@ExtendWith(HeadlessProjectExtension.class)
public class WolfExternalSourceProblemsTest {
    private Disposable myScope;
    private final List<String> myEvents = new CopyOnWriteArrayList<>();

    @BeforeEach
    public void setUp() {
        myScope = Disposable.newDisposable();
        myEvents.clear();
    }

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myScope);
    }

    @Test
    public void fileIsAProblemWhileAnySourceReportsProblems(CodeInsightTestFixture fixture) throws Exception {
        VirtualFile file = fixture.addFileToProject("src/" + HeadlessWolfFileProblemFilter.PREFIX + "-sources.txt", "text\n").getVirtualFile();
        WolfTheProblemSolver solver = solver(fixture.getProject());

        solver.reportProblemsFromExternalSource(file, "first");
        assertThat(solver.isProblemFile(file)).isTrue();
        assertThat(solver.hasProblemFilesBeneath(file::equals)).isTrue();

        solver.reportProblemsFromExternalSource(file, "second");
        solver.clearProblemsFromExternalSource(file, "first");
        assertThat(solver.isProblemFile(file)).isTrue();

        solver.clearProblemsFromExternalSource(file, "second");
        assertThat(solver.isProblemFile(file)).isFalse();
        assertThat(solver.hasProblemFilesBeneath(file::equals)).isFalse();

        assertThat(myEvents).containsExactly(
            "appeared " + file.getName(),
            "changed " + file.getName(),
            "changed " + file.getName(),
            "disappeared " + file.getName()
        );
    }

    @Test
    public void clearingRegularProblemsKeepsTheExternalOnes(CodeInsightTestFixture fixture) throws Exception {
        VirtualFile file = fixture.addFileToProject("src/" + HeadlessWolfFileProblemFilter.PREFIX + "-regular.txt", "text\n").getVirtualFile();
        WolfTheProblemSolver solver = solver(fixture.getProject());

        solver.weHaveGotNonIgnorableProblems(file, List.of(solver.convertToProblem(file, 1, 1, new String[]{"error"})));
        solver.reportProblemsFromExternalSource(file, "external");
        solver.clearProblems(file);

        assertThat(solver.isProblemFile(file)).isTrue();
        assertThat(myEvents).containsExactly(
            "appeared " + file.getName(),
            "changed " + file.getName(),
            "changed " + file.getName()
        );

        solver.clearProblemsFromExternalSource(file, "external");
        assertThat(solver.isProblemFile(file)).isFalse();
        assertThat(myEvents).last().isEqualTo("disappeared " + file.getName());
    }

    @Test
    public void filteredOutFileIsIgnored(CodeInsightTestFixture fixture) throws Exception {
        VirtualFile file = fixture.addFileToProject("src/other-wolf.txt", "text\n").getVirtualFile();
        WolfTheProblemSolver solver = solver(fixture.getProject());

        solver.reportProblemsFromExternalSource(file, "external");

        assertThat(solver.isProblemFile(file)).isFalse();
        assertThat(myEvents).isEmpty();
    }

    private WolfTheProblemSolver solver(Project project) {
        project.getMessageBus().connect(myScope).subscribe(ProblemListener.class, new ProblemListener() {
            @Override
            public void problemsAppeared(VirtualFile file) {
                myEvents.add("appeared " + file.getName());
            }

            @Override
            public void problemsChanged(VirtualFile file) {
                myEvents.add("changed " + file.getName());
            }

            @Override
            public void problemsDisappeared(VirtualFile file) {
                myEvents.add("disappeared " + file.getName());
            }
        });
        return WolfTheProblemSolver.getInstance(project);
    }
}
