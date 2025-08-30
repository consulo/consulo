/*
 * Copyright 2013-2020 consulo.io
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
package consulo.fileChooser;

import consulo.annotation.DeprecationInfo;
import consulo.component.ComponentManager;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.PseudoComponent;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TextComponentAccessor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.localize.UILocalize;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Builder for {@link TextBox} with browse button. On click show file chooser dialog
 * <p>
 * If disposable is not set used {@link PlatformDataKeys#UI_DISPOSABLE)
 *
 * @author VISTALL
 * @since 2020-05-29
 */
public final class FileChooserTextBoxBuilder {
    private static AnAction STUB = new EmptyAction();

    private static class BrowseAction extends DumbAwareAction {
        private final Controller myController;

        public BrowseAction(Controller controller) {
            super(LocalizeValue.localizeTODO("Browse"), LocalizeValue.of(), PlatformIconGroup.nodesFolderopened());
            myController = controller;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            FileChooserDescriptor fileChooserDescriptor = (FileChooserDescriptor) myController.myFileChooserDescriptor.clone();
            fileChooserDescriptor.withTitleValue(myController.myDialogTitle);
            fileChooserDescriptor.withDescriptionValue(myController.myDialogDescription);

            String text = myController.myAccessor.getValue(myController.myTextBox);

            FileChooser.chooseFile(fileChooserDescriptor, myController.myProject, myController.mySelectedFileMapper.apply(text)).
                doWhenDone((f) -> {
                    myController.myAccessor.setValue(myController.myTextBox, f.getPresentableUrl());
                });
        }
    }

    public static class Controller implements PseudoComponent {
        private final TextBox myTextBox;
        private final TextComponentAccessor<TextBox> myAccessor;
        private final ComponentManager myProject;
        private final FileChooserDescriptor myFileChooserDescriptor;
        private final Function<String, VirtualFile> mySelectedFileMapper;
        private final LocalizeValue myDialogTitle;
        private final LocalizeValue myDialogDescription;

        public Controller(FileChooserTextBoxBuilder builder) {
            myAccessor = builder.myTextBoxAccessor;
            myProject = builder.myProject;
            myFileChooserDescriptor = builder.myFileChooserDescriptor;
            mySelectedFileMapper = builder.mySelectedFileMapper;
            myDialogTitle = builder.myDialogTitle;
            myDialogDescription = builder.myDialogDescription;

            myTextBox = TextBox.create();
            if (!builder.myDisableCompletion) {
                FileChooserFactory.getInstance().installFileCompletion(myTextBox, myFileChooserDescriptor, true, builder.myDisposable);
            }

            // replace stub by own action impl
            builder.myActions.replaceAll(action -> action == STUB ? new BrowseAction(this) : action);

            ActionGroup actionGroup = ActionGroup.newImmutableBuilder().addAll(builder.myActions).build();
            ActionToolbar toolbar =
                ActionToolbarFactory.getInstance().createActionToolbar("FileChooserTextBox", actionGroup, ActionToolbar.Style.INPLACE);

            toolbar.setTargetUIComponent(myTextBox);

            myTextBox.setSuffixComponent(toolbar.getUIComponent());
        }

        @RequiredUIAccess
        public void setValue(String text) {
            myAccessor.setValue(myTextBox, text);
        }

        @RequiredUIAccess
        public void setValue(@Nonnull String text, boolean fireListeners) {
            myAccessor.setValue(myTextBox, text, fireListeners);
        }

        @RequiredUIAccess
        public void setValue(@Nonnull VirtualFile value) {
            setValue(value.getPresentableUrl());
        }

        @RequiredUIAccess
        @Nonnull
        public String getValue() {
            return StringUtil.notNullize(myAccessor.getValue(myTextBox));
        }

        @RequiredUIAccess
        @Nonnull
        @Override
        public TextBox getComponent() {
            return myTextBox;
        }
    }

    @Nonnull
    public static FileChooserTextBoxBuilder create(@Nullable ComponentManager project) {
        return new FileChooserTextBoxBuilder(project);
    }

    private final ComponentManager myProject;

    private LocalizeValue myDialogTitle = UILocalize.fileChooserDefaultTitle();

    private LocalizeValue myDialogDescription = LocalizeValue.empty();

    private FileChooserDescriptor myFileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);

    private TextComponentAccessor<TextBox> myTextBoxAccessor = TextComponentAccessor.TEXT_BOX_WHOLE_TEXT;

    private List<AnAction> myActions = new ArrayList<>(1);

    private boolean myDisableCompletion;

    private Disposable myDisposable;

    private FileChooserTextBoxBuilder(ComponentManager project) {
        myProject = project;
        myActions.add(STUB);
    }

    private Function<String, VirtualFile> mySelectedFileMapper = directoryName -> {
        if (StringUtil.isEmptyOrSpaces(directoryName)) {
            return null;
        }

        directoryName = FileUtil.toSystemIndependentName(directoryName);
        VirtualFile path = LocalFileSystem.getInstance().findFileByPath(directoryName);
        while (path == null && directoryName.length() > 0) {
            int pos = directoryName.lastIndexOf('/');
            if (pos <= 0) {
                break;
            }
            directoryName = directoryName.substring(0, pos);
            path = LocalFileSystem.getInstance().findFileByPath(directoryName);
        }
        return path;
    };

    @Nonnull
    public FileChooserTextBoxBuilder dialogTitle(@Nonnull LocalizeValue dialogTitle) {
        myDialogTitle = dialogTitle;
        return this;
    }

    @Nonnull
    @Deprecated
    @DeprecationInfo("Use #dialogTitle(LocalizeValue)")
    public FileChooserTextBoxBuilder dialogTitle(@Nonnull String dialogTitle) {
        return dialogTitle(LocalizeValue.of(dialogTitle));
    }

    @Nonnull
    public FileChooserTextBoxBuilder dialogDescription(@Nonnull LocalizeValue dialogDescription) {
        myDialogDescription = dialogDescription;
        return this;
    }

    @Nonnull
    @Deprecated
    @DeprecationInfo("Use #dialogDescription(LocalizeValue)")
    public FileChooserTextBoxBuilder dialogDescription(@Nonnull String dialogDescription) {
        return dialogTitle(LocalizeValue.of(dialogDescription));
    }

    @Nonnull
    public FileChooserTextBoxBuilder fileChooserDescriptor(@Nonnull FileChooserDescriptor chooserDescriptor) {
        myFileChooserDescriptor = chooserDescriptor;
        return this;
    }

    public FileChooserTextBoxBuilder textBoxAccessor(@Nonnull TextComponentAccessor<TextBox> componentAccessor) {
        myTextBoxAccessor = componentAccessor;
        return this;
    }

    @Nonnull
    public FileChooserTextBoxBuilder firstActions(@Nonnull AnAction... actions) {
        for (AnAction action : actions) {
            myActions.addFirst(action);
        }
        return this;
    }

    @Nonnull
    public FileChooserTextBoxBuilder lastActions(@Nonnull AnAction... actions) {
        for (AnAction action : actions) {
            myActions.addLast(action);
        }
        return this;
    }

    @Nonnull
    public FileChooserTextBoxBuilder disableCompletion() {
        myDisableCompletion = true;
        return this;
    }

    @Nonnull
    public FileChooserTextBoxBuilder uiDisposable(@Nonnull Disposable disposable) {
        myDisposable = disposable;
        return this;
    }

    @RequiredUIAccess
    @Nonnull
    public Controller build() {
        return new Controller(this);
    }
}
