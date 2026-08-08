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
package consulo.web.internal.execution.terminal;

import com.jediterm.core.Color;
import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.CursorShape;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.Terminal;
import com.jediterm.terminal.TerminalDataStream;
import com.jediterm.terminal.TerminalDisplay;
import com.jediterm.terminal.emulator.JediEmulator;
import com.jediterm.terminal.emulator.mouse.MouseFormat;
import com.jediterm.terminal.emulator.mouse.MouseMode;
import com.jediterm.terminal.model.JediTerminal;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalSelection;
import com.jediterm.terminal.model.TerminalTextBuffer;
import consulo.logging.Logger;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Headless jediterm model fed with the very same bytes that are sent to xterm.js, so that the server side
 * can answer for the screen content xterm.js is showing.
 *
 * @author VISTALL
 * @since 2026-08-08
 */
public class WebTerminalMirror {
    private static final Logger LOG = Logger.getInstance(WebTerminalMirror.class);

    private static final int QUEUE_CAPACITY = 1024;

    private final TerminalTextBuffer myTextBuffer;
    private final JediTerminal myTerminal;
    private final CharQueueDataStream myDataStream = new CharQueueDataStream();

    private volatile Thread myPumpThread;
    private volatile boolean myStopped;

    public WebTerminalMirror(int columns, int rows) {
        StyleState styleState = new StyleState();
        myTextBuffer = new TerminalTextBuffer(columns, rows, styleState);
        myTerminal = new JediTerminal(new HeadlessDisplay(), myTextBuffer, styleState);
    }

    public Terminal getTerminal() {
        return myTerminal;
    }

    public TerminalTextBuffer getTextBuffer() {
        return myTextBuffer;
    }

    public void start(BiFunction<TerminalDataStream, Terminal, JediEmulator> emulatorFactory) {
        JediEmulator emulator = emulatorFactory.apply(myDataStream, myTerminal);

        Thread thread = new Thread(() -> {
            while (!myStopped) {
                try {
                    if (!emulator.hasNext()) {
                        break;
                    }
                    emulator.next();
                }
                catch (Exception e) {
                    LOG.warn("terminal mirror stopped", e);
                    break;
                }
            }
        }, "Web Terminal Mirror");
        thread.setDaemon(true);
        myPumpThread = thread;
        thread.start();
    }

    /**
     * Hands the mirror the same characters that were written to xterm.js.
     */
    public void write(char[] data, int offset, int length) {
        myDataStream.put(data, offset, length);
    }

    public void resize(int columns, int rows) {
        myTerminal.resize(new TermSize(columns, rows), RequestOrigin.User);
    }

    public void stop() {
        myStopped = true;
        Thread thread = myPumpThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * {@link TerminalDataStream} which blocks until characters produced by the pty arrive.
     */
    private class CharQueueDataStream implements TerminalDataStream {
        private final BlockingQueue<Character> myQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        private void put(char[] data, int offset, int length) {
            for (int i = 0; i < length; i++) {
                try {
                    myQueue.put(data[offset + i]);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        @Override
        public char getChar() throws IOException {
            while (!myStopped) {
                try {
                    Character c = myQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (c != null) {
                        return c;
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            throw new IOException("terminal mirror closed");
        }

        @Override
        public void pushChar(char c) {
            myQueue.offer(c);
        }

        @Override
        public String readNonControlCharacters(int maxChars) throws IOException {
            StringBuilder sb = new StringBuilder();
            while (sb.length() < maxChars) {
                Character c = myQueue.peek();
                if (c == null || c.charValue() < 32) {
                    break;
                }
                myQueue.poll();
                sb.append(c.charValue());
            }
            if (sb.isEmpty()) {
                char c = getChar();
                if (c < 32) {
                    pushBackBuffer(new char[]{c}, 1);
                    return "";
                }
                sb.append(c);
            }
            return sb.toString();
        }

        @Override
        public void pushBackBuffer(char[] chars, int length) {
            for (int i = length - 1; i >= 0; i--) {
                myQueue.offer(chars[i]);
            }
        }

        @Override
        public boolean isEmpty() {
            return myQueue.isEmpty();
        }
    }

    private static class HeadlessDisplay implements TerminalDisplay {
        private String myWindowTitle = "";

        @Override
        public void setCursor(int x, int y) {
        }

        @Override
        public void setCursorShape(CursorShape cursorShape) {
        }

        @Override
        public void beep() {
        }

        @Override
        public void scrollArea(int scrollRegionTop, int dy, int scrollRegionBottom) {
        }

        @Override
        public void setCursorVisible(boolean isCursorVisible) {
        }

        @Override
        public void useAlternateScreenBuffer(boolean useAlternateScreenBuffer) {
        }

        @Override
        public String getWindowTitle() {
            return myWindowTitle;
        }

        @Override
        public void setWindowTitle(String windowTitle) {
            myWindowTitle = windowTitle;
        }

        @Override
        public TerminalSelection getSelection() {
            return null;
        }

        @Override
        public void terminalMouseModeSet(MouseMode mouseMode) {
        }

        @Override
        public void setMouseFormat(MouseFormat mouseFormat) {
        }

        @Override
        public boolean ambiguousCharsAreDoubleWidth() {
            return false;
        }

        @Override
        public Color getWindowForeground() {
            return null;
        }

        @Override
        public Color getWindowBackground() {
            return null;
        }
    }
}
