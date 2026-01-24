// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.action;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.component.ProcessCanceledException;
import consulo.execution.debug.stream.ChainStatus;
import consulo.execution.debug.stream.lib.LibrarySupportProvider;
import consulo.execution.debug.stream.wrapper.StreamChain;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiInvalidElementAccessException;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Helps {@link TraceStreamAction} understand if there is a suitable chain under the debugger position or not.
 */
public class ChainResolver {
    private static final Logger LOG = Logger.getInstance(ChainResolver.class);

    private final AtomicReference<ChainsSearchResult> mySearchResult = new AtomicReference<>(new ChainsSearchResult(0, -1, null));

    @Nonnull
    public ChainStatus tryFindChain(@Nonnull PsiElement elementAtDebugger) {
        ChainsSearchResult result = mySearchResult.get();
        if (result.isSuitableFor(elementAtDebugger)) {
            return result.getChainsStatus();
        }

        result = ChainsSearchResult.of(elementAtDebugger);
        checkChainsExistenceInBackground(elementAtDebugger, result);
        mySearchResult.set(result);
        return result.getChainsStatus();
    }

    @Nonnull
    List<StreamChainWithLibrary> getChains(@Nonnull PsiElement elementAtDebugger) {
        ChainsSearchResult result = mySearchResult.get();
        if (!result.isSuitableFor(elementAtDebugger) || result.getChainsStatus() != ChainStatus.FOUND) {
            LOG.error("Cannot build chains: " + result.getChainsStatus());
            return Collections.emptyList();
        }

        String elementLanguageId = elementAtDebugger.getLanguage().getID();
        LibrarySupportProvider provider = LibrarySupportProvider.EP_NAME.findFirstSafe(it ->
            it.getLanguageId().equals(elementLanguageId) && it.getChainBuilder().isChainExists(elementAtDebugger)
        );
        if (provider != null) {
            return provider.getChainBuilder().build(elementAtDebugger)
                .stream()
                .map(chain -> new StreamChainWithLibrary(chain, provider))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public static class StreamChainWithLibrary {
        @Nonnull
        public final StreamChain chain;
        @Nonnull
        public final LibrarySupportProvider provider;

        StreamChainWithLibrary(@Nonnull StreamChain chain, @Nonnull LibrarySupportProvider provider) {
            this.chain = chain;
            this.provider = provider;
        }
    }

    private static class ChainsSearchResult {
        private final long elementHash;
        private final long offset;
        private final long fileModificationStamp;

        @Nonnull
        private volatile ChainStatus chainsStatus = ChainStatus.COMPUTING;

        ChainsSearchResult(long elementHash, long offset, @Nullable PsiFile containingFile) {
            this.elementHash = elementHash;
            this.offset = offset;
            this.fileModificationStamp = getModificationStamp(containingFile);
        }

        @Nonnull
        ChainStatus getChainsStatus() {
            return chainsStatus;
        }

        void updateStatus(boolean found) {
            LOG.assertTrue(ChainStatus.COMPUTING == chainsStatus);
            chainsStatus = found ? ChainStatus.FOUND : ChainStatus.NOT_FOUND;
        }

        void markUnsupportedLanguage() {
            LOG.assertTrue(ChainStatus.COMPUTING == chainsStatus);
            chainsStatus = ChainStatus.LANGUAGE_NOT_SUPPORTED;
        }

        boolean isSuitableFor(@Nonnull PsiElement element) {
            return elementHash == element.hashCode() &&
                offset == element.getTextOffset() &&
                fileModificationStamp == getModificationStamp(element.getContainingFile());
        }

        private static long getModificationStamp(@Nullable PsiFile file) {
            return file == null ? -1 : file.getModificationStamp();
        }

        @Nonnull
        static ChainsSearchResult of(@Nonnull PsiElement element) {
            return new ChainsSearchResult(element.hashCode(), element.getTextOffset(), element.getContainingFile());
        }
    }

    private static void checkChainsExistenceInBackground(
        @Nonnull PsiElement elementAtDebugger,
        @Nonnull ChainsSearchResult searchResult
    ) {
        List<LibrarySupportProvider> extensions = forLanguage(elementAtDebugger.getLanguage());
        if (extensions.isEmpty()) {
            searchResult.markUnsupportedLanguage();
        }
        else {
            ReadAction.nonBlocking(() -> {
                    boolean found = false;
                    for (LibrarySupportProvider provider : extensions) {
                        try {
                            if (provider.getChainBuilder().isChainExists(elementAtDebugger)) {
                                found = true;
                                break;
                            }
                        }
                        catch (ProcessCanceledException e) {
                            throw e;
                        }
                        catch (PsiInvalidElementAccessException ignored) {
                        }
                        catch (Throwable e) {
                            LOG.error(e);
                        }
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Chains found:" + found);
                    }
                    searchResult.updateStatus(found);
                })
                .inSmartMode(elementAtDebugger.getProject())
                .executeSynchronously();
        }
    }

    @Nonnull
    private static List<LibrarySupportProvider> forLanguage(@Nonnull Language language) {
        return Application.get().getExtensionList(LibrarySupportProvider.class)
            .stream()
            .filter(it -> Objects.equals(language.getID(), it.getLanguageId()))
            .collect(Collectors.toList());
    }
}
