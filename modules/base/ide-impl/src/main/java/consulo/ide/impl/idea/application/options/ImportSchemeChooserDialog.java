package consulo.ide.impl.idea.application.options;

import consulo.application.ApplicationBundle;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImportSchemeChooserDialog extends DialogWrapper {
  private JPanel contentPane;
  private JBList mySchemeList;
  private JTextField myTargetNameField;
  private JCheckBox myUseCurrentScheme;
  private String mySelectedName;
  private final static String UNNAMED_SCHEME_ITEM = "<" + ApplicationBundle.message("code.style.scheme.import.unnamed") + ">";
  private final List<String> myNames = new ArrayList<String>();

  public ImportSchemeChooserDialog(@Nonnull Component parent,
                                   String[] schemeNames,
                                   final @Nullable String currScheme) {
    super(parent, false);
    if (schemeNames.length > 0) {
      myNames.addAll(Arrays.asList(schemeNames));
    }
    else {
      myNames.add(UNNAMED_SCHEME_ITEM);
    }
    mySchemeList.setModel(new DefaultListModel() {
      @Override
      public int getSize() {
        return myNames.size();
      }

      @Override
      public Object getElementAt(int index) {
        return myNames.get(index);
      }
    });
    mySchemeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mySchemeList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        int index = mySchemeList.getSelectedIndex();
        if (index >= 0) {
          mySelectedName = myNames.get(index);
          if (!myUseCurrentScheme.isSelected() && !UNNAMED_SCHEME_ITEM.equals(mySelectedName)) myTargetNameField.setText(mySelectedName);
        }
      }
    });
    myUseCurrentScheme.setEnabled(currScheme != null);
    myUseCurrentScheme.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myUseCurrentScheme.isSelected()) {
          myTargetNameField.setEnabled(false);
          if (currScheme != null) {
            myTargetNameField.setText(currScheme);
          }
        }
        else {
          myTargetNameField.setEnabled(true);
          if (mySelectedName != null) myTargetNameField.setText(mySelectedName);
        }
      }
    });
    mySchemeList.getSelectionModel().setSelectionInterval(0,0);
    init();
    setTitle(ApplicationBundle.message("title.import.scheme.chooser"));
  }

  public String getSelectedName() {
    return UNNAMED_SCHEME_ITEM.equals(mySelectedName) ? null : mySelectedName;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return contentPane;
  }

  public boolean isUseCurrentScheme() {
    return myUseCurrentScheme.isSelected();
  }

  @Nullable
  public String getTargetName() {
    String name = myTargetNameField.getText();
    return name != null && !name.trim().isEmpty() ? name : null;
  }
}
