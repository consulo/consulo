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
package consulo.ai.editor.impl.internal;

import consulo.ai.AIChatAttachment;
import consulo.ai.AIChatInput;
import consulo.ai.AICommand;
import consulo.ai.AIContent;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.application.Application;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionManager;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.EditorBoxStyle;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionListProvider;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.Clipboard;
import consulo.ui.clipboard.ClipboardFeature;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyStroke;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.internal.AnActionWithUIUpdate;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.LayoutStyle;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import consulo.language.editor.WriteCommandAction;
import consulo.codeEditor.util.EditorModificationUtil;

/**
 * The box of a chat as an editor: the completion of the platform offers the commands after {@code /} and the files of
 * the project after {@code @}, and the keys of a chat are actions of the editor, which give way to an open completion.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
final class AIChatInputImpl implements AIChatInput {

    private static final Logger LOG = Logger.getInstance(AIChatInputImpl.class);

    private static final int MAX_FILES = 50;
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
        "png", "image/png",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "gif", "image/gif",
        "webp", "image/webp"
    );

    private record Item(String lookup, @Nullable String tail, @Nullable Image icon) {
    }

    private final Project myProject;
    private final EditorBox myEditorBox;
    /**
     * The box over its buttons - the buttons stay at the bottom however much is written.
     */
    private final DockLayout myRoot = DockLayout.create();
    private final HorizontalLayout mySuffix = HorizontalLayout.create(Space.NONE);
    private final Button mySend;
    private final List<Consumer<String>> mySubmitListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<AIChatAttachment>> myAttachListeners = new CopyOnWriteArrayList<>();

    private List<String> myHistory = List.of();
    /**
     * The entry of the history shown, -1 while the user writes.
     */
    private int myHistoryIndex = -1;
    private String myDraft = "";

    @RequiredUIAccess
    AIChatInputImpl(Project project, LocalizeValue placeholder, Supplier<List<AICommand>> commands, Disposable parentDisposable) {
        myProject = project;
        myEditorBox = project.getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(project)
            .language(PlainTextLanguage.INSTANCE)
            .multiline()
            .placeholder(placeholder)
            .completion(new CompletionProvider(project, commands))
            // a message wraps its lines - a box sliding sideways would hide what was written
            .customize(editor -> {
                editor.getSettings().setUseSoftWraps(true);
                // a message has no intentions to offer - a bulb would only sit over the text
                editor.getSettings().setShowIntentionBulb(false);
                editor.setHorizontalScrollbarVisible(false);
            })
            .build();

        mySend = Button.create(LocalizeValue.empty(), event -> submit());
        mySend.setIcon(PlatformIconGroup.actionsSend());
        mySend.setToolTipText(AIEditorLocalize.buttonSend());
        mySend.addStyle(ButtonStyle.INPLACE);
        mySuffix.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        mySuffix.add(mySend);

        myEditorBox.addStyle(EditorBoxStyle.INPLACE);
        DockLayout buttons = DockLayout.create();
        buttons.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        buttons.paddingBuilder().topSet(Space.SMALL).apply();
        buttons.right(mySuffix);
        myRoot.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        myRoot.center(myEditorBox);
        myRoot.bottom(buttons);

        myEditorBox.addValueListener(event -> myHistoryIndex = -1);
        myEditorBox.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                openCompletionOnTrigger(event);
            }
        }, parentDisposable);

        registerKey(KeyStroke.pressed(KeyCode.ENTER), parentDisposable, editor -> true, editor -> submit());
        registerKey(KeyStroke.pressed(KeyCode.ENTER, Modifier.SHIFT), parentDisposable, editor -> true, this::breakLine);
        registerKey(KeyStroke.pressed(KeyCode.UP), parentDisposable, this::canGoBack, editor -> goBack());
        installImagePaste(parentDisposable);
        myRoot.setDropHandler((component, transfer, checkOnly) -> {
            if (transfer.get(DataTransferType.IMAGE) == null && transfer.get(DataTransferType.FILE_LIST) == null) {
                return false;
            }
            return checkOnly || attachDropped(transfer);
        });
        registerKey(KeyStroke.pressed(KeyCode.DOWN), parentDisposable, this::canGoForward, editor -> goForward());
    }

    @Override
    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    @Override
    public void focus() {
        myEditorBox.focus();
    }

    @RequiredUIAccess
    @Override
    public void setSuffixComponent(Component suffix) {
        mySuffix.removeAll();
        mySuffix.add(suffix);
        mySuffix.add(mySend);
    }

    @Override
    public void addSubmitListener(Consumer<String> listener) {
        mySubmitListeners.add(listener);
    }

    @Override
    public void addTextListener(Consumer<String> listener) {
        myEditorBox.addValueListener(event -> listener.accept(getText()));
    }

    @Override
    public void addAttachListener(Consumer<AIChatAttachment> listener) {
        myAttachListeners.add(listener);
    }

    /**
     * A screenshot pasted into the box goes with the message as an image. A browser tells a paste as it happens; a
     * desktop pastes through the paste action, which the box takes over - what is no image pastes as always.
     */
    @RequiredUIAccess
    private void installImagePaste(Disposable parentDisposable) {
        UIAccess uiAccess = UIAccess.current();
        Clipboard clipboard = uiAccess.getClipboard();
        if (clipboard.isSupported(ClipboardFeature.PASTE_LISTENER)) {
            Disposable listener = clipboard.addPasteListener(transfer -> uiAccess.give(() -> {
                byte[] image = transfer.get(DataTransferType.IMAGE);
                if (image != null && myEditorBox.hasFocus()) {
                    attachImage(AIEditorLocalize.labelPastedImage(), "image/png", image);
                }
            }));
            Disposer.register(parentDisposable, listener);
            return;
        }

        class PasteAction extends DumbAwareAction {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                DataContext dataContext = e.getDataContext();
                clipboard.getContents().whenCompleteAsync((transfer, error) -> {
                    if (transfer != null && attachPasted(transfer)) {
                        return;
                    }
                    Editor editor = myEditorBox.getEditor();
                    if (editor != null) {
                        EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_PASTE)
                            .execute(editor, editor.getCaretModel().getCurrentCaret(), dataContext);
                    }
                }, uiAccess);
            }
        }
        new PasteAction().registerCustomShortcutSet(CommonShortcuts.getPaste(), myEditorBox, parentDisposable);
    }

    /**
     * Attaches the image of the clipboard, or the image files on it - false when it holds neither.
     */
    @RequiredUIAccess
    private boolean attachPasted(DataTransfer transfer) {
        byte[] image = transfer.get(DataTransferType.IMAGE);
        if (image != null) {
            attachImage(AIEditorLocalize.labelPastedImage(), "image/png", image);
            return true;
        }
        List<File> files = transfer.get(DataTransferType.FILE_LIST);
        boolean attached = false;
        if (files != null) {
            for (File file : files) {
                attached |= attachImageFile(file);
            }
        }
        return attached;
    }

    /**
     * A dropped image goes as an image, any other file as a link the agent reads itself.
     */
    @RequiredUIAccess
    private boolean attachDropped(DataTransfer transfer) {
        byte[] image = transfer.get(DataTransferType.IMAGE);
        if (image != null) {
            attachImage(AIEditorLocalize.labelPastedImage(), "image/png", image);
            return true;
        }
        List<File> files = transfer.get(DataTransferType.FILE_LIST);
        if (files == null) {
            return false;
        }
        boolean attached = false;
        for (File file : files) {
            if (attachImageFile(file)) {
                attached = true;
                continue;
            }
            if (!file.exists()) {
                continue;
            }
            AIContent link = AIContent.resourceLink(file.toPath().toUri().toString(), file.getName(), null);
            fireAttached(new AIChatAttachment(LocalizeValue.of(file.getName()), List.of(link)));
            attached = true;
        }
        return attached;
    }

    @RequiredUIAccess
    private boolean attachImageFile(File file) {
        String name = file.getName();
        String mediaType = IMAGE_TYPES.get(name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT));
        if (mediaType == null || !file.isFile() || file.length() > MAX_IMAGE_BYTES) {
            return false;
        }
        try {
            attachImage(LocalizeValue.of(name), mediaType, Files.readAllBytes(file.toPath()));
            return true;
        }
        catch (IOException e) {
            LOG.warn("Cannot read the image " + file, e);
            return false;
        }
    }

    @RequiredUIAccess
    private void fireAttached(AIChatAttachment attachment) {
        for (Consumer<AIChatAttachment> listener : myAttachListeners) {
            listener.accept(attachment);
        }
    }

    @RequiredUIAccess
    private void attachImage(LocalizeValue label, String mediaType, byte[] data) {
        fireAttached(new AIChatAttachment(label, List.of(AIContent.image(mediaType, data))));
    }

    @RequiredUIAccess
    @Override
    public void setHistory(List<String> history) {
        myHistory = List.copyOf(history);
        myHistoryIndex = -1;
    }

    /**
     * A key of the chat as an action of the editor - it stands aside while the completion is open, and while it is
     * not {@code enabled}, so the editor does with the key what it always does.
     */
    @RequiredUIAccess
    private void registerKey(KeyStroke keyStroke,
                             Disposable parentDisposable,
                             Predicate<Editor> enabled,
                             Consumer<Editor> action) {
        class KeyAction extends DumbAwareAction implements AnActionWithUIUpdate {
            @RequiredUIAccess
            @Override
            public void updateAtUI(AnActionEvent e) {
                Editor editor = myEditorBox.getEditor();
                boolean lookup = LookupManager.getInstance(myProject).getActiveLookup() != null;
                e.getPresentation().setEnabled(editor != null && !lookup && enabled.test(editor));
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                Editor editor = myEditorBox.getEditor();
                if (editor != null) {
                    action.accept(editor);
                }
            }
        }
        new KeyAction().registerCustomShortcutSet(new CustomShortcutSet(keyStroke), myEditorBox, parentDisposable);
    }

    /**
     * Opens the completion while a word starting with {@code /} or {@code @} is typed - again on every character of
     * it, as an automatic popup does not follow the typing on its own here.
     */
    private void openCompletionOnTrigger(DocumentEvent event) {
        CharSequence inserted = event.getNewFragment();
        if (inserted.length() != 1 || Character.isWhitespace(inserted.charAt(0))) {
            return;
        }
        CharSequence text = event.getDocument().getCharsSequence();
        int start = event.getOffset();
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) {
            start--;
        }
        char first = text.charAt(start);
        if (first != '/' && first != '@') {
            return;
        }
        Editor editor = myEditorBox.getEditor();
        if (editor != null && LookupManager.getInstance(myProject).getActiveLookup() == null) {
            AutoPopupController.getInstance(myProject).scheduleAutoPopup(editor, CompletionType.BASIC, null);
        }
    }

    @RequiredUIAccess
    private void submit() {
        String text = myEditorBox.getValue();
        if (text == null || text.isBlank()) {
            return;
        }
        myEditorBox.setValue("");
        myHistoryIndex = -1;
        for (Consumer<String> listener : mySubmitListeners) {
            listener.accept(text);
        }
    }

    @RequiredUIAccess
    private void breakLine(Editor editor) {
        // a message has no indent to keep - the line break goes in as it is, on every frontend
        WriteCommandAction.runWriteCommandAction(myProject, () -> EditorModificationUtil.insertStringAtCaret(editor, "\n"));
    }

    private boolean canGoBack(Editor editor) {
        Document document = editor.getDocument();
        return !myHistory.isEmpty() && document.getLineNumber(editor.getCaretModel().getOffset()) == 0;
    }

    private boolean canGoForward(Editor editor) {
        Document document = editor.getDocument();
        int line = document.getLineNumber(editor.getCaretModel().getOffset());
        return myHistoryIndex >= 0 && line == Math.max(0, document.getLineCount() - 1);
    }

    @RequiredUIAccess
    private void goBack() {
        if (myHistoryIndex < 0) {
            String value = myEditorBox.getValue();
            myDraft = value == null ? "" : value;
            myHistoryIndex = myHistory.size();
        }
        if (myHistoryIndex == 0) {
            return;
        }
        int index = myHistoryIndex - 1;
        show(myHistory.get(index));
        myHistoryIndex = index;
    }

    @RequiredUIAccess
    private void goForward() {
        int index = myHistoryIndex + 1;
        String text = index >= myHistory.size() ? myDraft : myHistory.get(index);
        show(text);
        myHistoryIndex = index >= myHistory.size() ? -1 : index;
    }

    @RequiredUIAccess
    @Override
    public void setText(String text) {
        myHistoryIndex = -1;
        show(text);
    }

    @Override
    public String getText() {
        String value = myEditorBox.getValue();
        return value == null ? "" : value;
    }

    @RequiredUIAccess
    private void show(String text) {
        myEditorBox.setValue(text);
        Editor editor = myEditorBox.getEditor();
        if (editor != null) {
            editor.getCaretModel().moveToOffset(text.length());
        }
    }

    /**
     * The commands after {@code /}, the files of the project after {@code @} - a file matches where its name contains
     * what is typed, and goes in as its path in the project.
     */
    private static final class CompletionProvider extends TextFieldWithAutoCompletionListProvider<Item> {
        private final Project myProject;
        private final Supplier<List<AICommand>> myCommands;

        CompletionProvider(Project project, Supplier<List<AICommand>> commands) {
            super(List.of());
            myProject = project;
            myCommands = commands;
        }

        @Override
        public Collection<Item> getItems(String prefix, boolean cached, CompletionParameters parameters) {
            if (prefix == null || prefix.isEmpty()) {
                return List.of();
            }
            if (prefix.charAt(0) == '/') {
                if (!cached) {
                    return List.of();
                }
                List<Item> items = new ArrayList<>();
                for (AICommand command : myCommands.get()) {
                    items.add(new Item("/" + command.name(), command.description().isEmpty() ? null : command.description(), null));
                }
                return items;
            }
            if (prefix.charAt(0) == '@' && !cached) {
                return Application.get().runReadAction((Supplier<List<Item>>) () -> files(prefix.substring(1)));
            }
            return List.of();
        }

        private List<Item> files(String query) {
            String lowered = query.toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            FilenameIndex.processAllFileNames(name -> {
                if (name.toLowerCase(Locale.ROOT).contains(lowered)) {
                    names.add(name);
                }
                return names.size() < MAX_FILES;
            }, GlobalSearchScope.projectScope(myProject), null);

            String basePath = myProject.getBasePath();
            List<Item> items = new ArrayList<>();
            for (String name : names) {
                for (VirtualFile file : FilenameIndex.getVirtualFilesByName(myProject, name, GlobalSearchScope.projectScope(myProject))) {
                    String path = file.getPath();
                    String relative = basePath != null && path.startsWith(basePath + "/") ? path.substring(basePath.length() + 1) : path;
                    items.add(new Item("@" + relative, null, file.getFileType().getIcon()));
                    if (items.size() >= MAX_FILES) {
                        return items;
                    }
                }
            }
            return items;
        }

        @Override
        public @Nullable PrefixMatcher createPrefixMatcher(String prefix) {
            return new ContainsMatcher(prefix);
        }

        @Override
        protected @Nullable Image getIcon(Item item) {
            return item.icon();
        }

        @Override
        protected String getLookupString(Item item) {
            return item.lookup();
        }

        @Override
        protected @Nullable String getTailText(Item item) {
            return item.tail() == null ? null : "  " + item.tail();
        }

        @Override
        protected @Nullable String getTypeText(Item item) {
            return null;
        }

        @Override
        public int compare(Item item1, Item item2) {
            return item1.lookup().compareToIgnoreCase(item2.lookup());
        }
    }

    /**
     * Matches a command by its start, a file wherever its path holds the typed part.
     */
    private static final class ContainsMatcher extends PrefixMatcher {
        ContainsMatcher(String prefix) {
            super(prefix);
        }

        @Override
        public boolean prefixMatches(String name) {
            if (myPrefix.isEmpty() || name.isEmpty() || name.charAt(0) != myPrefix.charAt(0)) {
                return false;
            }
            String typed = myPrefix.substring(1).toLowerCase(Locale.ROOT);
            String candidate = name.substring(1).toLowerCase(Locale.ROOT);
            return myPrefix.charAt(0) == '/' ? candidate.startsWith(typed) : candidate.contains(typed);
        }

        @Override
        public PrefixMatcher cloneWithPrefix(String prefix) {
            return new ContainsMatcher(prefix);
        }
    }
}
