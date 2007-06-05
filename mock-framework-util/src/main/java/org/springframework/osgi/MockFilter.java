package org.springframework.osgi;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.Vector;
import java.util.Dictionary;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.security.AccessController;

public class MockFilter implements Filter {
    static class Parser {

        protected void parse(MockFilter parent) throws InvalidSyntaxException {
            try {
                parse_filter(parent);
            } catch (ArrayIndexOutOfBoundsException _ex) {
                throw new InvalidSyntaxException(
                                                 "FILTER_TERMINATED_ABRUBTLY",
                                                 filterstring);
            }
            if (pos != filter.length)
                throw new InvalidSyntaxException(
                                                 "Msg.FILTER_TRAILING_CHARACTERS",
                                                 filterstring);
            else {
            }
        }

        protected void parse_filter(MockFilter parent) throws InvalidSyntaxException {
            skipWhiteSpace();
            if (filter[pos] != '(')
                throw new InvalidSyntaxException("FILTER_MISSING_LEFTPAREN",
                                                 filterstring);
            pos++;
            parse_filtercomp(parent);
            skipWhiteSpace();
            if (filter[pos] != ')') {
                throw new InvalidSyntaxException(
                                                 "FILTER_MISSING_RIGHTPAREN",
                                                 filterstring);
            } else {
                pos++;
                skipWhiteSpace();
            }
        }

        protected void parse_filtercomp(MockFilter parent) throws InvalidSyntaxException {
            skipWhiteSpace();
            char c = filter[pos];
            switch (c) {
                case 38: // '&'
                    pos++;
                    parse_and(parent);
                    break;

                case 124: // '|'
                    pos++;
                    parse_or(parent);
                    break;

                case 33: // '!'
                    pos++;
                    parse_not(parent);
                    break;

                default:
                    parse_item(parent);
                    break;
            }
        }

        protected void parse_and(MockFilter parent) throws InvalidSyntaxException {
            skipWhiteSpace();
            if (filter[pos] != '(')
                throw new InvalidSyntaxException(
                                                 "FILTER_MISSING_LEFTPAREN",
                                                 filterstring);
            Vector operands = new Vector(10, 10);
            while (filter[pos] == '(') {
                MockFilter child = new MockFilter();
                parse_filter(child);
                operands.addElement(child);
            }
            int size = operands.size();
            MockFilter children[] = new MockFilter[size];
            operands.copyInto(children);
            parent.setFilter(7, null, children);
        }

        protected void parse_or(MockFilter parent) throws InvalidSyntaxException {
            skipWhiteSpace();
            if (filter[pos] != '(')
                throw new InvalidSyntaxException(
                                                 "FILTER_MISSING_LEFTPAREN",
                                                 filterstring);
            Vector operands = new Vector(10, 10);
            while (filter[pos] == '(') {
                MockFilter child = new MockFilter();
                parse_filter(child);
                operands.addElement(child);
            }
            int size = operands.size();
            MockFilter children[] = new MockFilter[size];
            operands.copyInto(children);
            parent.setFilter(8, null, children);
        }

        protected void parse_not(MockFilter parent) throws InvalidSyntaxException {
            skipWhiteSpace();
            if (filter[pos] != '(') {
                throw new InvalidSyntaxException(
                                                 "FILTER_MISSING_LEFTPAREN",
                                                 filterstring);
            } else {
                MockFilter child = new MockFilter();
                parse_filter(child);
                parent.setFilter(9, null, child);
            }
        }

        protected void parse_item(MockFilter parent) throws InvalidSyntaxException {
            String attr = parse_attr();
            skipWhiteSpace();
            switch (filter[pos]) {
                default:
                    break;

                case 126: // '~'
                    if (filter[pos + 1] == '=') {
                        pos += 2;
                        parent.setFilter(2, attr, parse_value());
                        return;
                    }
                    break;

                case 62: // '>'
                    if (filter[pos + 1] == '=') {
                        pos += 2;
                        parent.setFilter(3, attr, parse_value());
                        return;
                    }
                    break;

                case 60: // '<'
                    if (filter[pos + 1] == '=') {
                        pos += 2;
                        parent.setFilter(4, attr, parse_value());
                        return;
                    }
                    break;

                case 61: // '='
                    if (filter[pos + 1] == '*') {
                        int oldpos = pos;
                        pos += 2;
                        skipWhiteSpace();
                        if (filter[pos] == ')') {
                            parent.setFilter(5, attr, null);
                            return;
                        }
                        pos = oldpos;
                    }
                    pos++;
                    Object string = parse_substring();
                    if (string instanceof String)
                        parent.setFilter(1, attr, string);
                    else
                        parent.setFilter(6, attr, string);
                    return;
            }
            throw new InvalidSyntaxException(
                                             "FILTER_INVALID_OPERATOR",
                                             filterstring);
        }

        protected String parse_attr() throws InvalidSyntaxException {
            skipWhiteSpace();
            int begin = pos;
            int end = pos;
            for (char c = filter[pos]; "~<>=()".indexOf(c) == -1; c = filter[pos]) {
                pos++;
                if (!Character.isWhitespace(c))
                    end = pos;
            }

            int length = end - begin;
            if (length == 0)
                throw new InvalidSyntaxException(
                                                 "FILTER_MISSING_ATTR",
                                                 filterstring);
            else
                return new String(filter, begin, length);
        }

        protected String parse_value() throws InvalidSyntaxException {
            StringBuffer sb = new StringBuffer(filter.length - pos);
            label0: do {
                char c = filter[pos];
                switch (c) {
                    case 41: // ')'
                        break label0;

                    case 40: // '('
                        throw new InvalidSyntaxException(
                                                         "FILTER_INVALID_VALUE",
                                                         filterstring);

                    case 92: // '\\'
                        pos++;
                        c = filter[pos];
                        // fall through

                    default:
                        sb.append(c);
                        pos++;
                        break;
                }
            } while (true);
            if (sb.length() == 0)
                throw new InvalidSyntaxException(
                                                 "FILTER_MISSING_VALUE",
                                                 filterstring);
            else
                return sb.toString();
        }

        protected Object parse_substring() throws InvalidSyntaxException {
            StringBuffer sb = new StringBuffer(filter.length - pos);
            Vector operands = new Vector(10, 10);
            label0: do {
                char c = filter[pos];
                switch (c) {
                    case 41: // ')'
                        if (sb.length() > 0)
                            operands.addElement(sb.toString());
                        break label0;

                    case 40: // '('
                        throw new InvalidSyntaxException(
                                                         "FILTER_INVALID_VALUE",
                                                         filterstring);

                    case 42: // '*'
                        if (sb.length() > 0)
                            operands.addElement(sb.toString());
                        sb.setLength(0);
                        operands.addElement(null);
                        pos++;
                        break;

                    case 92: // '\\'
                        pos++;
                        c = filter[pos];
                        // fall through

                    default:
                        sb.append(c);
                        pos++;
                        break;
                }
            } while (true);
            int size = operands.size();
            if (size == 0)
                throw new InvalidSyntaxException(
                                                 "FILTER_MISSING_VALUE",
                                                 filterstring);
            if (size == 1) {
                Object single = operands.elementAt(0);
                if (single != null)
                    return single;
            }
            String strings[] = new String[size];
            operands.copyInto(strings);
            return strings;
        }

        protected void skipWhiteSpace() {
            //noinspection StatementWithEmptyBody
            for (int length = filter.length; pos < length
                                             && Character.isWhitespace(filter[pos]); pos++)
                ;
        }

        protected String filterstring;
        protected char filter[];
        protected int pos;

        protected Parser(String filterstring) {
            this.filterstring = filterstring;
            filter = filterstring.toCharArray();
            pos = 0;
        }
    }

    static class SetAccessibleAction implements PrivilegedAction {

        public Object run() {
            constructor.setAccessible(true);
            return null;
        }

        private Constructor constructor;

        public SetAccessibleAction(Constructor constructor) {
            this.constructor = constructor;
        }
    }

    public MockFilter(String filter) throws InvalidSyntaxException {
        topLevel = true;
        (new Parser(filter)).parse(this);
    }

    public boolean match(ServiceReference reference) {
        throw new IllegalStateException("Operation not implemented");
    }

    public boolean match(Dictionary dictionary) {
        return match0(dictionary);
    }

    public boolean matchCase(Dictionary dictionary) {
        return match0(dictionary);
    }

    public String toString() {
        if (this.filter == null) {
            StringBuffer filter = new StringBuffer();
            filter.append('(');
            switch (operation) {
                default:
                    break;

                case 7: // '\007'
                {
                    filter.append('&');
                    MockFilter filters[] = (MockFilter[]) value;
                    int size = filters.length;
                    for (int i = 0; i < size; i++)
                        filter.append(filters[i].toString());

                    break;
                }

                case 8: // '\b'
                {
                    filter.append('|');
                    MockFilter filters[] = (MockFilter[]) value;
                    int size = filters.length;
                    for (int i = 0; i < size; i++)
                        filter.append(filters[i].toString());

                    break;
                }

                case 9: // '\t'
                {
                    filter.append('!');
                    filter.append(value.toString());
                    break;
                }

                case 6: // '\006'
                {
                    filter.append(attr);
                    filter.append('=');
                    String substrings[] = (String[]) value;
                    int size = substrings.length;
                    for (int i = 0; i < size; i++) {
                        String substr = substrings[i];
                        if (substr == null)
                            filter.append('*');
                        else
                            filter.append(encodeValue(substr));
                    }

                    break;
                }

                case 1: // '\001'
                {
                    filter.append(attr);
                    filter.append('=');
                    filter.append(encodeValue(value.toString()));
                    break;
                }

                case 3: // '\003'
                {
                    filter.append(attr);
                    filter.append(">=");
                    filter.append(encodeValue(value.toString()));
                    break;
                }

                case 4: // '\004'
                {
                    filter.append(attr);
                    filter.append("<=");
                    filter.append(encodeValue(value.toString()));
                    break;
                }

                case 2: // '\002'
                {
                    filter.append(attr);
                    filter.append("~=");
                    filter.append(encodeValue(approxString(value.toString())));
                    break;
                }

                case 5: // '\005'
                {
                    filter.append(attr);
                    filter.append("=*");
                    break;
                }
            }
            filter.append(')');
            if (topLevel)
                this.filter = filter.toString();
            else
                return filter.toString();
        }
        return this.filter;
    }

    public boolean equals(Object obj) {
        return obj == this || obj instanceof MockFilter && toString().equals(obj.toString());
    }

    public int hashCode() {
        return toString().hashCode();
    }

    protected MockFilter() {
        topLevel = false;
    }

    protected void setFilter(int operation, String attr, Object value) {
        this.operation = operation;
        this.attr = attr;
        this.value = value;
    }

    protected boolean match0(Dictionary properties) {
        switch (operation) {
            case 7: // '\007'
            {
                MockFilter filters[] = (MockFilter[]) value;
                int size = filters.length;
                for (int i = 0; i < size; i++)
                    if (!filters[i].match0(properties))
                        return false;

                return true;
            }

            case 8: // '\b'
            {
                MockFilter filters[] = (MockFilter[]) value;
                int size = filters.length;
                for (int i = 0; i < size; i++)
                    if (filters[i].match0(properties))
                        return true;

                return false;
            }

            case 9: // '\t'
            {
                MockFilter filter = (MockFilter) value;
                return !filter.match0(properties);
            }

            case 1: // '\001'
            case 2: // '\002'
            case 3: // '\003'
            case 4: // '\004'
            case 6: // '\006'
            {
                Object prop = properties != null ? properties.get(attr) : null;
                return compare(operation, prop, value);
            }

            case 5: // '\005'
            {
                Object prop = properties != null ? properties.get(attr) : null;
                return prop != null;
            }
        }
        return false;
    }

    protected static String encodeValue(String value) {
        boolean encoded = false;
        int inlen = value.length();
        int outlen = inlen << 1;
        char output[] = new char[outlen];
        value.getChars(0, inlen, output, inlen);
        int cursor = 0;
        for (int i = inlen; i < outlen;) {
            char c = output[i];
            switch (c) {
                case 40: // '('
                case 41: // ')'
                case 42: // '*'
                case 92: // '\\'
                    output[cursor] = '\\';
                    cursor++;
                    encoded = true;
                    // fall through

                default:
                    output[cursor] = c;
                    cursor++;
                    i++;
                    break;
            }
        }

        return encoded ? new String(output, 0, cursor) : value;
    }

    protected boolean compare(int operation, Object value1, Object value2) {
        if (value1 == null) {
            return false;
        }
        if (value1 instanceof String)
            return compare_String(operation, (String) value1, value2);
        Class clazz = value1.getClass();
        if (clazz.isArray()) {
            Class type = clazz.getComponentType();
            if (type.isPrimitive())
                return compare_PrimitiveArray(operation, type, value1, value2);
            else
                return compare_ObjectArray(operation, (Object[]) value1, value2);
        }
        if (value1 instanceof Vector)
            return compare_Vector(operation, (Vector) value1, value2);
        if (value1 instanceof Integer)
            return compare_Integer(operation, ((Integer) value1).intValue(),
                                   value2);
        if (value1 instanceof Long)
            return compare_Long(operation, ((Long) value1).longValue(), value2);
        if (value1 instanceof Byte)
            return compare_Byte(operation, ((Byte) value1).byteValue(), value2);
        if (value1 instanceof Short)
            return compare_Short(operation, ((Short) value1).shortValue(),
                                 value2);
        if (value1 instanceof Character)
            return compare_Character(operation,
                                     ((Character) value1).charValue(), value2);
        if (value1 instanceof Float)
            return compare_Float(operation, ((Float) value1).floatValue(),
                                 value2);
        if (value1 instanceof Double)
            return compare_Double(operation, ((Double) value1).doubleValue(),
                                  value2);
        if (value1 instanceof Boolean)
            return compare_Boolean(operation,
                                   ((Boolean) value1).booleanValue(), value2);
        if (value1 instanceof Comparable)
            return compare_Comparable(operation, (Comparable) value1, value2);
        else
            return compare_Unknown(operation, value1, value2);
    }

    protected boolean compare_Vector(int operation, Vector vector, Object value2) {
        int size = vector.size();
        for (int i = 0; i < size; i++)
            if (compare(operation, vector.elementAt(i), value2))
                return true;

        return false;
    }

    protected boolean compare_ObjectArray(int operation, Object array[],
                                          Object value2) {
        int size = array.length;
        for (int i = 0; i < size; i++)
            if (compare(operation, array[i], value2))
                return true;

        return false;
    }

    protected boolean compare_PrimitiveArray(int operation, Class type,
                                             Object primarray, Object value2) {
        if (Integer.TYPE.isAssignableFrom(type)) {
            int array[] = (int[]) primarray;
            int size = array.length;
            for (int i = 0; i < size; i++)
                if (compare_Integer(operation, array[i], value2))
                    return true;

            return false;
        }
        if (Long.TYPE.isAssignableFrom(type)) {
            long array[] = (long[]) primarray;
            int size = array.length;
            for (int i = 0; i < size; i++)
                if (compare_Long(operation, array[i], value2))
                    return true;

            return false;
        }
        if (Byte.TYPE.isAssignableFrom(type)) {
            byte array[] = (byte[]) primarray;
            int size = array.length;
            for (int i = 0; i < size; i++)
                if (compare_Byte(operation, array[i], value2))
                    return true;

            return false;
        }
        if (Short.TYPE.isAssignableFrom(type)) {
            short array[] = (short[]) primarray;
            int size = array.length;
            for (int i = 0; i < size; i++)
                if (compare_Short(operation, array[i], value2))
                    return true;

            return false;
        }
        if (Character.TYPE.isAssignableFrom(type)) {
            char array[] = (char[]) primarray;
            int size = array.length;
            for (int i = 0; i < size; i++)
                if (compare_Character(operation, array[i], value2))
                    return true;

            return false;
        }
        if (Float.TYPE.isAssignableFrom(type)) {
            float array[] = (float[]) primarray;
            int size = array.length;
            for (int i = 0; i < size; i++)
                if (compare_Float(operation, array[i], value2))
                    return true;

            return false;
        }
        if (Double.TYPE.isAssignableFrom(type)) {
            double array[] = (double[]) primarray;
            int size = array.length;
            for (int i = 0; i < size; i++)
                if (compare_Double(operation, array[i], value2))
                    return true;

            return false;
        }
        if (Boolean.TYPE.isAssignableFrom(type)) {
            boolean array[] = (boolean[]) primarray;
            int size = array.length;
            for (int i = 0; i < size; i++)
                if (compare_Boolean(operation, array[i], value2))
                    return true;

            return false;
        } else {
            return false;
        }
    }

    protected boolean compare_String(int operation, String string, Object value2) {
        switch (operation) {
            case 6: // '\006'
                String substrings[] = (String[]) value2;
                int pos = 0;
                int size = substrings.length;
                for (int i = 0; i < size; i++) {
                    String substr = substrings[i];
                    if (i + 1 < size) {
                        if (substr == null) {
                            String substr2 = substrings[i + 1];
                            if (substr2 != null) {
                                int index = string.indexOf(substr2, pos);
                                if (index == -1)
                                    return false;
                                pos = index + substr2.length();
                                if (i + 2 < size)
                                    i++;
                            }
                        } else {
                            int len = substr.length();
                            if (string.regionMatches(pos, substr, 0, len))
                                pos += len;
                            else
                                return false;
                        }
                    } else {
                        return substr == null || string.endsWith(substr);
                    }
                }

                return true;

            case 1: // '\001'
                return string.equals(value2);

            case 2: // '\002'
                string = approxString(string);
                String string2 = approxString((String) value2);
                return string.equalsIgnoreCase(string2);

            case 3: // '\003'
                return string.compareTo((String) value2) >= 0;

            case 4: // '\004'
                return string.compareTo((String) value2) <= 0;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Integer(int operation, int intval, Object value2) {
        int intval2 = Integer.parseInt(((String) value2).trim());
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return intval == intval2;

            case 2: // '\002'
                return intval == intval2;

            case 3: // '\003'
                return intval >= intval2;

            case 4: // '\004'
                return intval <= intval2;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Long(int operation, long longval, Object value2) {
        long longval2 = Long.parseLong(((String) value2).trim());
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return longval == longval2;

            case 2: // '\002'
                return longval == longval2;

            case 3: // '\003'
                return longval >= longval2;

            case 4: // '\004'
                return longval <= longval2;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Byte(int operation, byte byteval, Object value2) {
        byte byteval2 = Byte.parseByte(((String) value2).trim());
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return byteval == byteval2;

            case 2: // '\002'
                return byteval == byteval2;

            case 3: // '\003'
                return byteval >= byteval2;

            case 4: // '\004'
                return byteval <= byteval2;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Short(int operation, short shortval, Object value2) {
        short shortval2 = Short.parseShort(((String) value2).trim());
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return shortval == shortval2;

            case 2: // '\002'
                return shortval == shortval2;

            case 3: // '\003'
                return shortval >= shortval2;

            case 4: // '\004'
                return shortval <= shortval2;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Character(int operation, char charval,
                                        Object value2) {
        char charval2 = ((String) value2).trim().charAt(0);
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return charval == charval2;

            case 2: // '\002'
                return Character.toLowerCase(charval) == Character.toLowerCase(charval2);

            case 3: // '\003'
                return charval >= charval2;

            case 4: // '\004'
                return charval <= charval2;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Boolean(int operation, boolean boolval,
                                      Object value2) {
        boolean boolval2 = (Boolean.valueOf(((String) value2).trim())).booleanValue();
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return boolval == boolval2;

            case 2: // '\002'
                return boolval == boolval2;

            case 3: // '\003'
                return boolval == boolval2;

            case 4: // '\004'
                return boolval == boolval2;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Float(int operation, float floatval, Object value2) {
        float floatval2 = Float.parseFloat(((String) value2).trim());
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return floatval == floatval2;

            case 2: // '\002'
                return floatval == floatval2;

            case 3: // '\003'
                return floatval >= floatval2;

            case 4: // '\004'
                return floatval <= floatval2;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Double(int operation, double doubleval,
                                     Object value2) {
        double doubleval2 = Double.parseDouble(((String) value2).trim());
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return doubleval == doubleval2;

            case 2: // '\002'
                return doubleval == doubleval2;

            case 3: // '\003'
                return doubleval >= doubleval2;

            case 4: // '\004'
                return doubleval <= doubleval2;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Comparable(int operation, Comparable value1,
                                         Object value2) {
        Constructor constructor;
        try {
            constructor = value1.getClass().getConstructor(constructorType);
        } catch (NoSuchMethodException _ex) {
            return false;
        }
        try {
            if (!constructor.isAccessible())
                AccessController.doPrivileged(new SetAccessibleAction(
                                                                      constructor));
            value2 = constructor.newInstance(new Object[] { ((String) value2).trim() });
        } catch (IllegalAccessException _ex) {
            return false;
        } catch (InvocationTargetException _ex) {
            return false;
        } catch (InstantiationException _ex) {
            return false;
        }
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return value1.compareTo(value2) == 0;

            case 2: // '\002'
                return value1.compareTo(value2) == 0;

            case 3: // '\003'
                return value1.compareTo(value2) >= 0;

            case 4: // '\004'
                return value1.compareTo(value2) <= 0;

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected boolean compare_Unknown(int operation, Object value1,
                                      Object value2) {
        Constructor constructor;
        try {
            constructor = value1.getClass().getConstructor(constructorType);
        } catch (NoSuchMethodException _ex) {
            return false;
        }
        try {
            if (!constructor.isAccessible())
                AccessController.doPrivileged(new SetAccessibleAction(
                                                                      constructor));
            value2 = constructor.newInstance(new Object[] { ((String) value2).trim() });
        } catch (IllegalAccessException _ex) {
            return false;
        } catch (InvocationTargetException _ex) {
            return false;
        } catch (InstantiationException _ex) {
            return false;
        }
        switch (operation) {
            case 6: // '\006'
                return false;

            case 1: // '\001'
                return value1.equals(value2);

            case 2: // '\002'
                return value1.equals(value2);

            case 3: // '\003'
                return value1.equals(value2);

            case 4: // '\004'
                return value1.equals(value2);

            case 5: // '\005'
            default:
                return false;
        }
    }

    protected static String approxString(String input) {
        boolean changed = false;
        char output[] = input.toCharArray();
        int length = output.length;
        int cursor = 0;
        for (int i = 0; i < length; i++) {
            char c = output[i];
            if (Character.isWhitespace(c)) {
                changed = true;
            } else {
                output[cursor] = c;
                cursor++;
            }
        }

        return changed ? new String(output, 0, cursor) : input;
    }

    protected int operation;
    protected static final int EQUAL = 1;
    protected static final int APPROX = 2;
    protected static final int GREATER = 3;
    protected static final int LESS = 4;
    protected static final int PRESENT = 5;
    protected static final int SUBSTRING = 6;
    protected static final int AND = 7;
    protected static final int OR = 8;
    protected static final int NOT = 9;
    protected String attr;
    protected Object value;
    protected String filter;
    protected boolean topLevel;
    protected static final Class constructorType[];

    static {
        constructorType = (new Class[] { java.lang.String.class });
    }
}