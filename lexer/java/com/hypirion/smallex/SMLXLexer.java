package com.hypirion.smallex;

import java.io.Reader;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import clojure.lang.Keyword;
import clojure.lang.PersistentArrayMap;

import com.hypirion.smallex.Item;

public class SMLXLexer implements Iterator<Item> {
    private static final Keyword
        ERROR = Keyword.intern(null, "error"),
        PAREN_START = Keyword.intern(null, "paren-start"),
        PAREN_END = Keyword.intern(null, "paren-end"),
        OP = Keyword.intern(null, "op"),
        SYMBOL = Keyword.intern(null, "symbol"),
        CHAR_SET = Keyword.intern(null, "char-set"),
        STRING = Keyword.intern(null, "string"),
        COL = Keyword.intern(null, "col"),
        LINE = Keyword.intern(null, "line");

    private static final Map<String, Item> predefined;

    static {
        String[] ops = {"or", "cat", "star", "plus", "opt", "not"};
        String[] other = {"def", "alias"};

        // Create immutable predefined lookup table
        Map<String, Item> temp = new HashMap<String, Item>();
        for (String op : ops) {
            Keyword k = Keyword.intern(null, op);
            temp.put(op, new Item(OP, k));
        }
        for (String o : other) {
            Keyword k = Keyword.intern(null, o);
            temp.put(o, new Item(k, k));
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
    private int cur, start_line, line, col, start_col;
    private IOException ioError;

    public SMLXLexer(Reader reader) {
        this.reader = reader;
        ioError = null;
        cur = NOT_INITIALISED;
        start_line = 1;
        line = 1;
        start_col = 0;
        col = -1;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        if (cur == NOT_INITIALISED) {
            init();
        }
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
                return error(ioError);
            case EOF:
                return error("Unexpectedly found EOF.");
            case '(':
                tryRead();
                return item(PAREN_START_ITEM);
            case ')':
                tryRead();
                return item(PAREN_END_ITEM);
            case '"':
                return lexDelimited('"', STRING, "string");
            case '[':
                return lexDelimited(']', CHAR_SET, "char set");
            }
            // support symbols starting with alphabetic chars for now only.
            if (Character.isLetter(cur) || "+*%&?_-$!".indexOf(cur) >= 0) {
                return lexSymbol();
            }
            return error(String.format("unexpected char: '%c'", (char) cur));
        }
    }

    private Item lexDelimited(char end, Keyword delimitedType,
                              String delimitedName) {
        // shave off first value
        tryRead();
        StringBuilder sb = new StringBuilder();
        while (cur != end) {
            if (cur == '\\') {
                tryRead();
                switch (cur) {
                case '\\':
                case '[':
                case ']':
                case '"':
                case ';':
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
                case 'u': {
                    // next 4 chars should be hex
                    int code = 0;
                    for (int i = 0; i < 4; i++) {
                        tryRead();
                        int conv = Character.digit(cur, 0x10);
                        if (conv == -1){
                            switch (cur) {
                            case IO_ERROR:
                                return error(ioError);
                            case EOF:
                                return error("Expected 4 hexadecimal values, but "+
                                             "got EOF within unicode escaping.");
                            }
                            return error("Malformed unicode escape: " +
                                         String.format("Character '%c' was given", cur));
                        } else {
                            code = 0x10 * code + conv;
                        }
                    }
                    // conversion went well, insert converted value into string:
                    sb.appendCodePoint(code);
                    break;
                }
                case 'x': {
                    // next 2 chars should be hex
                    int code = 0;
                    for (int i = 0; i < 2; i++) {
                        tryRead();
                        int conv = Character.digit(cur, 0x10);
                        if (conv == -1){
                            switch (cur) {
                            case IO_ERROR:
                                return error(ioError);
                            case EOF:
                                return error("Expected 4 hexadecimal values, but "+
                                             "got EOF within unicode escaping.");
                            }
                            return error("Malformed unicode escape: " +
                                         String.format("Character '%c' was given", cur));
                        } else {
                            code = 0x10 * code + conv;
                        }
                    }
                    // conversion went well, insert converted value into string:
                    sb.appendCodePoint(code);
                    break;
                }
                case EOF:
                    return error("Assumed quoted character after " +
                                 "backslash (\\) in char set, but was EOF.");
                case IO_ERROR:
                    return error(ioError);
                default:
                    return error("Unknown quoted character after " +
                                 String.format("backslash (\\) in char set ('%c').", cur));
                }
            } else {
                switch (cur) {
                case EOF:
                    return error(String.format("Found end of file, but %s is still open.",
                                               delimitedName));
                case IO_ERROR:
                    return error(ioError);
                default:
                    sb.appendCodePoint(cur);
                }
            }
            tryRead();
        }
        // flush out ']'
        tryRead();
        return item(delimitedType, sb.toString());
    }

    private Item lexSymbol() {
        StringBuilder sb = new StringBuilder();
        // TODO: Change to support Clojure-like symbol subset
        do {
            sb.appendCodePoint(cur);
            tryRead();
        } while (Character.isLetterOrDigit(cur) ||
                 "+*%&?_-$!'/".indexOf(cur) >= 0);
        if (cur == IO_ERROR) {
            return error(ioError);
        }
        String sym = sb.toString();
        if (predefined.containsKey(sym)) {
            return item(predefined.get(sym));
        } else {
            return item(SYMBOL, sym);
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
            col++;
            if (cur == '\n') {
                // LineNumberReader does this differently. Not sure if it's
                // justified to implement such functionality or wrap the reader
                // in a LineNumberReader.
                line++;
                col = -1;
            }
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

    private Item error(String errmsg) {
        cur = EOF;
        return item(ERROR, errmsg);
    }
    
    private Item error(IOException ioe) {
        cur = EOF;
        return item(ERROR, ioe);
    }

    private Item item(Keyword type, Object value) {
        Item it = (Item) new Item(type, value).withMeta(metaMap());
        removeWhitespace();
        // Next item—if existing—should be starting at current position.
        start_line = line;
        start_col = col;
        return it;
    }

    private Item item(Item basis) {
        return item((Keyword) basis.type, basis.value);
    }

    private PersistentArrayMap metaMap() {
        Object[] data = new Object[]{LINE, start_line, COL, start_col};
        return PersistentArrayMap.createWithCheck(data);
    }
}
