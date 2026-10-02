// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.client.generator;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.codeEditor.Editor;
import consulo.endpoint.EndpointChangeTrackerUtil;
import consulo.endpoint.client.generator.ClientExample;
import consulo.endpoint.client.generator.ClientGenerator;
import consulo.endpoint.client.generator.ClientGeneratorOpenInScratchService;
import consulo.endpoint.client.generator.ClientGeneratorSetting;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.oas.OasSpecificationProvider;
import consulo.endpoint.oas.OpenApiSpecification;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.endpoint.url.reference.UrlPathContext;
import consulo.endpoint.url.reference.UrlPathContextUtil;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ServiceImpl
@Singleton
public final class ClientGeneratorOpenInScratchServiceImpl implements ClientGeneratorOpenInScratchService {
    private static final Object NO_INPUT = new Object();

    private final Project myProject;
    private final ProgressBuilderFactory myProgressBuilderFactory;
    private final ScratchFileService myScratchFileService;
    private final PsiManager myPsiManager;
    private final CodeStyleManager myCodeStyleManager;

    @Inject
    public ClientGeneratorOpenInScratchServiceImpl(
        Project project,
        ProgressBuilderFactory progressBuilderFactory,
        ScratchFileService scratchFileService,
        PsiManager psiManager,
        CodeStyleManager codeStyleManager
    ) {
        myProject = project;
        myProgressBuilderFactory = progressBuilderFactory;
        myScratchFileService = scratchFileService;
        myPsiManager = psiManager;
        myCodeStyleManager = codeStyleManager;
    }

    @Override
    public CompletableFuture<?> createScratchFileWithoutEndpointsChangeTracking(ClientGenerator clientGenerator, OpenApiSpecification oas) {
        return EndpointChangeTrackerUtil.withExpectedChangesAsync(myProject, () -> createScratchFileFuture(clientGenerator, oas));
    }

    @Override
    public CompletableFuture<?> createScratchFile(ClientGenerator clientGenerator, Editor editor, UrlPathContext urlPathContext) {
        return Coroutine
            .first(CompletableFutureStep.<Object, Optional<OpenApiSpecification>>await(
                ignored -> computeOpenApiSpecificationWithProgress(urlPathContext)
            ))
            .then(UIAction.<Optional<OpenApiSpecification>, Optional<OpenApiSpecification>>apply(oas -> {
                if (oas.isEmpty()) {
                    CommonRefactoringUtil.showErrorHint(
                        myProject,
                        editor,
                        EndpointLocalize.clientGeneratorOpenInScratchError(),
                        EndpointLocalize.clientGeneratorOpenInScratchErrorTitle(),
                        null
                    );
                }
                return oas;
            }))
            .then(CompletableFutureStep.<Optional<OpenApiSpecification>, Optional<SmartPsiElementPointer<PsiFile>>>await(
                oas -> oas.map(it -> createScratchFileFuture(clientGenerator, it))
                    .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()))
            ))
            .runAsync(newScope(), NO_INPUT)
            .toFuture();
    }

    @Override
    public CompletableFuture<?> createScratchFile(ClientGenerator clientGenerator, OpenApiSpecification oas) {
        return createScratchFileFuture(clientGenerator, oas);
    }

    private CompletableFuture<Optional<OpenApiSpecification>> computeOpenApiSpecificationWithProgress(UrlPathContext urlPathContext) {
        return myProgressBuilderFactory.newProgressBuilder(myProject, EndpointLocalize.clientGeneratorProgressTitleOpenInScratch())
            .cancelable()
            .modal()
            .execute(
                myProject.getUIAccess(),
                () -> Coroutine.first(ReadLock.<Object, Optional<OpenApiSpecification>>apply(
                    ignored -> Optional.ofNullable(getOpenApiSpecification(urlPathContext))
                ))
            );
    }

    @RequiredReadAction
    private @Nullable OpenApiSpecification getOpenApiSpecification(UrlPathContext urlPathContext) {
        for (UrlTargetInfo resolvedTarget : UrlPathContextUtil.resolveTargets(urlPathContext, myProject)) {
            OpenApiSpecification specification = OasSpecificationProvider.findOasSpecification(resolvedTarget);
            if (specification != null) {
                return specification;
            }
        }
        return null;
    }

    private CompletableFuture<Optional<SmartPsiElementPointer<PsiFile>>> createScratchFileFuture(
        ClientGenerator clientGenerator,
        OpenApiSpecification oas
    ) {
        return Coroutine
            .first(ReadLock.<Object, Optional<ClientExample>>apply(
                ignored -> Optional.ofNullable(generateClientWithBoilerplate(clientGenerator, oas))
            ))
            .then(WriteLock.<Optional<ClientExample>, Optional<VirtualFile>>apply(
                clientExample -> clientExample.map(this::writeScratchFile)
            ))
            .then(ReadLock.<Optional<VirtualFile>, Optional<ScratchPointer>>apply(
                scratchFile -> scratchFile.flatMap(this::createScratchPointer)
            ))
            .then(UIAction.<Optional<ScratchPointer>, Optional<ScratchPointer>>apply(scratch -> {
                if (scratch.isPresent()) {
                    reformat(scratch.get().pointer());
                }
                return scratch;
            }))
            .then(UIAction.<Optional<ScratchPointer>, Optional<SmartPsiElementPointer<PsiFile>>>apply(scratch -> {
                if (scratch.isPresent()) {
                    VirtualFile file = scratch.get().file();
                    if (file.isValid()) {
                        OpenFileDescriptorFactory.getInstance(myProject).newBuilder(file).build().navigate(true);
                    }
                }
                return scratch.map(ScratchPointer::pointer);
            }))
            .runAsync(newScope(), NO_INPUT)
            .toFuture();
    }

    @RequiredWriteAction
    private @Nullable VirtualFile writeScratchFile(ClientExample clientExample) {
        FileType fileType = clientExample.getFileType();
        String extension = fileType.getDefaultExtension();
        String fileName = extension.isEmpty() ? CLIENT_EXAMPLE_SCRATCH_PREFIX : CLIENT_EXAMPLE_SCRATCH_PREFIX + "." + extension;
        Language language = fileType instanceof LanguageFileType languageFileType ? languageFileType.getLanguage() : null;

        VirtualFile scratchFile;
        try {
            scratchFile = myScratchFileService.findFile(
                RootType.findById("scratches"),
                fileName,
                ScratchFileService.Option.create_new_always
            );
        }
        catch (IOException | RuntimeException | AssertionError e) {
            return null;
        }

        myScratchFileService.getScratchesMapping().setMapping(scratchFile, language);
        try {
            VirtualFileUtil.saveText(scratchFile, clientExample.getText());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return scratchFile;
    }

    @RequiredReadAction
    private Optional<ScratchPointer> createScratchPointer(VirtualFile scratchFile) {
        PsiFile psiFile = myPsiManager.findFile(scratchFile);
        return psiFile != null ? Optional.of(new ScratchPointer(SmartPointerManager.createPointer(psiFile), scratchFile)) : Optional.empty();
    }

    @RequiredUIAccess
    private void reformat(SmartPsiElementPointer<PsiFile> pointer) {
        WriteCommandAction.runWriteCommandAction(myProject, EndpointLocalize.commandExportClientExample().get(), null, () -> {
            PsiFile psiFile = pointer.getElement();
            if (psiFile == null) {
                return;
            }
            myCodeStyleManager.reformat(psiFile);
        });
    }

    @RequiredReadAction
    private static @Nullable ClientExample generateClientWithBoilerplate(ClientGenerator clientGenerator, OpenApiSpecification oas) {
        ClientGeneratorSetting clientSettings = clientGenerator.getAvailableClientSettings().getActualClientSettings();
        synchronized (clientSettings) {
            boolean previousSettings = clientSettings.getBoilerplate();
            clientSettings.setBoilerplate(true);
            try {
                return clientGenerator.generate(oas);
            }
            finally {
                clientSettings.setBoilerplate(previousSettings);
            }
        }
    }

    private CoroutineScope newScope() {
        CoroutineScope scope = CoroutineScope.of(myProject.coroutineContext());
        scope.putCopyableUserData(UIAccess.KEY, myProject.getUIAccess());
        return scope;
    }

    private record ScratchPointer(SmartPsiElementPointer<PsiFile> pointer, VirtualFile file) {
    }
}
