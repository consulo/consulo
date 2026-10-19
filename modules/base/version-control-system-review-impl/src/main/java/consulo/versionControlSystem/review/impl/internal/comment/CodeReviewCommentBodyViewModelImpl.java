// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.comment;

import consulo.application.progress.ProgressBuilderFactory;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.review.comment.CodeReviewCommentBodyBlock;
import consulo.review.comment.CodeReviewMarkdownCommentBodyBlock;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.versionControlSystem.change.patch.ApplyPatchStatus;
import consulo.versionControlSystem.change.patch.PatchApplyService;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.change.patch.PatchHunkUtil;
import consulo.versionControlSystem.change.patch.PatchLine;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.impl.internal.patch.PatchReader;
import consulo.versionControlSystem.impl.internal.patch.PatchSyntaxException;
import consulo.versionControlSystem.review.comment.CodeReviewCommentBodyViewModel;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestedChangeSupport;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionApplicability;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionSyntax;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionThreadData;
import consulo.versionControlSystem.review.impl.internal.timeline.TimelineDiffComponentFactoryImpl;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CodeReviewCommentBodyViewModelImpl implements CodeReviewCommentBodyViewModel {
    private static final Logger LOG = Logger.getInstance(CodeReviewCommentBodyViewModelImpl.class);

    private final Project myProject;
    private final CodeReviewSuggestedChangeSupport mySupport;
    private final ObservableValue<@Nullable CodeReviewSuggestionThreadData> myThreadData;
    private final ObservableValue<String> myBody;
    private final TimelineDiffComponentFactoryImpl myDiffComponentFactory;
    private final MutableObservableValue<List<CodeReviewCommentBodyBlock>> myBlocks;

    public CodeReviewCommentBodyViewModelImpl(Project project,
                                              Disposable parentDisposable,
                                              CodeReviewSuggestedChangeSupport support,
                                              ObservableValue<@Nullable CodeReviewSuggestionThreadData> threadData,
                                              ObservableValue<String> body,
                                              TimelineDiffComponentFactoryImpl diffComponentFactory) {
        myProject = project;
        mySupport = support;
        myThreadData = threadData;
        myBody = body;
        myDiffComponentFactory = diffComponentFactory;
        myBlocks = MutableObservableValue.of(computeBlocks(threadData.get(), body.get()));

        Runnable threadListener = threadData.addListener(value -> updateBlocks());
        Runnable bodyListener = body.addListener(value -> updateBlocks());
        Disposer.register(parentDisposable, () -> {
            threadListener.run();
            bodyListener.run();
        });
    }

    private void updateBlocks() {
        myBlocks.set(computeBlocks(myThreadData.get(), myBody.get()));
    }

    @Override
    public ObservableValue<List<CodeReviewCommentBodyBlock>> getBlocks() {
        return myBlocks;
    }

    @Override
    public CodeReviewSuggestedChangeSupport getSupport() {
        return mySupport;
    }

    @Override
    public ObservableValue<Boolean> isOnReviewBranch() {
        return mySupport.isOnReviewBranch();
    }

    TimelineDiffComponentFactoryImpl getDiffComponentFactory() {
        return myDiffComponentFactory;
    }

    Project getProject() {
        return myProject;
    }

    private List<CodeReviewCommentBodyBlock> computeBlocks(@Nullable CodeReviewSuggestionThreadData thread, String body) {
        if (thread == null) {
            return List.of();
        }
        CodeReviewSuggestionSyntax syntax = mySupport.getSyntax();
        if (!syntax.containsSuggestedChange(body)) {
            return List.of(new CodeReviewMarkdownCommentBodyBlock(body));
        }

        PatchHunk hunk = readHunk(thread);
        if (hunk == null) {
            LOG.warn("Empty diff hunk for thread " + thread.getId());
            return List.of(new CodeReviewMarkdownCommentBodyBlock(body));
        }

        List<String> codeLines = new ArrayList<>();
        for (PatchLine line : hunk.getLines()) {
            if (line.getType() != PatchLine.Type.REMOVE) {
                codeLines.add(line.getText());
            }
        }
        codeLines = codeLines.subList(Math.max(0, codeLines.size() - thread.getCodeLinesCount()), codeLines.size());

        List<CodeReviewCommentBodyBlock> blocks = new ArrayList<>();
        List<String> markdown = new ArrayList<>();
        String suggestionStart = "";
        List<String> suggestion = null;
        for (String line : body.replace("\r", "").split("\n", -1)) {
            if (suggestion == null && syntax.isSuggestionStart(line)) {
                suggestionStart = line;
                suggestion = new ArrayList<>();
                continue;
            }
            else if (suggestion != null && syntax.isSuggestionEnd(line)) {
                addMarkdown(blocks, markdown);
                blocks.add(createSuggestionBlock(thread, suggestionStart, suggestion, hunk, codeLines));
                suggestion = null;
                continue;
            }

            if (suggestion != null) {
                suggestion.add(line);
            }
            else {
                markdown.add(line);
            }
        }
        if (suggestion != null) {
            markdown.add(suggestionStart);
            markdown.addAll(suggestion);
        }
        addMarkdown(blocks, markdown);
        return blocks;
    }

    private static void addMarkdown(List<CodeReviewCommentBodyBlock> blocks, List<String> markdown) {
        String text = String.join("\n", markdown);
        markdown.clear();
        if (!text.isBlank()) {
            blocks.add(new CodeReviewMarkdownCommentBodyBlock(text.strip()));
        }
    }

    private static @Nullable PatchHunk readHunk(CodeReviewSuggestionThreadData thread) {
        try {
            PatchReader patchReader = new PatchReader(PatchHunkUtil.createPatchFromHunk("_", thread.getDiffHunk()));
            List<TextFilePatch> patches = patchReader.readTextPatches();
            if (patches.isEmpty() || patches.get(0).getHunks().isEmpty()) {
                return null;
            }
            return patches.get(0).getHunks().get(0);
        }
        catch (PatchSyntaxException e) {
            LOG.warn("Cannot read the diff hunk of thread " + thread.getId(), e);
            return null;
        }
    }

    private CodeReviewSuggestedChangeBlockImpl createSuggestionBlock(CodeReviewSuggestionThreadData thread,
                                                                     String suggestionStart,
                                                                     List<String> suggestion,
                                                                     PatchHunk hunk,
                                                                     List<String> codeLines) {
        CodeReviewSuggestionSyntax syntax = mySupport.getSyntax();
        int linesAbove = syntax.getLinesAbove(suggestionStart);
        int linesBelow = syntax.getLinesBelow(suggestionStart);

        CodeReviewSuggestionApplicability applicability;
        TextFilePatch patch;
        List<String> changedLines = codeLines;
        Integer originalStartLineIndex = thread.getOriginalStartLineIndex();
        Integer originalEndLineIndex = thread.getOriginalEndLineIndex();
        Integer startLineIndex = thread.getStartLineIndex();
        Integer endLineIndex = thread.getEndLineIndex();
        if (thread.isResolved()) {
            applicability = CodeReviewSuggestionApplicability.RESOLVED;
            patch = null;
        }
        else if (originalStartLineIndex == null || originalEndLineIndex == null || startLineIndex == null || endLineIndex == null) {
            applicability = CodeReviewSuggestionApplicability.OUTDATED;
            patch = null;
        }
        else {
            applicability = CodeReviewSuggestionApplicability.APPLICABLE;

            int start = startLineIndex - linesAbove;
            int end = endLineIndex + linesBelow;
            int suggestionIndexShift = Math.max(suggestion.size() - 1, 0);
            PatchHunk suggestedChangePatchHunk = new PatchHunk(start, end, start, start + suggestionIndexShift);
            List<PatchLine> changed = PatchHunkUtil.getLinesInRange(hunk,
                Side.RIGHT,
                new LineRange(originalStartLineIndex - linesAbove, originalEndLineIndex + linesBelow + 1));
            changedLines = new ArrayList<>();
            for (PatchLine line : changed) {
                suggestedChangePatchHunk.addLine(new PatchLine(PatchLine.Type.REMOVE, line.getText()));
                changedLines.add(line.getText());
            }
            for (String line : suggestion) {
                suggestedChangePatchHunk.addLine(new PatchLine(PatchLine.Type.ADD, line));
            }
            patch = new TextFilePatch(StandardCharsets.UTF_8);
            patch.setBeforeName(thread.getFilePath());
            patch.setAfterName(thread.getFilePath());
            patch.addHunk(suggestedChangePatchHunk);
        }
        return new CodeReviewSuggestedChangeBlockImpl(this, patch, applicability, List.copyOf(changedLines), List.copyOf(suggestion));
    }

    @RequiredUIAccess
    @Override
    public void applySuggestionLocally(TextFilePatch patch) {
        CodeReviewSuggestionThreadData thread = myThreadData.get();
        myProject.getInstance(PatchApplyService.class)
            .apply(mySupport.getRepositoryRoot(), List.of(patch), false)
            .whenComplete((status, error) -> {
                if (error != null) {
                    LOG.warn("Failed to apply suggested change\n" + hunksText(patch), error);
                }
                else {
                    resolveIfApplied(thread, status);
                }
            });
    }

    @RequiredUIAccess
    @Override
    public void commitSuggestion(TextFilePatch patch, String commitMessage) {
        CodeReviewSuggestionThreadData thread = myThreadData.get();
        myProject.getApplication().getInstance(ProgressBuilderFactory.class)
            .newProgressBuilder(myProject, ReviewLocalize.reviewSuggestedChangeProgressCommit())
            .execute(UIAccess.current(), () -> Coroutine.first(
                CompletableFutureStep.<Object, ApplyPatchStatus>await(input -> mySupport.commit(patch, commitMessage))))
            .whenComplete((status, error) -> {
                if (error != null) {
                    LOG.warn("Failed to apply suggested change\n" + hunksText(patch), error);
                }
                else {
                    resolveIfApplied(thread, status);
                }
            });
    }

    private void resolveIfApplied(@Nullable CodeReviewSuggestionThreadData thread, @Nullable ApplyPatchStatus status) {
        if (status == ApplyPatchStatus.SUCCESS && thread != null && thread.canResolve()) {
            CompletableFuture<?> resolved = mySupport.resolveThread(thread.getId());
            resolved.whenComplete((value, error) -> {
                if (error != null) {
                    LOG.warn("Failed to resolve thread " + thread.getId(), error);
                }
            });
        }
    }

    private static String hunksText(TextFilePatch patch) {
        List<String> texts = new ArrayList<>();
        for (PatchHunk hunk : patch.getHunks()) {
            texts.add(hunk.getText());
        }
        return String.join("\n\n", texts);
    }
}
