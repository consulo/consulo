package consulo.language.editor.problemView.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.language.editor.problemView.*;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFileMoveEvent;
import consulo.virtualFileSystem.event.VFileExistingFileEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Singleton
@ServiceImpl
public final class ProjectErrorsCollector implements ProblemsCollector {
    private final Project myProject;
    private final Set<String> myProviderClassFilter;
    private final Map<VirtualFile, Set<FileProblem>> myFileProblems = new ConcurrentHashMap<>();
    private final Set<Problem> myOtherProblems = new HashSet<>();
    private final AtomicInteger myProblemCount = new AtomicInteger();

    @Inject
    public ProjectErrorsCollector(Project project) {
        myProject = project;
        myProviderClassFilter = new HashSet<>(List.of("".split(" ,/|")));
        VirtualFileManager.getInstance().addAsyncFileListener(events -> {
            onVfsChanges(events);
            return null;
        }, project);
    }

    @Override
    public int getProblemCount() {
        return myProblemCount.get();
    }

    @Override
    public Set<VirtualFile> getProblemFiles() {
        synchronized (myFileProblems) {
            return new HashSet<>(myFileProblems.keySet());
        }
    }

    @Override
    public int getFileProblemCount(VirtualFile file) {
        synchronized (myFileProblems) {
            Set<FileProblem> set = myFileProblems.get(file);
            return set == null ? 0 : set.size();
        }
    }

    @Override
    public Set<Problem> getFileProblems(VirtualFile file) {
        synchronized (myFileProblems) {
            Set<FileProblem> set = myFileProblems.get(file);
            return set == null ? Collections.emptySet() : new HashSet<>(set);
        }
    }

    @Override
    public int getOtherProblemCount() {
        synchronized (myOtherProblems) {
            return myOtherProblems.size();
        }
    }

    @Override
    public Set<Problem> getOtherProblems() {
        synchronized (myOtherProblems) {
            return new HashSet<>(myOtherProblems);
        }
    }

    @Override
    public void problemAppeared(Problem problem) {
        boolean ignored = isIgnored(problem.getProvider());
        SetUpdateState state;
        if (ignored) {
            state = SetUpdateState.IGNORED;
        }
        else if (problem instanceof FileProblem fileProblem) {
            state = process(fileProblem, true, set -> {
                if (problem instanceof HighlightingDuplicateProblem &&
                    set.stream().anyMatch(p -> p instanceof HighlightingProblem)) {
                    return SetUpdateState.IGNORED;
                }
                return SetUpdateState.add(fileProblem, set);
            });
        }
        else {
            synchronized (myOtherProblems) {
                state = SetUpdateState.add(problem, myOtherProblems);
            }
        }

        notify(problem, state, true);

        if (!ignored && problem instanceof HighlightingProblem highlightingProblem) {
            List<FileProblem> duplicatesToRemove = null;
            synchronized (myFileProblems) {
                Set<FileProblem> set = myFileProblems.get(highlightingProblem.getFile());
                if (set != null) {
                    duplicatesToRemove = set.stream()
                        .filter(p -> p instanceof HighlightingDuplicateProblem)
                        .toList();
                }
            }
            if (duplicatesToRemove != null) {
                for (FileProblem duplicate : duplicatesToRemove) {
                    problemDisappeared(duplicate);
                }
            }
        }
    }

    @Override
    public void problemDisappeared(Problem problem) {
        SetUpdateState state;
        if (isIgnored(problem.getProvider())) {
            state = SetUpdateState.IGNORED;
        }
        else if (problem instanceof FileProblem fileProblem) {
            state = process(fileProblem, false, set -> SetUpdateState.remove(fileProblem, set));
        }
        else {
            synchronized (myOtherProblems) {
                state = SetUpdateState.remove(problem, myOtherProblems);
            }
        }
        notify(problem, state, true);
    }

    @Override
    public void problemUpdated(Problem problem) {
        SetUpdateState state;
        if (isIgnored(problem.getProvider())) {
            state = SetUpdateState.IGNORED;
        }
        else if (problem instanceof FileProblem fileProblem) {
            state = process(fileProblem, false, set -> SetUpdateState.update(fileProblem, set));
        }
        else {
            synchronized (myOtherProblems) {
                state = SetUpdateState.update(problem, myOtherProblems);
            }
        }
        notify(problem, state, true);
    }

    private boolean isIgnored(ProblemsProvider provider) {
        return provider.getProject() != myProject || myProviderClassFilter.contains(provider.getClass().getName());
    }

    private SetUpdateState process(FileProblem problem,
                                   boolean create,
                                   Function<Set<FileProblem>, SetUpdateState> function) {
        VirtualFile file = problem.getFile();
        synchronized (myFileProblems) {
            Set<FileProblem> set;
            if (create) {
                set = myFileProblems.computeIfAbsent(file, __ -> new HashSet<>());
            }
            else {
                set = myFileProblems.get(file);
                if (set == null) return SetUpdateState.IGNORED;
            }

            SetUpdateState state = function.apply(set);
            if (set.isEmpty()) {
                myFileProblems.remove(file);
            }
            return state;
        }
    }

    private void notify(Problem problem, SetUpdateState state, boolean later) {
        if (state == SetUpdateState.IGNORED || myProject.isDisposed()) return;

        if (later && Registry.is("ide.problems.view.notify.later", true)) {
            Application.get().invokeLater(() -> notify(problem, state, false));
            return;
        }

        switch (state) {
            case ADDED -> {
                myProject.getMessageBus().syncPublisher(ProblemsListener.TOPIC).problemAppeared(problem);
                boolean emptyBefore = myProblemCount.getAndIncrement() == 0;
                if (emptyBefore) ProblemsViewIconUpdater.update(myProject);
            }
            case REMOVED -> {
                myProject.getMessageBus().syncPublisher(ProblemsListener.TOPIC).problemDisappeared(problem);
                boolean emptyAfter = myProblemCount.decrementAndGet() == 0;
                if (emptyAfter) ProblemsViewIconUpdater.update(myProject);
            }
            case UPDATED -> myProject.getMessageBus().syncPublisher(ProblemsListener.TOPIC).problemUpdated(problem);
            case IGNORED -> {
            }
        }
    }

    private void onVfsChanges(List<? extends VFileEvent> events) {
        Set<VirtualFile> files = new LinkedHashSet<>();
        for (VFileEvent event : events) {
            if (event instanceof VFileDeleteEvent || event instanceof VFileMoveEvent) {
                VirtualFile file = ((VFileExistingFileEvent) event).getFile();
                files.add(file);
            }
        }

        List<Problem> toRemove = new ArrayList<>();
        for (VirtualFile file : files) {
            toRemove.addAll(getFileProblems(file));
        }
        for (Problem problem : toRemove) {
            problemDisappeared(problem);
        }
    }
}
