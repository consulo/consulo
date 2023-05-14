/*
 * Copyright 2013-2017 consulo.io
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

package consulo.ide.impl.presentationAssistant;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ui.popup.ComponentPopupBuilderImpl;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * @author VISTALL
 * @since 21-Aug-17
 * <p>
 * original author Nikolay Chashnikov (kotlin)
 */
class ActionInfoPanel extends NonOpaquePanel implements Disposable {
  public class FadeInOutAnimator extends Animator {
    private boolean myForward;

    public FadeInOutAnimator(boolean forward) {
      super("Action Hint Fade In/Out", 5, 100, false, forward);
      myForward = forward;
    }

    @Override
    public void paintNow(int frame, int totalFrames, int cycle) {
      if (myForward && phase != Phase.FADING_IN || !myForward && phase != Phase.FADING_OUT) {
        setAlpha(hintAlpha + (1 - hintAlpha) * (totalFrames - frame) / totalFrames);
      }
    }

    @Override
    protected void paintCycleEnd() {
      if (myForward) {
        showFinal();
      }
      else {
        close();
      }
    }
  }

  enum Phase {
    FADING_IN,
    SHOWN,
    FADING_OUT,
    HIDDEN
  }

  private static final int hideDelay = 4 * 1000;


  private JPanel labelsPanel;
  private JBPopup hint;
  private Alarm hideAlarm = new Alarm(this);
  private Animator animator;
  private Phase phase = Phase.FADING_IN;
  private float hintAlpha = UIUtil.isUnderDarkTheme() ? 0.05f : 0.1f;

  public ActionInfoPanel(Project project, List<Pair<String, Font>> textFragments) {
    super(new BorderLayout());

    IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
    labelsPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    updateLabelText(project, textFragments);
    setBackground(LightColors.GREEN);
    setOpaque(true);
    add(labelsPanel, BorderLayout.CENTER);
    JBEmptyBorder emptyBorder = JBUI.Borders.empty(5, 10);
    setBorder(emptyBorder);

    ComponentPopupBuilderImpl builder = (ComponentPopupBuilderImpl)JBPopupFactory.getInstance().createComponentPopupBuilder(this, this);
    builder.setAlpha(1.0f);
    builder.setFocusable(false);
    builder.setBelongsToGlobalPopupStack(false);
    builder.setCancelKeyEnabled(false);
    builder.setCancelCallback(() ->
                              {
                                phase = Phase.HIDDEN;
                                return true;
                              });
    hint = builder.createPopup();

    hint.addListener(new JBPopupAdapter() {
      @Override
      public void onClosed(LightweightWindowEvent event) {
        phase = Phase.HIDDEN;
      }
    });
    animator = new FadeInOutAnimator(true);
    hint.show(computeLocation(ideFrame));
    animator.resume();
  }

  public boolean canBeReused() {
    return phase == Phase.FADING_IN || phase == Phase.SHOWN;
  }

  public void updateText(Project project, List<Pair<String, Font>> textFragments) {
    if (getHintWindow() == null) {
      return;
    }
    labelsPanel.removeAll();
    updateLabelText(project, textFragments);
    hint.getContent().invalidate();
    IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
    hint.setLocation(computeLocation(ideFrame).getScreenPoint());
    hint.setSize(getPreferredSize());
    hint.getContent().repaint();
    showFinal();
  }

  public Window getHintWindow() {
    if (hint.isDisposed()) {
      return null;
    }
    Window window = SwingUtilities.windowForComponent(hint.getContent());
    if (window != null && window.isShowing()) {
      return window;
    }
    return null;
  }

  private void setAlpha(float alpha) {
    Window window = getHintWindow();
    if (window != null) {
      WindowManager.getInstance().setAlphaModeRatio(window, alpha);
    }
  }

  public void showFinal() {
    phase = Phase.SHOWN;
    setAlpha(hintAlpha);
    hideAlarm.cancelAllRequests();
    hideAlarm.addRequest(this::fadeOut, hideDelay);
  }

  private void fadeOut() {
    if (phase != Phase.SHOWN) {
      return;
    }
    phase = Phase.FADING_OUT;
    Disposer.dispose(animator);
    animator = new FadeInOutAnimator(false);
    animator.resume();
  }

  private RelativePoint computeLocation(IdeFrame ideFrame) {
    int statusBarHeight = ideFrame.getStatusBar().getComponent().getHeight();
    Rectangle visibleRect = ideFrame.getComponent().getVisibleRect();
    Dimension popupSize = getPreferredSize();
    Point popup = new Point(visibleRect.x + (visibleRect.width - popupSize.width) / 2,
                            visibleRect.y + visibleRect.height - popupSize.height - statusBarHeight - JBUI.scale(5));
    return new RelativePoint(ideFrame.getComponent(), popup);
  }

  private void updateLabelText(Project project, List<Pair<String, Font>> textFragments) {
    IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
    for (JLabel label : createLabels(textFragments, ideFrame)) {
      labelsPanel.add(label);
    }
  }

  @Nonnull
  private List<JLabel> createLabels(List<Pair<String, Font>> textFragments, IdeFrame ideFrame) {
    int fontSize = PresentationAssistant.getInstance().getConfiguration().getFontSize();

    List<JLabel> labels = ContainerUtil.map(mergeFragments(textFragments), it ->
    {
      JLabel label = new JLabel("<html>" + it.getFirst() + "</html>", SwingConstants.CENTER);
      if (it.getSecond() != null) {
        label.setFont(it.getSecond());
      }
      return label;
    });

    IntConsumer setFontSize = size ->
    {
      for (JLabel label : labels) {
        // float cast -> size parameter
        label.setFont(label.getFont().deriveFont((float)size));
      }

      List<Integer> max = ContainerUtil.map(labels, it -> it.getFontMetrics(it.getFont()).getMaxAscent());

      int maxAscent = max.isEmpty() ? 0 : Collections.max(max);

      for (JLabel label : labels) {
        int ascent = label.getFontMetrics(label.getFont()).getMaxAscent();
        if (ascent < maxAscent) {
          label.setBorder(BorderFactory.createEmptyBorder(maxAscent - ascent, 0, 0, 0));
        }
        else {
          label.setBorder(null);
        }
      }
    };

    setFontSize.accept(fontSize);

    int frameWidth = ideFrame.getComponent().getWidth();
    if (frameWidth > 100) {
      while (labels.stream()
                   .mapToInt(value -> value.getPreferredSize().width)
                   .sum() > frameWidth - JBUI.scale(10) && fontSize > JBUI.scaleFontSize(12)) {
        setFontSize.accept(--fontSize);
      }
    }
    return labels;
  }

  private List<Pair<String, Font>> mergeFragments(List<Pair<String, Font>> textFragments) {
    List<Pair<String, Font>> result = new ArrayList<>();
    for (Pair<String, Font> item : textFragments) {
      Pair<String, Font> last = ContainerUtil.getLastItem(result);

      if (last != null && Comparing.equal(last.getSecond(), item.getSecond())) {
        result.remove(result.size() - 1); // remove last
        result.add(Pair.create(last.getFirst() + item.getFirst(), last.getSecond()));
      }
      else {
        result.add(item);
      }
    }
    return result;
  }

  @Override
  public void dispose() {
    phase = Phase.HIDDEN;
    if (!hint.isDisposed()) {
      hint.cancel();
    }

    Disposer.dispose(animator);
  }

  public void close() {
    Disposer.dispose(this);
  }
}
