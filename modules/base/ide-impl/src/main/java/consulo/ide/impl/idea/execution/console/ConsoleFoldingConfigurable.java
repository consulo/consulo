package consulo.ide.impl.idea.execution.console;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.ui.ex.awt.AddEditDeleteListPanel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.Splitter;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author peter
 */
@ExtensionImpl
public class ConsoleFoldingConfigurable implements SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
  private final Provider<ConsoleFoldingSettings> mySettings;

  private JPanel myMainComponent;
  private MyAddDeleteListPanel myPositivePanel;
  private MyAddDeleteListPanel myNegativePanel;

  @Inject
  public ConsoleFoldingConfigurable(Provider<ConsoleFoldingSettings> settings) {
    mySettings = settings;
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable disposable) {
    if (myMainComponent == null) {
      myMainComponent = new JPanel(new BorderLayout());
      Splitter splitter = new Splitter(true);
      myMainComponent.add(splitter);
      myPositivePanel =
        new MyAddDeleteListPanel("Fold console lines that contain", "Enter a substring of a console line you'd like to see folded:");
      myNegativePanel = new MyAddDeleteListPanel("Exceptions", "Enter a substring of a console line you don't want to fold:");
      splitter.setFirstComponent(myPositivePanel);
      splitter.setSecondComponent(myNegativePanel);

      myPositivePanel.getEmptyText().setText("Fold nothing");
      myNegativePanel.getEmptyText().setText("No exceptions");
    }
    return myMainComponent;
  }

  public void addRule(@Nonnull String rule) {
    myPositivePanel.addRule(rule);
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    ConsoleFoldingSettings consoleFoldingSettings = mySettings.get();
    return !Arrays.asList(myNegativePanel.getListItems()).equals(consoleFoldingSettings.getNegativePatterns()) ||
           !Arrays.asList(myPositivePanel.getListItems()).equals(consoleFoldingSettings.getPositivePatterns());

  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    ConsoleFoldingSettings consoleFoldingSettings = mySettings.get();

    myNegativePanel.applyTo(consoleFoldingSettings.getNegativePatterns());
    myPositivePanel.applyTo(consoleFoldingSettings.getPositivePatterns());
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    ConsoleFoldingSettings consoleFoldingSettings = mySettings.get();

    myNegativePanel.resetFrom(consoleFoldingSettings.getNegativePatterns());
    myPositivePanel.resetFrom(consoleFoldingSettings.getPositivePatterns());
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myMainComponent = null;
    myNegativePanel = null;
    myPositivePanel = null;
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

  @Override
  @Nls
  public String getDisplayName() {
    return "Console Folding";
  }

  private static class MyAddDeleteListPanel extends AddEditDeleteListPanel<String> {
    private final String myQuery;

    public MyAddDeleteListPanel(String title, String query) {
      super(title, new ArrayList<String>());
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
        patterns.add((String)o);
      }
    }

    public void addRule(String rule) {
      addElement(rule);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
      super.setBounds(x, y, width, height);
    }

    @Override
    protected String editSelectedItem(String item) {
      return showEditDialog(item);
    }
  }
}
