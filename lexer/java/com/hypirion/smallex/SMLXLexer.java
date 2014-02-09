package com.hypirion.smallex;

import java.io.Reader;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import clojure.lang.Keyword;

import com.hypirion.smallex.Item;

public class SMLXLexer implements Iterator<Item> {
    private static final Keyword
        ERROR = Keyword.intern(null, "error"),
        PAREN_START = Keyword.intern(null, "paren-start"),
        PAREN_END = Keyword.intern(null, "paren-end"),
        OP = Keyword.intern(null, "op"),
        SYMBOL = Keyword.intern(null, "symbol");

    private static final Keyword[] predefs;
    private static final Map<String, Keyword> predefined;

    static {
        String[] strPredefs = {"or", "cat", "star", "plus", "opt", "not", "def",
                               "alias"};

        // Create and populate predefs
        predefs = new Keyword[strPredefs.length];
        for (int i = 0; i < strPredefs.length; i++) {
            predefs[i] = Keyword.intern(null, strPredefs[i]);
        }

        // Create immutable predefined lookup table
        Map<String, Keyword> temp = new HashMap<String, Keyword>();
        for (Keyword k : predefs) {
            temp.put(k.getName(), k);
        }
        predefined = Collections.<String, Keyword>unmodifiableMap(temp);
    }

    private Reader reader;
    private StringBuilder str;
    private char cur;

    public SMLXLexer(Reader reader) {
        this.reader = reader;
        str = new StringBuilder();
        removeWhitespace();
        readOne();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        return cur != -1;
    }

    @Override
    public Item next() {
        // Clear up whitespace before returning.
        return new Item(ERROR, "not yet implemented");
    }

    private void readOne() {
        try {
            cur = (char) reader.read();
        } catch (IOException ioe) {
            // TODO: do something smart here.
        }
    }

    private void removeWhitespace() {

    }
}
