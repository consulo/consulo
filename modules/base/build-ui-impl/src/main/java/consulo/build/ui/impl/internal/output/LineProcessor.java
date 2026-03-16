// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.output;


import java.io.Closeable;

abstract class LineProcessor implements Appendable, Closeable {
    private StringBuilder myLineBuilder = new StringBuilder();

    abstract void process(String line);

    @Override
    public LineProcessor append(CharSequence csq) {
        for (int i = 0; i < csq.length(); i++) {
            append(csq.charAt(i));
        }
        return this;
    }

    @Override
    public LineProcessor append(CharSequence csq, int start, int end) {
        append(csq.subSequence(start, end));
        return this;
    }

    @Override
    public LineProcessor append(char c) {
        if (myLineBuilder == null) throw new IllegalStateException("The line processor was closed");
        if (c == '\n') {
            int length = myLineBuilder.length();
            if (length > 0 && myLineBuilder.charAt(length - 1) == '\r') {
                myLineBuilder.deleteCharAt(length - 1);
            }
            flushBuffer();
        }
        else {
            myLineBuilder.append(c);
        }
        return this;
    }

    @Override
    public void close() {
        if (myLineBuilder != null) {
            flushBuffer();
            myLineBuilder = null;
        }
    }

    private void flushBuffer() {
        String line = myLineBuilder.toString();
        myLineBuilder.setLength(0);
        process(line);
    }
}