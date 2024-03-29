/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.internal.arrangement;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Denis Zhdanov
 * @since 11/8/12 10:46 AM
 */
public class ArrangementAnimationManager implements ArrangementAnimationPanel.Listener, ActionListener {

  @Nonnull
  private final Timer myTimer = new Timer(ArrangementConstants.ANIMATION_STEPS_TIME_GAP_MILLIS, this);

  @Nonnull
  private final ArrangementAnimationPanel myAnimationPanel;
  @Nonnull
  private final Callback                  myCallback;
  
  private boolean myFinished;

  public ArrangementAnimationManager(@Nonnull ArrangementAnimationPanel panel, @Nonnull Callback callback) {
    myAnimationPanel = panel;
    myCallback = callback;
    myAnimationPanel.setListener(this);
  }

  public void startAnimation() {
    myAnimationPanel.startAnimation();
    myCallback.onAnimationIteration(false);
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    myTimer.stop();
    myFinished = !myAnimationPanel.nextIteration();
    myCallback.onAnimationIteration(myFinished);
  }

  @Override
  public void onPaint() {
    if (!myFinished && !myTimer.isRunning()) {
      myTimer.start();
    }
  }

  public interface Callback {
    void onAnimationIteration(boolean finished);
  }
}
