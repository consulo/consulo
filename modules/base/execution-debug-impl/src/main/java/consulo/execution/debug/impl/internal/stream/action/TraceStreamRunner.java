package consulo.execution.debug.impl.internal.stream.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.impl.internal.stream.trace.TraceResultInterpreterImpl;
import consulo.execution.debug.impl.internal.stream.ui.ElementChooserImpl;
import consulo.execution.debug.impl.internal.stream.ui.EvaluationAwareTraceWindow;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.stream.ChainStatus;
import consulo.execution.debug.stream.TraceCompilationException;
import consulo.execution.debug.stream.TraceEvaluationException;
import consulo.execution.debug.stream.lib.LibrarySupportProvider;
import consulo.execution.debug.stream.psi.DebuggerPositionResolver;
import consulo.execution.debug.stream.psi.impl.DebuggerPositionResolverImpl;
import consulo.execution.debug.stream.trace.EvaluateExpressionTracer;
import consulo.execution.debug.stream.trace.StreamTracer;
import consulo.execution.debug.stream.ui.ChooserOption;
import consulo.execution.debug.stream.wrapper.StreamChain;
import consulo.fileEditor.FileEditorManager;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class TraceStreamRunner {
    @Nonnull
    public static TraceStreamRunner getInstance(@Nonnull Project project) {
        return project.getInstance(TraceStreamRunner.class);
    }
    
    private static final Logger LOG = Logger.getInstance(TraceStreamRunner.class);

    private static final ChainResolver CHAIN_RESOLVER = new ChainResolver();

    private final DebuggerPositionResolver myPositionResolver = new DebuggerPositionResolverImpl();

    public TraceStreamRunner() {
    }

    @Nonnull
    public ChainStatus getChainStatus(@Nullable XDebugSession session) {
        PsiElement element = session == null ? null : myPositionResolver.getNearestElementToBreakpoint(session);
        if (element == null) {
            return ChainStatus.NOT_FOUND;
        }
        else {
            return CHAIN_RESOLVER.tryFindChain(element);
        }
    }

    public void actionPerformed(@Nullable XDebugSession session) {
        if (session == null) {
            LOG.info("Session is null");
            return;
        }

        List<ChainResolver.StreamChainWithLibrary> chains = getChains(session);
        displayChains(session, chains);
    }

    private void displayChains(
        @Nonnull XDebugSession session,
        @Nonnull List<ChainResolver.StreamChainWithLibrary> chains
    ) {
        if (chains.isEmpty()) {
            LOG.warn("Stream chain is not built");
            return;
        }

        if (chains.size() == 1) {
            runTrace(chains.get(0).chain, chains.get(0).provider, session);
        }
        else {
            Project project = session.getProject();
            VirtualFile file = chains.get(0).chain.getContext().getContainingFile().getVirtualFile();
            OpenFileDescriptor descriptor = OpenFileDescriptorFactory.getInstance(project).newBuilder(file).build();
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
            if (editor == null) {
                throw new IllegalStateException("Cannot open editor for file: " + file.getName());
            }

            MyStreamChainChooser chooser = new MyStreamChainChooser(editor);
            List<StreamChainOption> options = chains.stream()
                .map(StreamChainOption::new)
                .collect(Collectors.toList());

            chooser.show(options, provider -> runTrace(provider.chain, provider.provider, session));
        }
    }

    @Nonnull
    private List<ChainResolver.StreamChainWithLibrary> getChains(@Nonnull XDebugSession session) {
        return ReadAction.compute(() -> {
            PsiElement element = myPositionResolver.getNearestElementToBreakpoint(session);
            if (element == null) {
                LOG.info("Element at cursor is not found");
                return Collections.emptyList();
            }
            else {
                return ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    () -> CHAIN_RESOLVER.getChains(element),
                    XDebuggerLocalize.actionCalculatingChainsBackgroundProgressTitle(),
                    true,
                    session.getProject()
                );
            }
        });
    }

    private static class MyStreamChainChooser extends ElementChooserImpl<StreamChainOption> {
        MyStreamChainChooser(@Nonnull Editor editor) {
            super(editor);
        }
    }

    private static class StreamChainOption implements ChooserOption {
        @Nonnull
        final StreamChain chain;
        @Nonnull
        final LibrarySupportProvider provider;

        StreamChainOption(@Nonnull ChainResolver.StreamChainWithLibrary chainWithLibrary) {
            this.chain = chainWithLibrary.chain;
            this.provider = chainWithLibrary.provider;
        }

        @Nonnull
        @Override
        public Stream<TextRange> rangeStream() {
            return Stream.of(
                new TextRange(chain.getQualifierExpression().getTextRange().getStartOffset(),
                    chain.getTerminationCall().getTextRange().getEndOffset())
            );
        }

        @Nonnull
        @Override
        public String getText() {
            return chain.getCompactText();
        }
    }

    private static void runTrace(
        @Nonnull StreamChain chain,
        @Nonnull LibrarySupportProvider provider,
        @Nonnull XDebugSession session
    ) {
        EvaluationAwareTraceWindow window = new EvaluationAwareTraceWindow(session, chain);

        window.show();

        Project project = session.getProject();
        var expressionBuilder = provider.getExpressionBuilder(project);
        var resultInterpreter = new TraceResultInterpreterImpl(provider.getLibrarySupport().getInterpreterFactory());
        var xValueInterpreter = provider.getXValueInterpreter(project);
        var debuggerLauncher = provider.getDebuggerCommandLauncher(session);
        StreamTracer tracer = new EvaluateExpressionTracer(session, expressionBuilder, resultInterpreter, xValueInterpreter);

        debuggerLauncher.launchDebuggerCommand(() -> {
            StreamTracer.Result result = null;
            try {
                result = tracer.trace(chain).get();
            }
            catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            if (result instanceof StreamTracer.Result.Evaluated) {
                StreamTracer.Result.Evaluated evaluated = (StreamTracer.Result.Evaluated) result;
                var resolvedTrace = evaluated.getResult().resolve(provider.getLibrarySupport().getResolverFactory());
                window.setTrace(resolvedTrace, debuggerLauncher, evaluated.getEvaluationContext(), provider.getCollectionTreeBuilder(project));
            }
            else if (result instanceof StreamTracer.Result.EvaluationFailed) {
                StreamTracer.Result.EvaluationFailed failed = (StreamTracer.Result.EvaluationFailed) result;
                window.setFailMessage(failed.getMessage());
                throw new TraceEvaluationException(failed.getMessage(), failed.getTraceExpression());
            }
            else if (result instanceof StreamTracer.Result.CompilationFailed) {
                StreamTracer.Result.CompilationFailed failed = (StreamTracer.Result.CompilationFailed) result;
                window.setFailMessage(failed.getMessage());
                throw new TraceCompilationException(failed.getMessage(), failed.getTraceExpression());
            }
            else if (result == StreamTracer.Result.Unknown.INSTANCE) {
                LOG.error("Unknown result");
            }
        });
    }
}
