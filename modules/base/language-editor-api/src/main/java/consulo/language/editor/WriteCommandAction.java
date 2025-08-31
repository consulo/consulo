/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.BaseActionRunnable;
import consulo.application.Result;
import consulo.application.RunResult;
import consulo.application.util.function.ThrowableComputable;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.undoRedo.builder.CommandBuilder;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

@Deprecated
@DeprecationInfo("Use CommandProcessor#newCommand()")
public abstract class WriteCommandAction<T> extends BaseActionRunnable<T> {
    private static final Logger LOG = Logger.getInstance(WriteCommandAction.class);

    private static final String DEFAULT_COMMAND_NAME = "Undefined";
    private static final String DEFAULT_GROUP_ID = null;

    public interface Builder {
        @Nonnull
        Builder withName(@Nullable String name);

        @Nonnull
        Builder withGroupId(@Nullable String groupId);

        <E extends Throwable> void run(@Nonnull ThrowableRunnable<E> action) throws E;

        <R, E extends Throwable> R compute(@Nonnull ThrowableComputable<R, E> action) throws E;
    }

    private static class BuilderImpl implements Builder {
        private final Project myProject;
        private final PsiFile[] myFiles;
        private String myCommandName = DEFAULT_COMMAND_NAME;
        private String myGroupId = DEFAULT_GROUP_ID;

        private BuilderImpl(Project project, PsiFile... files) {
            myProject = project;
            myFiles = files;
        }

        @Nonnull
        @Override
        public Builder withName(String name) {
            myCommandName = name;
            return this;
        }

        @Nonnull
        @Override
        public Builder withGroupId(String groupId) {
            myGroupId = groupId;
            return this;
        }

        @Override
        @RequiredUIAccess
        public <E extends Throwable> void run(@Nonnull final ThrowableRunnable<E> action) throws E {
            new WriteCommandAction(myProject, myCommandName, myGroupId, myFiles) {
                @Override
                protected void run(@Nonnull Result result) throws Throwable {
                    action.run();
                }
            }.execute();
        }

        @Override
        @RequiredUIAccess
        public <R, E extends Throwable> R compute(@Nonnull final ThrowableComputable<R, E> action) throws E {
            return new WriteCommandAction<R>(myProject, myCommandName, myGroupId, myFiles) {
                @Override
                protected void run(@Nonnull Result<R> result) throws Throwable {
                    result.setResult(action.compute());
                }
            }.execute().getResultObject();
        }
    }

    @Nonnull
    public static Builder writeCommandAction(Project project) {
        return new BuilderImpl(project);
    }

    @Nonnull
    public static Builder writeCommandAction(@Nonnull PsiFile first, @Nonnull PsiFile... others) {
        return new BuilderImpl(first.getProject(), ArrayUtil.prepend(first, others));
    }

    private final String myCommandName;
    private final String myGroupID;
    private final Project myProject;
    private final PsiFile[] myPsiFiles;

    protected WriteCommandAction(@Nullable Project project, /*@NotNull*/ PsiFile... files) {
        this(project, DEFAULT_COMMAND_NAME, files);
    }

    protected WriteCommandAction(@Nullable Project project, @Nullable String commandName, /*@NotNull*/ PsiFile... files) {
        this(project, commandName, DEFAULT_GROUP_ID, files);
    }

    protected WriteCommandAction(
        @Nullable Project project,
        @Nullable String commandName,
        @Nullable String groupID, /*@NotNull*/
        PsiFile... files
    ) {
        myCommandName = commandName;
        myGroupID = groupID;
        myProject = project;
        if (files == null) {
            LOG.warn("'files' parameter must not be null", new Throwable());
        }
        myPsiFiles = files == null || files.length == 0 ? PsiFile.EMPTY_ARRAY : files;
    }

    public final Project getProject() {
        return myProject;
    }

    public final String getCommandName() {
        return myCommandName;
    }

    public String getGroupID() {
        return myGroupID;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public RunResult<T> execute() {
        Application application = Application.get();
        boolean dispatchThread = application.isDispatchThread();

        if (!dispatchThread && application.isReadAccessAllowed()) {
            LOG.error("Must not start write action from within read action in the other thread - deadlock is coming");
            throw new IllegalStateException();
        }

        RunResult<T> result = new RunResult<>(this);
        performWriteCommandAction(result);
        return result;
    }

    @RequiredUIAccess
    private void performWriteCommandAction(@Nonnull RunResult<T> result) {
        if (!FileModificationService.getInstance().preparePsiElementsForWrite(Arrays.asList(myPsiFiles))) {
            return;
        }

        // this is needed to prevent memory leak, since the command is put into undo queue
        RunResult[] results = {result};

        doExecuteCommand(() -> {
            results[0].run();
            results[0] = null;
        });
    }

    protected boolean isGlobalUndoAction() {
        return false;
    }

    protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
        return UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION;
    }

    /**
     * See {@link CommandBuilder#shouldRecordActionForActiveDocument (boolean)} for details.
     */
    protected boolean shouldRecordActionForActiveDocument() {
        return true;
    }

    @RequiredUIAccess
    public void performCommand() throws Throwable {
        //this is needed to prevent memory leak, since command
        // is put into undo queue
        RunResult[] results = {new RunResult<>(this)};
        SimpleReference<Throwable> exception = new SimpleReference<>();

        doExecuteCommand(() -> {
            exception.set(results[0].run().getThrowable());
            results[0] = null;
        });

        Throwable throwable = exception.get();
        if (throwable != null) {
            throw throwable;
        }
    }

    @RequiredUIAccess
    private void doExecuteCommand(Runnable runnable) {
        CommandProcessor.getInstance().newCommand()
            .project(getProject())
            .name(LocalizeValue.ofNullable(getCommandName()))
            .groupId(getGroupID())
            .undoConfirmationPolicy(getUndoConfirmationPolicy())
            .shouldRecordActionForActiveDocument(shouldRecordActionForActiveDocument())
            .inWriteAction()
            .inGlobalUndoActionIf(isGlobalUndoAction())
            .run(runnable);
    }

    /**
     * WriteCommandAction without result
     */
    public abstract static class Simple<T> extends WriteCommandAction<T> {
        protected Simple(Project project, /*@NotNull*/ PsiFile... files) {
            super(project, files);
        }

        protected Simple(Project project, String commandName, /*@NotNull*/ PsiFile... files) {
            super(project, commandName, files);
        }

        protected Simple(Project project, String name, String groupID, /*@NotNull*/ PsiFile... files) {
            super(project, name, groupID, files);
        }

        @Override
        protected void run(@Nonnull Result<T> result) throws Throwable {
            run();
        }

        protected abstract void run() throws Throwable;
    }

    @RequiredUIAccess
    public static void runWriteCommandAction(Project project, @Nonnull Runnable runnable) {
        runWriteCommandAction(project, DEFAULT_COMMAND_NAME, DEFAULT_GROUP_ID, runnable);
    }

    @RequiredUIAccess
    public static void runWriteCommandAction(
        Project project,
        @Nullable final String commandName,
        @Nullable final String groupID,
        @Nonnull final Runnable runnable,
        @Nonnull PsiFile... files
    ) {
        new Simple(project, commandName, groupID, files) {
            @Override
            protected void run() throws Throwable {
                runnable.run();
            }
        }.execute();
    }

    @SuppressWarnings("LambdaUnfriendlyMethodOverload")
    @RequiredUIAccess
    public static <T> T runWriteCommandAction(Project project, @Nonnull final Supplier<T> computable) {
        return new WriteCommandAction<T>(project) {
            @Override
            protected void run(@Nonnull Result<T> result) throws Throwable {
                result.setResult(computable.get());
            }
        }.execute().getResultObject();
    }

    @SuppressWarnings("LambdaUnfriendlyMethodOverload")
    @RequiredUIAccess
    public static <T, E extends Throwable> T runWriteCommandAction(
        Project project,
        @Nonnull final ThrowableComputable<T, E> computable
    ) throws E {
        RunResult<T> result = new WriteCommandAction<T>(project, "") {
            @Override
            protected void run(@Nonnull Result<T> result) throws Throwable {
                result.setResult(computable.compute());
            }
        }.execute();
        Throwable t = result.getThrowable();
        if (t != null) {
            @SuppressWarnings("unchecked") E e = (E)t;
            throw e;
        }
        return result.throwException().getResultObject();
    }

    //<editor-fold desc="Deprecated stuff.">

    /**
     * @deprecated use {@link FileModificationService#preparePsiElementsForWrite(Collection)} (to be removed in IDEA 2018)
     */
    @SuppressWarnings("unused")
    public static boolean ensureFilesWritable(@Nonnull Project project, @Nonnull Collection<PsiFile> psiFiles) {
        return FileModificationService.getInstance().preparePsiElementsForWrite(psiFiles);
    }
    //</editor-fold>
}