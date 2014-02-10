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
                return lexString();
            case '[':
                return lexCharSet();
            }
            // support symbols starting with alphabetic chars for now only.
            if (Character.isLetter(cur)) {
                return lexSymbol();
            }
            return new Item(ERROR,
                            String.format("unexpected char: '%c'", (char) cur));
        }
    }

    private Item lexString() {
        // shave off first value
        tryRead();
        StringBuilder sb = new StringBuilder();
        while (cur != '"') {
            if (cur == '\\') {
                tryRead();
                switch (cur) {
                case '\\':
                case '"':
                    sb.appendCodePoint(cur);
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case EOF:
                    return new Item(ERROR, "Assumed quoted character after " +
                                    "backslash (\\) in string, but was EOF.");
                case IO_ERROR:
                    return new Item(ERROR, ioError);
                default:
                    return new Item(ERROR, "Unknown quoted character after " +
                                    String.format("backslash (\\) in string ('%c').", cur));
                }
            } else {
                switch (cur) {
                case EOF:
                    return new Item(ERROR, "Found end of file, but string" +
                                    " is still open.");
                case IO_ERROR:
                    return new Item(ERROR, ioError);
                default:
                    sb.appendCodePoint(cur);
                }
            }
            tryRead();
        }
        // flush out last '"'
        tryRead();
        removeWhitespace();
        return new Item(STRING, sb.toString());
    }

    private Item lexCharSet() {
        // shave off first value
        tryRead();
        StringBuilder sb = new StringBuilder();
        while (cur != ']') {
            if (cur == '\\') {
                tryRead();
                switch (cur) {
                case '\\':
                case ']':
                    sb.appendCodePoint(cur);
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case EOF:
                    return new Item(ERROR, "Assumed quoted character after " +
                                    "backslash (\\) in char set, but was EOF.");
                case IO_ERROR:
                    return new Item(ERROR, ioError);
                default:
                    return new Item(ERROR, "Unknown quoted character after " +
                                    String.format("backslash (\\) in char set ('%c').", cur));
                }
            } else {
                switch (cur) {
                case EOF:
                    return new Item(ERROR, "Found end of file, but char set " +
                                    "is still open.");
                case IO_ERROR:
                    return new Item(ERROR, ioError);
                default:
                    sb.appendCodePoint(cur);
                }
            }
            tryRead();
        }
        // flush out ']'
        tryRead();
        removeWhitespace();
        return new Item(CHAR_SET, sb.toString());
    }

    private Item lexSymbol() {
        StringBuilder sb = new StringBuilder();
        // TODO: Change to support Clojure-like symbol subset
        do {
            sb.appendCodePoint(cur);
            tryRead();
        } while (Character.isLetterOrDigit(cur));
        if (cur == IO_ERROR) {
            return new Item(ERROR, ioError);
        }
        removeWhitespace();
        String sym = sb.toString();
        if (predefined.containsKey(sym)) {
            return predefined.get(sym);
        } else {
            return new Item(SYMBOL, sym);
        }
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
