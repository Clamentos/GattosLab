package io.github.clamentos.gattoslab.datastructures;

///
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
        this.appendChar(character);
    }

    ///..
    public void append(final CharSequence charSequence) {

        if(charSequence.isEmpty()) return;

        final int length = charSequence.length();
        final int normalizedLength = length << 2;

        if(this.index + normalizedLength >= this.characters.length) {

            this.characters = Arrays.copyOf(this.characters, this.characters.length + normalizedLength);
        }

        for(int i = 0; i < length; i++) {

            final char character = charSequence.charAt(i);
            this.appendChar(character);
        }
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
    public CharSequence subSequence(final int start, final int length) {

        return new MutableString(Arrays.copyOfRange(this.characters, start, length - 1));
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

        return new String(this.characters, 0, this.index);
    }

    ///.
    private void grow() {

        this.characters = Arrays.copyOf(this.characters, this.characters.length * 2);
    }

    ///..
    private void appendChar(final char data) {

        if(data <= 0x007F) {

            this.characters[this.index++] = (byte)(data & 0x00FF);
        }

        else {

            this.characters[this.index++] = (byte)((data & 0xFF00) >> 8);
            this.characters[this.index++] = (byte)(data & 0x00FF);
        }
    }

    ///
}
