/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.vcs.log.ui.actions;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.textCompletion.DefaultTextCompletionValueDescriptor;
import com.intellij.util.textCompletion.ValuesCompletionProvider;
import com.intellij.util.ui.UIUtil;
import com.intellij.vcs.log.VcsLogRefs;
import com.intellij.vcs.log.VcsRef;
import com.intellij.vcs.log.ui.VcsLogColorManager;
import com.intellij.vcs.log.ui.frame.VcsLogGraphTable;
import com.intellij.vcsUtil.VcsImplUtil;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GoToHashOrRefPopup {
  private static final Logger LOG = Logger.getInstance(GoToHashOrRefPopup.class);

  @Nonnull
  private final TextFieldWithProgress myTextField;
  @Nonnull
  private final Function<String, Future> myOnSelectedHash;
  @Nonnull
  private final Function<VcsRef, Future> myOnSelectedRef;
  @Nonnull
  private final JBPopup myPopup;
  @Nullable private Future myFuture;
  @Nullable private VcsRef mySelectedRef;

  public GoToHashOrRefPopup(@Nonnull final Project project,
                            @Nonnull VcsLogRefs variants,
                            Collection<VirtualFile> roots,
                            @Nonnull Function<String, Future> onSelectedHash,
                            @Nonnull Function<VcsRef, Future> onSelectedRef,
                            @Nonnull VcsLogColorManager colorManager,
                            @Nonnull Comparator<VcsRef> comparator) {
    myOnSelectedHash = onSelectedHash;
    myOnSelectedRef = onSelectedRef;
    myTextField =
      new TextFieldWithProgress(project, new VcsRefCompletionProvider(project, variants, roots, colorManager, comparator)) {
        @Override
        public void onOk() {
          if (myFuture == null) {
            final Future future = ((mySelectedRef == null || (!mySelectedRef.getName().equals(getText().trim())))
                                   ? myOnSelectedHash.fun(getText().trim())
                                   : myOnSelectedRef.fun(mySelectedRef));
            myFuture = future;
            showProgress();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
              try {
                future.get();
                okPopup();
              }
              catch (CancellationException ex) {
                cancelPopup();
              }
              catch (InterruptedException ex) {
                cancelPopup();
              }
              catch (ExecutionException ex) {
                LOG.error(ex);
                cancelPopup();
              }
            });
          }
        }
      };
    myTextField.setAlignmentX(Component.LEFT_ALIGNMENT);

    JBLabel label = new JBLabel("Enter hash or branch/tag name:");
    label.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD));
    label.setAlignmentX(Component.LEFT_ALIGNMENT);

    JPanel panel = new JPanel();
    BoxLayout layout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
    panel.setLayout(layout);
    panel.add(label);
    panel.add(myTextField);
    panel.setBorder(new EmptyBorder(2, 2, 2, 2));

    myPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, myTextField.getPreferableFocusComponent())
      .setCancelOnClickOutside(true).setCancelOnWindowDeactivation(true).setCancelKeyEnabled(true).setRequestFocus(true).createPopup();
    myPopup.addListener(new JBPopupListener.Adapter() {
      @Override
      public void onClosed(LightweightWindowEvent event) {
        if (!event.isOk()) {
          if (myFuture != null) {
            myFuture.cancel(true);
          }
        }
        myFuture = null;
        myTextField.hideProgress();
      }
    });
  }

  private void cancelPopup() {
    ApplicationManager.getApplication().invokeLater(() -> myPopup.cancel());
  }

  private void okPopup() {
    ApplicationManager.getApplication().invokeLater(() -> myPopup.closeOk(null));
  }

  public void show(@Nonnull JComponent anchor) {
    myPopup.showInCenterOf(anchor);
  }

  private class VcsRefCompletionProvider extends ValuesCompletionProvider<VcsRef> {
    private static final int TIMEOUT = 100;
    @Nonnull
    private final VcsLogRefs myRefs;
    @Nonnull
    private final Collection<VirtualFile> myRoots;

    public VcsRefCompletionProvider(@Nonnull Project project,
                                    @Nonnull VcsLogRefs refs,
                                    @Nonnull Collection<VirtualFile> roots,
                                    @Nonnull VcsLogColorManager colorManager,
                                    @Nonnull Comparator<VcsRef> comparator) {
      super(new VcsRefDescriptor(project, colorManager, comparator, roots), ContainerUtil.emptyList());
      myRefs = refs;
      myRoots = roots;
    }

    @Override
    public void fillCompletionVariants(@Nonnull CompletionParameters parameters,
                                       @Nonnull String prefix,
                                       @Nonnull CompletionResultSet result) {
      addValues(result, filterAndSort(result, myRefs.getBranches().stream()));

      Future<List<VcsRef>> future = ApplicationManager.getApplication()
        .executeOnPooledThread(() -> filterAndSort(result, myRefs.stream().filter(ref -> !ref.getType().isBranch())));
      while (true) {
        try {
          List<VcsRef> tags = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
          if (tags != null) {
            addValues(result, tags);
            break;
          }
        }
        catch (InterruptedException | CancellationException e) {
          break;
        }
        catch (TimeoutException ignored) {
        }
        catch (ExecutionException e) {
          LOG.error(e);
          break;
        }
        ProgressManager.checkCanceled();
      }
      result.stopHere();
    }

    public void addValues(@Nonnull CompletionResultSet result, @Nonnull Collection<? extends VcsRef> values) {
      for (VcsRef completionVariant : values) {
        result.addElement(installInsertHandler(myDescriptor.createLookupBuilder(completionVariant)));
      }
    }

    @Nonnull
    private List<VcsRef> filterAndSort(@Nonnull CompletionResultSet result, @Nonnull Stream<VcsRef> stream) {
      return ContainerUtil
        .sorted(stream.filter(ref -> myRoots.contains(ref.getRoot()) && result.getPrefixMatcher().prefixMatches(ref.getName()))
                  .collect(Collectors.toList()), myDescriptor);
    }
  }

  private class VcsRefDescriptor extends DefaultTextCompletionValueDescriptor<VcsRef> {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final VcsLogColorManager myColorManager;
    @Nonnull
    private final Comparator<VcsRef> myReferenceComparator;
    @Nonnull
    private final Map<VirtualFile, String> myCachedRootNames = ContainerUtil.newHashMap();

    private VcsRefDescriptor(@Nonnull Project project,
                             @Nonnull VcsLogColorManager manager,
                             @Nonnull Comparator<VcsRef> comparator,
                             @Nonnull Collection<VirtualFile> roots) {
      myProject = project;
      myColorManager = manager;
      myReferenceComparator = comparator;

      for (VirtualFile root : roots) {
        String text = VcsImplUtil.getShortVcsRootName(myProject, root);
        myCachedRootNames.put(root, text);
      }
    }

    @Nonnull
    @Override
    public LookupElementBuilder createLookupBuilder(@Nonnull VcsRef item) {
      LookupElementBuilder lookupBuilder = super.createLookupBuilder(item);
      if (myColorManager.isMultipleRoots()) {
        lookupBuilder = lookupBuilder
          .withTypeText(getTypeText(item),
                        ImageEffects.colorFilled(15, 15, TargetAWT.from(VcsLogGraphTable.getRootBackgroundColor(item.getRoot(), myColorManager))),
                        true);
      }
      return lookupBuilder;
    }

    @Nonnull
    @Override
    public String getLookupString(@Nonnull VcsRef item) {
      return item.getName();
    }

    @Nullable
    @Override
    protected String getTailText(@Nonnull VcsRef item) {
      if (!myColorManager.isMultipleRoots()) return null;
      return "";
    }

    @Nullable
    @Override
    protected String getTypeText(@Nonnull VcsRef item) {
      if (!myColorManager.isMultipleRoots()) return null;
      String text = myCachedRootNames.get(item.getRoot());
      if (text == null) {
        return VcsImplUtil.getShortVcsRootName(myProject, item.getRoot());
      }
      return text;
    }

    @Override
    public int compare(VcsRef item1, VcsRef item2) {
      return myReferenceComparator.compare(item1, item2);
    }

    @Nullable
    @Override
    protected InsertHandler<LookupElement> createInsertHandler(@Nonnull VcsRef item) {
      return (context, item1) -> {
        mySelectedRef = (VcsRef)item1.getObject();
        ApplicationManager.getApplication().invokeLater(() -> {
          // handleInsert is called in the middle of some other code that works with editor
          // (see CodeCompletionHandlerBase.insertItem)
          // for example, scrolls editor
          // problem is that in onOk we make text field not editable
          // by some reason this is done by disposing its editor and creating a new one
          // so editor gets disposed here and CodeCompletionHandlerBase can not finish doing whatever it is doing with it
          // I counter this by invoking onOk in invokeLater
          myTextField.onOk();
        });
      };
    }
  }
}
