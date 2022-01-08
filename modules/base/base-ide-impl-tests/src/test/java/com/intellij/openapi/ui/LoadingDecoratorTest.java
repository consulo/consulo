package com.intellij.openapi.ui;

import consulo.disposer.Disposable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoadingDecoratorTest {
  public static void main(String[] args) {
    final JFrame frame = new JFrame();
    frame.getContentPane().setLayout(new BorderLayout());

    final JPanel content = new JPanel(new BorderLayout());

    final LoadingDecorator loadingTree = new LoadingDecorator(new JComboBox(), Disposable.newDisposable(), -1);

    content.add(loadingTree.getComponent(), BorderLayout.CENTER);

    final JCheckBox loadingCheckBox = new JCheckBox("Loading");
    loadingCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (loadingTree.isLoading()) {
          loadingTree.stopLoading();
        } else {
          loadingTree.startLoading(false);
        }
      }
    });

    content.add(loadingCheckBox, BorderLayout.SOUTH);


    frame.getContentPane().add(content, BorderLayout.CENTER);

    frame.setBounds(300, 300, 300, 300);
    frame.show();
  }
}
