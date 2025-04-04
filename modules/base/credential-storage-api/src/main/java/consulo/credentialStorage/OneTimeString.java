package consulo.credentialStorage;

import consulo.util.collection.ArrayUtil;
import consulo.util.lang.CharArrayCharSequence;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.StringUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * clearable only if specified explicitly.
 * <p>
 * Case
 * 1) you create OneTimeString manually on user input.
 * 2) you store it in CredentialStore
 * 3) you consume it... BUT native credentials store do not store credentials immediately - write is postponed, so, will be an critical error.
 * <p>
 */
public final class OneTimeString extends CharArrayCharSequence {
    // Indicates whether the underlying char array should be cleared after consumption.
    private final boolean clearable;
    // Keeps track of whether this instance has been "consumed".
    private final AtomicReference<String> consumed = new AtomicReference<>(null);

    // Primary constructor: takes a char array, an offset, a length and the clearable flag.
    public OneTimeString(char[] value, int offset, int length, boolean clearable) {
        // The superclass constructor expects (char[] array, int start, int end)
        super(value, offset, offset + length);
        this.clearable = clearable;
    }

    // Overloaded constructor with default offset=0, length=value.length, clearable=false.
    public OneTimeString(char[] value) {
        this(value, 0, value.length, false);
    }

    // Overloaded constructor taking a String.
    public OneTimeString(String value) {
        this(value.toCharArray(), 0, value.length(), false);
    }

    // Private helper method to mark consumption.
    private void consume(boolean willBeCleared) {
        if (!clearable) {
            return;
        }
        if (!willBeCleared) {
            String current = consumed.get();
            if (current != null) {
                throw new IllegalStateException("Already consumed: " + current + "\n---\n");
            }
        }
        else {
            if (!consumed.compareAndSet(null, ExceptionUtil.currentStackTrace())) {
                throw new IllegalStateException("Already consumed at " + consumed.get());
            }
        }
    }

    /**
     * Returns the string representation of this OneTimeString.
     * If {@code clear} is true, the underlying characters will be cleared after conversion.
     *
     * @param clear whether to clear the contents after conversion
     * @return the string representation
     */
    public String toString(boolean clear) {
        consume(clear);
        String result = super.toString();
        clear();
        return result;
    }

    // Default toString() calls toString(false) so that clearing is not performed.
    @Override
    public String toString() {
        return toString(false);
    }

    /**
     * Converts the contents to a UTF-8 encoded byte array.
     * By default, the underlying characters are cleared.
     *
     * @param clear whether to clear the characters after conversion
     * @return the UTF-8 encoded byte array
     */
    public byte[] toByteArray(boolean clear) {
        consume(clear);
        CharBuffer charBuffer = CharBuffer.wrap(myChars, myStart, length());
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        if (clear) {
            clear();
        }
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    // Overloaded toByteArray() with default clear=true.
    public byte[] toByteArray() {
        return toByteArray(true);
    }

    // Clears the underlying character array if clearable.
    private void clear() {
        if (clearable) {
            Arrays.fill(myChars, myStart, myEnd, '\u0000');
        }
    }

    /**
     * Converts the contents to a character array.
     * By default, the underlying characters are cleared after the conversion.
     *
     * @param clear whether to clear the characters after conversion
     * @return a new character array containing the data
     */
    public char[] toCharArray(boolean clear) {
        consume(clear);
        int len = length();
        if (clear) {
            char[] result = new char[len];
            System.arraycopy(myChars, myStart, result, 0, len);
            clear();
            return result;
        }
        else {
            // Returns the underlying array (caution: may expose mutable internal state)
            return myChars;
        }
    }

    // Overloaded toCharArray() with default clear=true.
    public char[] toCharArray() {
        return toCharArray(true);
    }

    /**
     * Clones this OneTimeString by copying its contents.
     *
     * @param clear     whether to clear the original contents during cloning
     * @param clearable whether the new instance should be clearable
     * @return a new OneTimeString instance with copied characters
     */
    public OneTimeString clone(boolean clear, boolean clearable) {
        return new OneTimeString(toCharArray(clear), 0, length(), clearable);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CharSequence) {
            return StringUtil.equals(this, (CharSequence) obj);
        }
        return super.equals(obj);
    }

    /**
     * Appends the characters to the given StringBuilder.
     *
     * @param builder the StringBuilder to append to
     */
    public void appendTo(StringBuilder builder) {
        consume(false);
        builder.append(myChars, myStart, length());
    }

    /**
     * Static factory method to create a OneTimeString from a byte array.
     * After conversion, the specified portion of the byte array is cleared.
     *
     * @param value     the source byte array
     * @param offset    the starting offset in the byte array
     * @param length    the number of bytes to decode
     * @param clearable whether the resulting OneTimeString is clearable
     * @return a new OneTimeString instance
     * @throws Exception if a decoding error occurs
     */
    public static OneTimeString fromByteArray(byte[] value, int offset, int length, boolean clearable) throws Exception {
        if (length == 0) {
            return new OneTimeString(ArrayUtil.EMPTY_CHAR_ARRAY);
        }

        CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        int charArraySize = (int) (value.length * charsetDecoder.maxCharsPerByte());
        char[] charArray = new char[charArraySize];
        charsetDecoder.reset();
        CharBuffer charBuffer = CharBuffer.wrap(charArray);
        ByteBuffer byteBuffer = ByteBuffer.wrap(value, offset, length);
        CoderResult cr = charsetDecoder.decode(byteBuffer, charBuffer, true);
        if (!cr.isUnderflow()) {
            cr.throwException();
        }
        cr = charsetDecoder.flush(charBuffer);
        if (!cr.isUnderflow()) {
            cr.throwException();
        }
        Arrays.fill(value, offset, offset + length, (byte) 0);
        return new OneTimeString(charArray, 0, charBuffer.position(), clearable);
    }

    public static OneTimeString fromByteArray(byte[] value, boolean clearable) throws Exception {
        return fromByteArray(value, 0, value.length, clearable);
    }

    public static OneTimeString fromByteArray(byte[] value) throws Exception {
        return fromByteArray(value, 0, value.length, false);
    }

    public static OneTimeString fromByteArray(byte[] value, int offset, int length) throws Exception {
        return fromByteArray(value, offset, length, false);
    }
}
