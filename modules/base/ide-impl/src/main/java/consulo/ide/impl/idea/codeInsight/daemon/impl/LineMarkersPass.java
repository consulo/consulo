// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.gutter.*;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.internal.highlight.Divider;
import consulo.language.editor.util.CollectHighlightsUtil;
import consulo.language.file.FileViewProvider;
import consulo.document.DocumentWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.NotNullList;
import gnu.trove.TIntObjectHashMap;

import jakarta.annotation.Nonnull;
import java.util.*;
import java.util.function.BiConsumer;

public class LineMarkersPass extends TextEditorHighlightingPass {
  private static final Logger LOG = Logger.getInstance(LineMarkersPass.class);

  private volatile List<LineMarkerInfo<PsiElement>> myMarkers = Collections.emptyList();

  @Nonnull
  private final PsiFile myFile;
  @Nonnull
  private final TextRange myPriorityBounds;
  @Nonnull
  private final TextRange myRestrictRange;

  LineMarkersPass(@Nonnull Project project,
                  @Nonnull PsiFile file,
                  @Nonnull Document document,
                  @Nonnull TextRange priorityBounds,
                  @Nonnull TextRange restrictRange) {
    super(project, document, false);
    myFile = file;
    myPriorityBounds = priorityBounds;
    myRestrictRange = restrictRange;
  }

  @Nonnull
  @Override
  public Document getDocument() {
    //noinspection ConstantConditions
    return super.getDocument();
  }

  @Override
  public void doApplyInformationToEditor() {
    try {
      LineMarkersUtil.setLineMarkersToEditor(myProject, getDocument(), myRestrictRange, myMarkers, getId());
    }
    catch (IndexNotReadyException ignored) {
    }
  }

  @RequiredReadAction
  @Override
  public void doCollectInformation(@Nonnull ProgressIndicator progress) {
    final List<LineMarkerInfo<PsiElement>> lineMarkers = new ArrayList<>();
    FileViewProvider viewProvider = myFile.getViewProvider();
    for (Language language : viewProvider.getLanguages()) {
      final PsiFile root = viewProvider.getPsi(language);
      HighlightingLevelManager highlightingLevelManager = HighlightingLevelManager.getInstance(myProject);
      if (!highlightingLevelManager.shouldHighlight(root)) continue;
      Divider.divideInsideAndOutsideInOneRoot(root, myRestrictRange, myPriorityBounds, elements -> {
        Collection<LineMarkerProvider> providers = getMarkerProviders(root, language, myProject);
        List<LineMarkerProvider> providersList = new ArrayList<>(providers);

        queryProviders(elements.inside, root, providersList, (element, info) -> {
          lineMarkers.add(info);
          ApplicationManager.getApplication().invokeLater(() -> {
            if (isValid()) {
              LineMarkersUtil.addLineMarkerToEditorIncrementally(myProject, getDocument(), info);
            }
          }, myProject.getDisposed());
        });
        queryProviders(elements.outside, root, providersList, (element, info) -> lineMarkers.add(info));
      });
    }

    myMarkers = mergeLineMarkers(lineMarkers, getDocument());
    if (LOG.isDebugEnabled()) {
      LOG.debug("LineMarkersPass.doCollectInformation. lineMarkers: " + lineMarkers + "; merged: " + myMarkers);
    }
  }

  @Nonnull
  private static List<LineMarkerInfo<PsiElement>> mergeLineMarkers(@Nonnull List<LineMarkerInfo<PsiElement>> markers,
                                                                   @Nonnull Document document) {
    List<MergeableLineMarkerInfo<PsiElement>> forMerge = new ArrayList<>();
    TIntObjectHashMap<List<MergeableLineMarkerInfo<PsiElement>>> sameLineMarkers = new TIntObjectHashMap<>();

    for (int i = markers.size() - 1; i >= 0; i--) {
      LineMarkerInfo<PsiElement> marker = markers.get(i);
      if (marker instanceof MergeableLineMarkerInfo) {
        MergeableLineMarkerInfo<PsiElement> mergeable = (MergeableLineMarkerInfo<PsiElement>)marker;
        forMerge.add(mergeable);
        markers.remove(i);

        int line = document.getLineNumber(marker.startOffset);
        List<MergeableLineMarkerInfo<PsiElement>> infos = sameLineMarkers.get(line);
        if (infos == null) {
          infos = new ArrayList<>();
          sameLineMarkers.put(line, infos);
        }
        infos.add(mergeable);
      }
    }

    if (forMerge.isEmpty()) return markers;

    List<LineMarkerInfo<PsiElement>> result = new ArrayList<>(markers);

    sameLineMarkers.forEachValue(infos -> result.addAll(MergeableLineMarkerInfo.merge(infos)));

    return result;
  }

  @Nonnull
  public static List<LineMarkerProvider> getMarkerProviders(@Nonnull PsiFile psiFile,
                                                            @Nonnull Language language,
                                                            @Nonnull Project project) {
    DumbService dumbService = DumbService.getInstance(project);
    LineMarkerSettings settings = LineMarkerSettings.getInstance();

    List<LineMarkerProvider> forLanguage = LineMarkerProvider.forLanguage(language);
    List<LineMarkerProvider> result = new ArrayList<>();

    dumbService.forEachDumAwareness(forLanguage, (provider) -> {
      // provider disable by settings
      if (provider instanceof LineMarkerProviderDescriptor && !settings.isEnabled((GutterIconDescriptor)provider)) {
        return;
      }

      // provider is disabled by own check
      if (!provider.isAvailable(psiFile)) {
        return;
      }

      result.add(provider);
    });

    return result;
  }

  @RequiredReadAction
  @SuppressWarnings({"deprecation", "unchecked"})
  private void queryProviders(@Nonnull List<PsiElement> elements,
                              @Nonnull PsiFile containingFile,
                              @Nonnull List<? extends LineMarkerProvider> providers,
                              @Nonnull BiConsumer<? super PsiElement, ? super LineMarkerInfo<PsiElement>> consumer) {
    myProject.getApplication().assertReadAccessAllowed();
    Set<PsiFile> visitedInjectedFiles = new HashSet<>();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < elements.size(); i++) {
      PsiElement element = elements.get(i);

      //noinspection ForLoopReplaceableByForEach
      for (int j = 0; j < providers.size(); j++) {
        ProgressManager.checkCanceled();
        LineMarkerProvider provider = providers.get(j);
        LineMarkerInfo<PsiElement> info;
        try {
          info = provider.getLineMarkerInfo(element);
        }
        catch (ProcessCanceledException | IndexNotReadyException e) {
          throw e;
        }
        catch (Exception e) {
          LOG.error("During querying provider " + provider + " (" + provider.getClass() + ")", e);
          continue;
        }
        if (info != null) {
          consumer.accept(element, info);
        }
      }

      queryLineMarkersForInjected(element, containingFile, visitedInjectedFiles, consumer);
    }

    List<LineMarkerInfo<PsiElement>> slowLineMarkers = new NotNullList<>();
    //noinspection ForLoopReplaceableByForEach
    for (int j = 0; j < providers.size(); j++) {
      ProgressManager.checkCanceled();
      LineMarkerProvider provider = providers.get(j);
      try {
        provider.collectSlowLineMarkers(elements, (List)slowLineMarkers);
      }
      catch (ProcessCanceledException | IndexNotReadyException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error(e);
        continue;
      }

      if (!slowLineMarkers.isEmpty()) {
        //noinspection ForLoopReplaceableByForEach
        for (int k = 0; k < slowLineMarkers.size(); k++) {
          LineMarkerInfo<PsiElement> slowInfo = slowLineMarkers.get(k);
          PsiElement element = slowInfo.getElement();
          consumer.accept(element, slowInfo);
        }
        slowLineMarkers.clear();
      }
    }
  }

  @RequiredReadAction
  private void queryLineMarkersForInjected(@Nonnull PsiElement element,
                                           @Nonnull final PsiFile containingFile,
                                           @Nonnull Set<? super PsiFile> visitedInjectedFiles,
                                           @Nonnull final BiConsumer<? super PsiElement, ? super LineMarkerInfo<PsiElement>> consumer) {
    final InjectedLanguageManager manager = InjectedLanguageManager.getInstance(containingFile.getProject());
    if (manager.isInjectedFragment(containingFile)) return;

    InjectedLanguageManager.getInstance(containingFile.getProject()).enumerateEx(element, containingFile, false, (injectedPsi, places) -> {
      if (!visitedInjectedFiles.add(injectedPsi)) return; // there may be several concatenated literals making the one injected file
      final Project project = injectedPsi.getProject();
      Document document = PsiDocumentManager.getInstance(project).getCachedDocument(injectedPsi);
      if (!(document instanceof DocumentWindow)) return;
      List<PsiElement> injElements = CollectHighlightsUtil.getElementsInRange(injectedPsi, 0, injectedPsi.getTextLength());
      final List<LineMarkerProvider> providers = getMarkerProviders(injectedPsi, injectedPsi.getLanguage(), project);

      queryProviders(injElements, injectedPsi, providers, (injectedElement, injectedMarker) -> {
        GutterIconRenderer gutterRenderer = injectedMarker.createGutterRenderer();
        TextRange injectedRange = new TextRange(injectedMarker.startOffset, injectedMarker.endOffset);
        List<TextRange> editables = manager.intersectWithAllEditableFragments(injectedPsi, injectedRange);
        for (TextRange editable : editables) {
          TextRange hostRange = manager.injectedToHost(injectedPsi, editable);
          Image icon = gutterRenderer == null ? null : gutterRenderer.getIcon();
          GutterIconNavigationHandler<PsiElement> navigationHandler = injectedMarker.getNavigationHandler();
          LineMarkerInfo<PsiElement> converted =
            new LineMarkerInfo<>(injectedElement,
                                 hostRange,
                                 icon,
                                 injectedMarker.updatePass,
                                 e -> injectedMarker.getLineMarkerTooltip(),
                                 navigationHandler,
                                 GutterIconRenderer.Alignment.RIGHT);
          consumer.accept(injectedElement, converted);
        }
      });
    });
  }

  @Nonnull
  @RequiredReadAction
  public static Collection<LineMarkerInfo<PsiElement>> queryLineMarkers(@Nonnull PsiFile file, @Nonnull Document document) {
    if (file.getNode() == null) {
      // binary file? see IDEADEV-2809
      return Collections.emptyList();
    }
    LineMarkersPass pass = new LineMarkersPass(file.getProject(), file, document, file.getTextRange(), file.getTextRange());
    pass.doCollectInformation(new EmptyProgressIndicator());
    return pass.myMarkers;
  }

  @Override
  public String toString() {
    return super.toString() + "; myBounds: " + myPriorityBounds;
  }
}
