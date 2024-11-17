package consulo.ide.impl.idea.execution.console;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.openapi.vfs.encoding.EncodingManagerImpl;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.awt.AddEditDeleteListPanel;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.Splitter;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author peter
 */
@ExtensionImpl
public class ConsoleConfigurable implements SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
    private static class Panel {
        private final JPanel myMainComponent;
        private final MyAddDeleteListPanel myPositivePanel;
        private final MyAddDeleteListPanel myNegativePanel;

        private ConsoleEncodingComboBox myEncodingBox;

        public Panel() {
            myMainComponent = new JPanel(new BorderLayout());
            myEncodingBox = new ConsoleEncodingComboBox();
            myMainComponent.add(LabeledComponent.sided(myEncodingBox, IdeBundle.message("combobox.console.default.encoding.label")), BorderLayout.NORTH);

            Splitter splitter = new Splitter(true);
            myMainComponent.add(splitter, BorderLayout.CENTER);
            myPositivePanel =
                new MyAddDeleteListPanel("Fold console lines that contain", "Enter a substring of a console line you'd like to see folded:");
            myNegativePanel = new MyAddDeleteListPanel("Exceptions", "Enter a substring of a console line you don't want to fold:");
            splitter.setFirstComponent(myPositivePanel);
            splitter.setSecondComponent(myNegativePanel);

            myPositivePanel.getEmptyText().setText("Fold nothing");
            myNegativePanel.getEmptyText().setText("No exceptions");
        }
    }

    private static class MyAddDeleteListPanel extends AddEditDeleteListPanel<String> {
        private final String myQuery;

        public MyAddDeleteListPanel(String title, String query) {
            super(title, new ArrayList<>());
            myQuery = query;
        }

        @Override
        @Nullable
        protected String findItemToAdd() {
            return showEditDialog("");
        }

        @Nullable
        private String showEditDialog(final String initialValue) {
            return Messages.showInputDialog(this, myQuery, "Folding pattern", Messages.getQuestionIcon(), initialValue, new InputValidatorEx() {
                @RequiredUIAccess
                @Override
                public boolean checkInput(String inputString) {
                    return !StringUtil.isEmpty(inputString);
                }

                @RequiredUIAccess
                @Override
                public boolean canClose(String inputString) {
                    return !StringUtil.isEmpty(inputString);
                }

                @Nullable
                @Override
                public String getErrorText(String inputString) {
                    if (!checkInput(inputString)) {
                        return "Console folding rule string cannot be empty";
                    }
                    return null;
                }
            });
        }

        void resetFrom(List<String> patterns) {
            myListModel.clear();
            for (String pattern : patterns) {
                myListModel.addElement(pattern);
            }
        }

        void applyTo(List<String> patterns) {
            patterns.clear();
            for (Object o : getListItems()) {
                patterns.add((String) o);
            }
        }

        public void addRule(String rule) {
            addElement(rule);
        }

        @Override
        protected String editSelectedItem(String item) {
            return showEditDialog(item);
        }
    }

    private final Provider<ConsoleFoldingSettings> mySettings;
    private final EncodingManagerImpl myApplicationEncodingManager;

    private Panel myPanel;

    @Inject
    public ConsoleConfigurable(Provider<ConsoleFoldingSettings> settings, ApplicationEncodingManager applicationEncodingManager) {
        mySettings = settings;
        myApplicationEncodingManager = (EncodingManagerImpl) applicationEncodingManager;
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent(@Nonnull Disposable disposable) {
        if (myPanel == null) {
            myPanel = new Panel();
        }
        return myPanel.myMainComponent;
    }

    public void addRule(@Nonnull String rule) {
        myPanel.myPositivePanel.addRule(rule);
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        ConsoleFoldingSettings consoleFoldingSettings = mySettings.get();
        return !Arrays.asList(myPanel.myNegativePanel.getListItems()).equals(consoleFoldingSettings.getNegativePatterns()) ||
            !Arrays.asList(myPanel.myPositivePanel.getListItems()).equals(consoleFoldingSettings.getPositivePatterns()) ||
            !Objects.equals(myApplicationEncodingManager.getDefaultConsoleEncodingReference(), myPanel.myEncodingBox.getSelectedEncodingReference());

    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        ConsoleFoldingSettings consoleFoldingSettings = mySettings.get();

        myPanel.myNegativePanel.applyTo(consoleFoldingSettings.getNegativePatterns());
        myPanel.myPositivePanel.applyTo(consoleFoldingSettings.getPositivePatterns());

        myApplicationEncodingManager.setDefaultConsoleEncodingReference(myPanel.myEncodingBox.getSelectedEncodingReference());
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        ConsoleFoldingSettings consoleFoldingSettings = mySettings.get();

        myPanel.myEncodingBox.reset(myApplicationEncodingManager, myApplicationEncodingManager.getDefaultConsoleEncodingReference());
        myPanel.myNegativePanel.resetFrom(consoleFoldingSettings.getNegativePatterns());
        myPanel.myPositivePanel.resetFrom(consoleFoldingSettings.getPositivePatterns());
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myPanel = null;
    }

    @Override
    @Nonnull
    public String getId() {
        return "execution.console.folding";
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Console";
    }
}
