package com.hypirion.smallex;

import java.io.Reader;
import java.util.Iterator;

import clojure.lang.Keyword;

import com.hypirion.smallex.Item;

public class SMLXLexer implements Iterator<Item> {
    private static final Keyword ERROR = Keyword.intern(null, "error");

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
        return new Item(ERROR, "not yet implemented");
    }

    @Override
    public boolean hasNext() {
        return true;
    }
}
