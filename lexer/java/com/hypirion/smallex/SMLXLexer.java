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
        SYMBOL = Keyword.intern(null, "symbol"),
        CHAR_SET = Keyword.intern(null, "char-set"),
        STRING = Keyword.intern(null, "string");

    private static final Keyword[] predefs;
    private static final Map<String, Item> predefined;

    static {
        String[] strPredefs = {"or", "cat", "star", "plus", "opt", "not", "def",
                               "alias"};

        // Create and populate predefs
        predefs = new Keyword[strPredefs.length];
        for (int i = 0; i < strPredefs.length; i++) {
            predefs[i] = Keyword.intern(null, strPredefs[i]);
        }

        // Create immutable predefined lookup table
        Map<String, Item> temp = new HashMap<String, Item>();
        for (Keyword k : predefs) {
            temp.put(k.getName(), new Item(k, k));
        }
        predefined = Collections.<String, Item>unmodifiableMap(temp);
    }

    private static final Item
        PAREN_START_ITEM = new Item(PAREN_START, "("),
        PAREN_END_ITEM = new Item(PAREN_END, ")");

    private static final int NOT_INITIALISED = -3;
    private static final int IO_ERROR = -2;
    private static final int EOF = -1;

    private Reader reader;
    private int cur;
    private IOException ioError;

    public SMLXLexer(Reader reader) {
        this.reader = reader;
        ioError = null;
        cur = NOT_INITIALISED;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        return cur != EOF;
    }

    @Override
    public Item next() {
        while (true) {
            switch (cur) {
            case NOT_INITIALISED:
                init();
                continue;
            case IO_ERROR:
                return new Item(ERROR, ioError);
            case EOF:
                return new Item(ERROR, "Unexpectedly found EOF.");
            case '(':
                tryRead();
                removeWhitespace();
                return PAREN_START_ITEM;
            case ')':
                tryRead();
                removeWhitespace();
                return PAREN_END_ITEM;
            case '"':
                return lexCatString();
            case '[':
            }
            // support symbols starting with alphabetic chars for now only.
            if (Character.isLetter((char) cur)) {
                return lexSymbol();
            }
            return new Item(ERROR,
                            String.format("unexpected char: '%c'", (char) cur));
        }
    }

    private Item lexCatString() {
        // parse cat-string here. Keep in mind escaping.
        return null;
    }

    private Item lexOrString() {
        // parse or-string here. Keep in mind escaping.
        return null;
    }

    private Item lexSymbol() {
        return null;
    }

    // Always called before data
    private void removeWhitespace() {
        while (true) {
            // Remove whitespace
            while (Character.isWhitespace(cur)) {
                tryRead();
            }
            // Remove comments
            if (cur == ';') {
                do {
                    tryRead();
                } while (cur != '\n');
            } else {
                break;
                // We're done removing whitespace if no ';' was found.
            }
        }
    }

    // Safe to call this one until -1 is returned. Has to check the ioError for
    // non-null values though.
    private void tryRead() {
        try {
            cur = reader.read();
        } catch (IOException e) {
            ioError = e;
            cur = IO_ERROR;
        }
    }

    // We could theoretically do this in the parser ctor, but would force
    // reading early.
    private void init() {
        if (cur == NOT_INITIALISED) {
            tryRead();
            removeWhitespace();
        }
    }
}
