package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.application.util.matcher.NameUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ui.ex.awt.UIUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Model allowing to show a fixed list of items, each having a name and description, in Choose by Name popup.
 */
public class ListChooseByNameModel<T extends ChooseByNameItem> extends SimpleChooseByNameModel {

  private static final int MAX_DESC_LENGTH = 80;
  private static final String ELLIPSIS_SUFFIX = "...";

  private Pattern myCompiledPattern;
  private String myPattern;
  private final List<T> myItems;
  private final String myNotInMessage;

  public ListChooseByNameModel(@Nonnull Project project,
                               String prompt,
                               String notInMessage,
                               List<T> items) {
    super(project, prompt, null);

    myItems = items;
    myNotInMessage = notInMessage;
  }

  @Override
  public String[] getNames() {
    ArrayList<String> taskFullCmds = new ArrayList<String>();
    for (T item : myItems) {
      taskFullCmds.add(item.getName());
    }

    return ArrayUtil.toStringArray(taskFullCmds);
  }

  @Override
  protected Object[] getElementsByName(String name, String pattern) {
    for (T item : myItems) {
      if (item.getName().equals(name)) {
        return new Object[] { item };
      }
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public String getNotInMessage() {
    return myNotInMessage;
  }

  @Override
  public String getNotFoundMessage() {
    return myNotInMessage;
  }

  // from ruby plugin
  @Override
  public ListCellRenderer getListCellRenderer() {
    return new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list,
                                                    Object value,
                                                    int index, boolean isSelected, boolean cellHasFocus) {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBorder(new EmptyBorder(0, 0, 0, 5));

        Color bg = isSelected ? UIUtil.getListSelectionBackground() : UIUtil.getListBackground();
        panel.setBackground(bg);

        if (value instanceof ChooseByNameItem) {
          ChooseByNameItem item = (ChooseByNameItem) value;

          Color fg = isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();

          JLabel actionLabel = new JLabel(item.getName(), null, LEFT);
          actionLabel.setBackground(bg);
          actionLabel.setForeground(fg);
          actionLabel.setFont(actionLabel.getFont().deriveFont(Font.BOLD));
          actionLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

          panel.add(actionLabel, BorderLayout.WEST);

          String description = item.getDescription();
          if (description != null) {
            // truncate long descriptions
            String normalizedDesc;
            if (description.length() > MAX_DESC_LENGTH) {
              normalizedDesc = description.substring(0, MAX_DESC_LENGTH) + ELLIPSIS_SUFFIX;
            }
            else {
              normalizedDesc = description;
            }
            JLabel descriptionLabel = new JLabel(normalizedDesc);
            descriptionLabel.setBackground(bg);
            descriptionLabel.setForeground(fg);
            descriptionLabel.setBorder(new EmptyBorder(0, 15, 0, 0));

            panel.add(descriptionLabel, BorderLayout.EAST);
          }
        }
        else {
          // E.g. "..." item
          JLabel actionLabel = new JLabel(value.toString(), null, LEFT);
          actionLabel.setBackground(bg);
          actionLabel.setForeground(UIUtil.getListForeground());
          actionLabel.setFont(actionLabel.getFont().deriveFont(Font.PLAIN));
          actionLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
          panel.add(actionLabel, BorderLayout.WEST);
        }
        return panel;
      }
    };
  }

  @Override
  public String getElementName(Object element) {
    if (!(element instanceof ChooseByNameItem)) return null;
    return ((ChooseByNameItem)element).getName();
  }

  public boolean matches(@Nonnull String name, @Nonnull String pattern) {
    Pattern compiledPattern = getTaskPattern(pattern);
    if (compiledPattern == null) {
      return false;
    }

    return compiledPattern.matcher(name).find();
  }

  @Nullable
  private Pattern getTaskPattern(String pattern) {
    if (!Comparing.strEqual(pattern, myPattern)) {
      myCompiledPattern = null;
      myPattern = pattern;
    }
    if (myCompiledPattern == null) {
      String regex = "^.*" + NameUtil.buildRegexp(pattern, 0, true, true);

      myCompiledPattern = Pattern.compile(regex);
    }
    return myCompiledPattern;
  }
}
