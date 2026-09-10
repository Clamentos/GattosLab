package io.github.clamentos.gattoslab.datastructures;

///
import java.util.Arrays;

///
public final class FastAsciiJoiner {

    ///
    private CharSequence[] elements;
    private int index;

    ///
    public FastAsciiJoiner(final int initialCapacity) {

        this.elements = new CharSequence[initialCapacity];
        this.index = 0;
    }

    ///
    public void add(final CharSequence value) {

        final int length = this.elements.length;
        if(this.index == length) this.elements = Arrays.copyOf(this.elements, length > 0 ? length * 2 : 1);

        this.elements[this.index++] = value;
    }

    ///..
    public void replaceLast(final CharSequence replacement) {

        this.elements[this.index - 1] = replacement;
    }

    ///..
    public void deleteLast() {

        if(this.index > 0) this.index--;
    }

    ///..
    public byte[] toByteArray() {

        final int length = this.index > 0 ? this.index : 0;
        int totalLength = 0;

        for(int i = 0; i < length; i++) {

            totalLength += this.elements[i].length();
        }

        final byte[] bytes = new byte[totalLength];
        int idx = 0;

        for(int i = 0; i < length; i++) {

            final CharSequence element = this.elements[i];
            final int elementLength = element.length();

            for(int j = 0; j < elementLength; j++) {

                bytes[idx++] = (byte)element.charAt(j);
            }
        }

        return bytes;
    }

    ///..
    public char[] toCharArray() {

        final int length = this.index > 0 ? this.index : 0;
        int totalLength = 0;

        for(int i = 0; i < length; i++) {

            totalLength += this.elements[i].length();
        }

        final char[] chars = new char[totalLength];
        int idx = 0;

        for(int i = 0; i < length; i++) {

            final CharSequence element = this.elements[i];
            final int elementLength = element.length();

            for(int j = 0; j < elementLength; j++) {

                chars[idx++] = element.charAt(j);
            }
        }

        return chars;
    }

    ///
}
