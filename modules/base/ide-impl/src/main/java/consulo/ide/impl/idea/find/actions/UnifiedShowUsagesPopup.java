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
package consulo.ide.impl.idea.find.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.fileEditor.FileEditorManager;
import consulo.find.FindManager;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.ide.impl.idea.find.findUsages.FindUsagesManager;
import consulo.ide.impl.idea.find.impl.FindManagerImpl;
import consulo.ide.impl.idea.find.impl.UnifiedFindPopupPanel;
import consulo.ide.impl.idea.usages.impl.UsageViewManagerImpl;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.RelativePoint2D;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemRender;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.usage.TextChunk;
import consulo.usage.Usage;
import consulo.usage.UsageInfo2UsageAdapter;
import consulo.usage.UsageTarget;
import consulo.usage.UsageViewManager;
import consulo.usage.localize.UsageLocalize;
import consulo.usage.rule.UsageInFile;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePresentation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class UnifiedShowUsagesPopup {
    private static final Key<JBPopup> HINT_KEY = Key.create("UnifiedShowUsagesPopup.hint");

    private static final Comparator<UnifiedShowUsagesItem> ITEM_ORDER =
        Comparator.comparingInt((UnifiedShowUsagesItem item) -> item.kind().ordinal())
            .thenComparing(UnifiedShowUsagesItem::path)
            .thenComparingInt(UnifiedShowUsagesItem::offset);

    private final Project myProject;
    private final Editor myEditor;
    private final FindUsagesManager myFindUsagesManager;
    private final FindUsagesHandler myHandler;
    private final RelativePoint2D myPosition;
    private final int myMaxUsages;

    private final Object myLock = new Object();
    private final List<UnifiedShowUsagesItem> myItems = new ArrayList<>();
    private boolean myHasMore;
    private int myOutOfScopeUsages;

    private volatile @Nullable FindUsagesOptions myOptions;
    private volatile String myTitle = "";

    private UnifiedShowUsagesPopup(
        Project project,
        Editor editor,
        FindUsagesManager findUsagesManager,
        FindUsagesHandler handler,
        RelativePoint2D position,
        int maxUsages
    ) {
        myProject = project;
        myEditor = editor;
        myFindUsagesManager = findUsagesManager;
        myHandler = handler;
        myPosition = position;
        myMaxUsages = maxUsages;
    }

    @RequiredUIAccess
    public static void show(Editor editor, Project project, PsiElement element, @Nullable RelativePoint2D point) {
        FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(project)).getFindUsagesManager();
        FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, false);
        if (handler == null) {
            return;
        }

        RelativePoint2D position = point != null ? point : EditorPopupHelper.getInstance().guessBestPopupLocation(editor);

        DataManager dataManager = DataManager.getInstance();
        DataContext dataContext = dataManager.createAsyncDataContext(dataManager.getDataContext(editor.getUIComponent()));

        UIAccess uiAccess = UIAccess.current();

        UnifiedShowUsagesPopup popup =
            new UnifiedShowUsagesPopup(project, editor, findUsagesManager, handler, position, ShowUsagesAction.getUsagesPageSize());
        popup.start(uiAccess, () -> ReadAction.compute(() -> handler.getFindUsagesOptions(dataContext)));
    }

    private void start(UIAccess uiAccess, FindUsagesOptions options) {
        start(uiAccess, () -> options);
    }

    private void start(UIAccess uiAccess, Supplier<FindUsagesOptions> options) {
        new Task.Backgroundable(myProject, FindLocalize.progressTitleFindingUsages()) {
            @Override
            public void run(ProgressIndicator indicator) {
                boolean delivered = false;
                try {
                    collect(options.get());
                    if (uiAccess.isValid()) {
                        uiAccess.give(UnifiedShowUsagesPopup.this::showResult);
                        delivered = true;
                    }
                }
                finally {
                    if (!delivered) {
                        dispose(takeItems());
                    }
                }
            }
        }.queue();
    }

    @RequiredUIAccess
    private void restart(int maxUsages, FindUsagesOptions options) {
        UnifiedShowUsagesPopup popup = new UnifiedShowUsagesPopup(myProject, myEditor, myFindUsagesManager, myHandler, myPosition, maxUsages);
        popup.start(UIAccess.current(), options);
    }

    private void collect(FindUsagesOptions options) {
        myOptions = options;
        SearchScope searchScope = options.searchScope;
        myTitle = ReadAction.compute(() -> myFindUsagesManager.createPresentation(myHandler, options).getTabText());

        PsiElement[] primaryElements = ReadAction.compute(myHandler::getPrimaryElements);
        PsiElement[] secondaryElements = ReadAction.compute(myHandler::getSecondaryElements);
        UsageTarget[] targets = ReadAction.compute(this::createSelfUsageTargets);
        ColorValue matchBackground = UnifiedFindPopupPanel.getSearchMatchBackground();

        FindUsagesManager.processUsages(
            myHandler,
            primaryElements,
            secondaryElements,
            usage -> accept(usage, searchScope, targets, matchBackground),
            options
        );
    }

    @RequiredReadAction
    private UsageTarget[] createSelfUsageTargets() {
        PsiElement element = myHandler.getPsiElement();
        if (!(element instanceof NavigationItem) || !element.isValid()) {
            return UsageTarget.EMPTY_ARRAY;
        }
        return new UsageTarget[]{new PsiElement2UsageTargetAdapter(element)};
    }

    private boolean accept(Usage usage, SearchScope searchScope, UsageTarget[] targets, @Nullable ColorValue matchBackground) {
        if (myEditor.isDisposed() || myProject.isDisposed()) {
            return false;
        }

        if (!ReadAction.compute(() -> UsageViewManagerImpl.isInScope(usage, searchScope))) {
            synchronized (myLock) {
                myOutOfScopeUsages++;
            }
            return true;
        }

        if (UsageViewManager.isSelfUsage(usage, targets)) {
            return true;
        }

        synchronized (myLock) {
            if (myItems.size() >= myMaxUsages) {
                myHasMore = true;
                return false;
            }
        }

        UnifiedShowUsagesItem item = ReadAction.compute(() -> createItem(usage, matchBackground));
        if (item == null) {
            return true;
        }

        synchronized (myLock) {
            if (myItems.size() < myMaxUsages) {
                myItems.add(item);
                return true;
            }
            myHasMore = true;
        }
        item.dispose();
        return false;
    }

    @RequiredReadAction
    private @Nullable UnifiedShowUsagesItem createItem(Usage usage, @Nullable ColorValue matchBackground) {
        if (!usage.isValid()) {
            return null;
        }

        TextChunk[] chunks = usage.getPresentation().getText();

        VirtualFile file = usage instanceof UsageInFile usageInFile ? usageInFile.getFile() : null;
        int offset = -1;
        int firstTextChunk = 0;
        Navigatable navigatable = null;
        if (usage instanceof UsageInfo2UsageAdapter adapter) {
            firstTextChunk = 1;
            offset = adapter.getNavigationOffset();
            if (file instanceof VirtualFileWindow window) {
                if (offset >= 0) {
                    offset = window.getDocumentWindow().injectedToHost(offset);
                }
                file = window.getDelegate();
            }
            if (file == null) {
                return null;
            }
        }
        else {
            if (!usage.canNavigate()) {
                return null;
            }
            navigatable = usage;
        }

        int line = 0;
        int column = 0;
        RangeMarker marker = null;
        if (file != null && offset >= 0) {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null && offset <= document.getTextLength()) {
                int lineNumber = document.getLineNumber(offset);
                line = lineNumber + 1;
                column = offset - document.getLineStartOffset(lineNumber) + 1;
                if (navigatable == null) {
                    marker = document.createRangeMarker(offset, offset);
                }
            }
        }

        List<Pair<String, TextAttribute>> text = new ArrayList<>(chunks.length);
        StringBuilder plainText = new StringBuilder();
        for (int i = firstTextChunk; i < chunks.length; i++) {
            TextChunk chunk = chunks[i];
            text.add(Pair.create(chunk.getText(), UnifiedFindPopupPanel.toTextAttribute(chunk, matchBackground)));
            plainText.append(chunk.getText());
        }

        return new UnifiedShowUsagesItem(
            UnifiedShowUsagesItemKind.USAGE,
            file,
            file == null ? usage.getPresentation().getIcon() : VirtualFilePresentation.getIcon(file),
            file == null ? "" : file.getName(),
            file == null ? "" : file.getPath(),
            offset,
            line,
            column,
            List.copyOf(text),
            plainText.toString(),
            navigatable,
            marker
        );
    }

    private List<UnifiedShowUsagesItem> takeItems() {
        synchronized (myLock) {
            List<UnifiedShowUsagesItem> items = new ArrayList<>(myItems);
            myItems.clear();
            return items;
        }
    }

    private static void dispose(List<UnifiedShowUsagesItem> items) {
        for (UnifiedShowUsagesItem item : items) {
            item.dispose();
        }
    }

    @RequiredUIAccess
    private void showResult() {
        List<UnifiedShowUsagesItem> items = takeItems();
        try {
            showResult(items);
        }
        catch (RuntimeException e) {
            dispose(items);
            throw e;
        }
    }

    @RequiredUIAccess
    private void showResult(List<UnifiedShowUsagesItem> items) {
        FindUsagesOptions options = myOptions;
        if (myEditor.isDisposed() || myProject.isDisposed() || options == null) {
            dispose(items);
            return;
        }

        SearchScope searchScope = options.searchScope;

        boolean hasMore;
        int outOfScopeUsages;
        synchronized (myLock) {
            hasMore = myHasMore;
            outOfScopeUsages = myOutOfScopeUsages;
        }
        items.sort(ITEM_ORDER);

        String scopeName = searchScope.getDisplayName();
        if (items.isEmpty()) {
            if (outOfScopeUsages > 0) {
                showHint(myEditor, myPosition, outOfScopeMessage(outOfScopeUsages, searchScope));
            }
            else {
                showHint(myEditor, myPosition, UsageLocalize.noUsagesFoundIn(scopeName));
            }
            return;
        }

        if (!hasMore && outOfScopeUsages == 0) {
            if (items.size() == 1) {
                navigateAndHint(items.get(0), UsageLocalize.showUsagesOnlyUsage(scopeName));
                dispose(items);
                return;
            }

            if (areAllUsagesInOneLine(items)) {
                navigateAndHint(items.get(0), UsageLocalize.allUsagesAreInThisLine(items.size(), scopeName));
                dispose(items);
                return;
            }
        }

        LocalizeValue count = hasMore ? UsageLocalize.showUsagesOnlyNUsagesShown(items.size()) : UsageLocalize.usagesN(items.size());
        String title = LocalizeValue.join(LocalizeValue.of(myTitle), LocalizeValue.of(" ("), count, LocalizeValue.of(")")).get();

        List<UnifiedShowUsagesItem> rows = new ArrayList<>(items);
        if (hasMore) {
            rows.add(UnifiedShowUsagesItem.sentinel(UnifiedShowUsagesItemKind.MORE, LocalizeValue.localizeTODO("more usages").get()));
        }
        if (outOfScopeUsages > 0) {
            rows.add(UnifiedShowUsagesItem.sentinel(
                UnifiedShowUsagesItemKind.OUTSIDE_SCOPE,
                outOfScopeMessage(outOfScopeUsages, searchScope).get()
            ));
        }

        BaseListPopupStep<UnifiedShowUsagesItem> step = new BaseListPopupStep<>(title, rows) {
            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public String getTextFor(UnifiedShowUsagesItem value) {
                return value.isSentinel() ? "" : value.fileName() + " " + value.plainText();
            }

            @Override
            @RequiredUIAccess
            public PopupStep<?> onChosen(UnifiedShowUsagesItem selectedValue, boolean finalChoice) {
                if (selectedValue.kind() == UnifiedShowUsagesItemKind.MORE) {
                    return doFinalStep(() -> restart(myMaxUsages + ShowUsagesAction.getUsagesPageSize(), options));
                }

                if (selectedValue.kind() == UnifiedShowUsagesItemKind.OUTSIDE_SCOPE) {
                    FindUsagesOptions projectOptions = options.clone();
                    projectOptions.searchScope = GlobalSearchScope.projectScope(myHandler.getProject());
                    return doFinalStep(() -> restart(myMaxUsages, projectOptions));
                }

                Navigatable target = resolveNavigatable(selectedValue);
                return doFinalStep(() -> navigate(target));
            }
        };

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(myProject, step);
        Disposer.register(popup, () -> dispose(items));
        popup.setRender((TextItemRender<UnifiedShowUsagesItem>) (presentation, renderItem) -> {
            UnifiedShowUsagesItem item = renderItem.getValue();
            if (item == null) {
                return;
            }

            if (item.isSentinel()) {
                presentation.append("...<", TextAttribute.GRAYED);
                presentation.append(item.plainText(), TextAttribute.GRAYED);
                presentation.append(">...", TextAttribute.GRAYED);
                return;
            }

            presentation.withIcon(item.fileIcon());
            presentation.append(item.fileName());
            if (item.line() > 0) {
                presentation.append(" (" + item.line() + ":" + item.column() + ")  ", TextAttribute.GRAYED);
            }
            else {
                presentation.append("  ");
            }
            for (Pair<String, TextAttribute> fragment : item.text()) {
                presentation.append(fragment.getFirst(), fragment.getSecond());
            }
        });
        popup.show(myPosition);
    }

    private static LocalizeValue outOfScopeMessage(int outOfScopeUsages, SearchScope searchScope) {
        return LocalizeValue.localizeTODO(UsageViewManagerImpl.outOfScopeMessage(outOfScopeUsages, searchScope));
    }

    private static boolean areAllUsagesInOneLine(List<UnifiedShowUsagesItem> items) {
        UnifiedShowUsagesItem first = items.get(0);
        if (first.file() == null || first.line() <= 0) {
            return false;
        }
        for (UnifiedShowUsagesItem item : items) {
            if (!Objects.equals(item.file(), first.file()) || item.line() != first.line()) {
                return false;
            }
        }
        return true;
    }

    private @Nullable Navigatable resolveNavigatable(UnifiedShowUsagesItem item) {
        Navigatable navigatable = item.navigatable();
        if (navigatable != null) {
            return navigatable;
        }

        VirtualFile file = item.file();
        if (file == null || !file.isValid()) {
            return null;
        }
        return OpenFileDescriptorFactory.getInstance(myProject).builder(file).offset(Math.max(item.navigationOffset(), 0)).build();
    }

    @RequiredUIAccess
    private void navigate(@Nullable Navigatable target) {
        if (target != null && !myProject.isDisposed()) {
            target.navigate(true);
        }
    }

    @RequiredUIAccess
    private void navigateAndHint(UnifiedShowUsagesItem item, LocalizeValue hint) {
        navigate(resolveNavigatable(item));

        VirtualFile file = item.file();
        if (file == null || myProject.isDisposed()) {
            return;
        }

        Editor target = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        if (target == null || target.isDisposed() || !file.equals(FileDocumentManager.getInstance().getFile(target.getDocument()))) {
            return;
        }

        showHint(target, EditorPopupHelper.getInstance().guessBestPopupLocation(target), hint);
    }

    @RequiredUIAccess
    private static void showHint(Editor editor, RelativePoint2D position, LocalizeValue text) {
        JBPopup previous = editor.getUserData(HINT_KEY);
        if (previous != null) {
            previous.cancel();
        }

        Label label = Label.create(text);
        label.paddingBuilder().allSet(Space.SMALL).apply();

        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(label, null)
            .setRequestFocus(false)
            .setCancelOnClickOutside(true)
            .setCancelKeyEnabled(true)
            .createPopup();

        editor.putUserData(HINT_KEY, popup);
        Disposer.register(popup, () -> {
            if (editor.getUserData(HINT_KEY) == popup) {
                editor.putUserData(HINT_KEY, null);
            }
        });
        editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent event) {
                popup.cancel();
            }
        }, popup);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                popup.cancel();
            }
        }, popup);

        popup.show(position);
    }
}
