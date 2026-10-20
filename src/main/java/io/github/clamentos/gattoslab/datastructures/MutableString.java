package io.github.clamentos.gattoslab.datastructures;

///
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

///
public final class MutableString implements CharSequence {

    ///
    private byte[] characters;
    private int index;

    ///
    public MutableString(final int initialCapacity) {

        this.characters = new byte[initialCapacity];
        this.index = 0;
    }

    ///..
    public MutableString(final byte[] characters) {

        this.characters = characters;
        this.index = characters.length;
    }

    ///
    public void append(final byte character) {

        if(this.index >= this.characters.length) this.grow();
        this.characters[this.index++] = character;
    }

    ///..
    public void append(final char character) {

        if(this.index + 1 >= this.characters.length) this.grow();
        this.characters[this.index++] = (byte)character;
    }

    ///..
    public void append(final CharSequence charSequence) {

        final int length = charSequence.length();
        if(length == 0) return;

        this.grow(length);

        for(int i = 0; i < length; i++) {

            this.characters[this.index++] = (byte)charSequence.charAt(i);
        }
    }

    ///..
    public void append(final byte[] characters) {

        final int length = characters.length;
        if(length == 0) return;

        this.grow(length);
        System.arraycopy(characters, 0, this.characters, this.index, length);
    }

    ///..
    public void clear() {

        this.index = 0;
    }

    ///..
    public void deleteLastChars(final int amount) {

        this.index -= amount;
    }

    ///..
    @Override
    public int length() {

        return this.index;
    }

    ///..
    @Override
    public char charAt(final int index) {

        return (char)(this.characters[index] & 0xFF);
    }

    ///..
    @Override
    public CharSequence subSequence(final int start, final int end) {

        return new MutableString(Arrays.copyOfRange(this.characters, start, end));
    }

    ///..
    @Override
    public boolean equals(final Object other) {

        if(this == other) return true;
        if(!(other instanceof MutableString)) return false;

        final MutableString casted = (MutableString)other;
        if(this.index != casted.index) return false;

        for(int i = 0; i < this.index; i++) {

            if(this.characters[i] != casted.characters[i]) return false;
        }

        return true;
    }

    ///..
    @Override
    public int hashCode() {

        int result = 1;

        for(int i = 0; i < this.index; i++) {

            result = 31 * result + this.characters[i];
        }

        return result;
    }

    ///..
    @Override
    public String toString() {

        return new String(this.characters, 0, this.index, StandardCharsets.ISO_8859_1);
    }

    ///.
    private void grow() {

        this.characters = Arrays.copyOf(this.characters, this.characters.length * 2);
    }

    ///..
    private void grow(final int length) {

        if(this.index + length >= this.characters.length) {

            this.characters = Arrays.copyOf(this.characters, this.characters.length + length);
        }
    }

    ///
}
