package net.paulem.krimson.utils.nbt;

/**
 * Parser SNBT (Stringified NBT) intégré.
 * <p>
 * Remplace {@code net.minecraft.nbt.TagParser}, dont la signature change entre les
 * versions de Minecraft (1.21.11 a supprimé {@code parseTag(String)}), ce qui rendait
 * le chargement des modèles BDEngine dépendant de la version du serveur.
 * <p>
 * Supporte : compounds, listes, tableaux typés ({@code [B;] [I;] [L;]}), chaînes
 * quotées (simples ou doubles, avec échappements), chaînes non quotées, booléens et
 * nombres suffixés ({@code b s l f d} et leurs majuscules).
 */
public final class SnbtParser {
    private final String input;
    private int cursor;

    private SnbtParser(String input) {
        this.input = input;
    }

    /**
     * Parse un compound SNBT, par exemple {@code {id:"minecraft:block_display",Tags:["bde_0"]}}.
     *
     * @throws SnbtParseException si la chaîne est mal formée
     */
    public static SnbtCompound parse(String snbt) {
        if (snbt == null) throw new SnbtParseException("Chaîne SNBT nulle");
        SnbtParser parser = new SnbtParser(snbt);
        parser.skipWhitespace();
        SnbtCompound compound = parser.readCompound();
        parser.skipWhitespace();
        if (parser.canRead()) {
            throw parser.error("Données supplémentaires après le compound");
        }
        return compound;
    }

    /**
     * Comme {@link #parse(String)} mais retourne {@code null} au lieu de lever une exception.
     */
    public static SnbtCompound parseOrNull(String snbt) {
        try {
            return parse(snbt);
        } catch (SnbtParseException e) {
            return null;
        }
    }

    // --- LECTURE ---

    private SnbtTag readValue() {
        skipWhitespace();
        if (!canRead()) throw error("Valeur attendue");

        char c = peek();
        if (c == '{') return readCompound();
        if (c == '[') return readListOrArray();
        return readScalar();
    }

    private SnbtCompound readCompound() {
        expect('{');
        SnbtCompound compound = new SnbtCompound();
        skipWhitespace();

        while (canRead() && peek() != '}') {
            skipWhitespace();
            String key = readKey();
            skipWhitespace();
            expect(':');
            compound.put(key, readValue());
            skipWhitespace();

            if (canRead() && peek() == ',') {
                cursor++;
                skipWhitespace();
                continue;
            }
            break;
        }

        skipWhitespace();
        expect('}');
        return compound;
    }

    private SnbtTag readListOrArray() {
        expect('[');
        skipWhitespace();

        // Tableaux typés : [B;1,2], [I;...], [L;...]
        if (cursor + 1 < input.length() && input.charAt(cursor + 1) == ';') {
            char type = peek();
            if (type == 'B' || type == 'I' || type == 'L') {
                cursor += 2;
            }
        }

        SnbtList list = new SnbtList();
        skipWhitespace();

        while (canRead() && peek() != ']') {
            list.add(readValue());
            skipWhitespace();
            if (canRead() && peek() == ',') {
                cursor++;
                skipWhitespace();
                continue;
            }
            break;
        }

        skipWhitespace();
        expect(']');
        return list;
    }

    private String readKey() {
        if (!canRead()) throw error("Clé attendue");
        char c = peek();
        if (c == '"' || c == '\'') return readQuotedString();
        // Une clé non quotée s'arrête au ':' séparateur, contrairement à une valeur
        // qui peut contenir un namespace (ex: minecraft:stone).
        int start = cursor;
        while (canRead() && isUnquotedChar(peek()) && peek() != ':') cursor++;
        String key = input.substring(start, cursor);
        if (key.isEmpty()) throw error("Clé vide");
        return key;
    }

    private SnbtValue readScalar() {
        char c = peek();
        if (c == '"' || c == '\'') return SnbtValue.ofString(readQuotedString());

        String raw = readUnquoted();
        if (raw.isEmpty()) throw error("Valeur vide");
        if (raw.equals("true")) return SnbtValue.ofBoolean(true);
        if (raw.equals("false")) return SnbtValue.ofBoolean(false);

        SnbtValue number = parseNumber(raw);
        return number != null ? number : SnbtValue.ofString(raw);
    }

    private static SnbtValue parseNumber(String raw) {
        char suffix = raw.charAt(raw.length() - 1);
        String body = raw.length() > 1 ? raw.substring(0, raw.length() - 1) : raw;
        try {
            return switch (suffix) {
                case 'b', 'B' -> SnbtValue.ofNumber(Byte.parseByte(body));
                case 's', 'S' -> SnbtValue.ofNumber(Short.parseShort(body));
                case 'l', 'L' -> SnbtValue.ofNumber(Long.parseLong(body));
                case 'f', 'F' -> SnbtValue.ofNumber(Float.parseFloat(body));
                case 'd', 'D' -> SnbtValue.ofNumber(Double.parseDouble(body));
                default -> raw.indexOf('.') >= 0
                        ? SnbtValue.ofNumber(Double.parseDouble(raw))
                        : SnbtValue.ofNumber(Integer.parseInt(raw));
            };
        } catch (NumberFormatException e) {
            // Certaines valeurs dépassent int : on retente en long puis en double
            try {
                return SnbtValue.ofNumber(Long.parseLong(raw));
            } catch (NumberFormatException ignored) {
                try {
                    return SnbtValue.ofNumber(Double.parseDouble(raw));
                } catch (NumberFormatException ignoredToo) {
                    return null;
                }
            }
        }
    }

    private String readQuotedString() {
        char quote = input.charAt(cursor++);
        StringBuilder sb = new StringBuilder();
        while (canRead()) {
            char c = input.charAt(cursor++);
            if (c == '\\') {
                if (!canRead()) throw error("Échappement incomplet");
                char escaped = input.charAt(cursor++);
                switch (escaped) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (cursor + 4 > input.length()) throw error("Échappement unicode incomplet");
                        sb.append((char) Integer.parseInt(input.substring(cursor, cursor + 4), 16));
                        cursor += 4;
                    }
                    default -> sb.append(escaped);
                }
            } else if (c == quote) {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        throw error("Chaîne non terminée");
    }

    private String readUnquoted() {
        int start = cursor;
        while (canRead() && isUnquotedChar(peek())) cursor++;
        return input.substring(start, cursor);
    }

    private static boolean isUnquotedChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || c == '_' || c == '-' || c == '.' || c == '+' || c == ':' || c == '/';
    }

    // --- OUTILS ---

    private boolean canRead() {
        return cursor < input.length();
    }

    private char peek() {
        return input.charAt(cursor);
    }

    private void skipWhitespace() {
        while (canRead() && Character.isWhitespace(peek())) cursor++;
    }

    private void expect(char expected) {
        skipWhitespace();
        if (!canRead() || peek() != expected) {
            throw error("'" + expected + "' attendu");
        }
        cursor++;
    }

    private SnbtParseException error(String message) {
        return new SnbtParseException(message + " (position " + cursor + ")");
    }
}
