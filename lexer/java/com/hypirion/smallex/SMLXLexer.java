import java.io.Reader;
import java.util.Iterator;

import com.hypirion.smallex.Item;

public class SMLXLexer implements Iterator<Item> {
    private Reader reader;

    public SMLXLexer(Reader reader) {
        this.reader = reader;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Item next() {
        return null;
    }

    @Override
    public boolean hasNext() {
        return false;
    }
}
