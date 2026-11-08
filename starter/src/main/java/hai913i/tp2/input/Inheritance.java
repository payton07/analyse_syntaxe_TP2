package hai913i.tp2.input;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Les liens d'héritage du projet analysé (bonus C2).
 *
 * <p>Le sujet ne compte que les appels de méthodes. Pour le bonus C2, on ajoute
 * une seconde relation : l'héritage d'une classe et l'implémentation d'une
 * interface. Ces faits ne sont pas dans les CSV : on les lit dans le bloc
 * facultatif {@code extra.inheritance} du fichier {@code <préfixe>.json}.</p>
 *
 * <p>On utilise Gson pour lire le JSON : contrairement aux CSV canoniques, un
 * fichier JSON a des chaînes échappées, des tableaux imbriqués et des espaces
 * libres, et un découpage à la main serait fragile.</p>
 */
public final class Inheritance {

    /**
     * Un lien d'héritage : {@code type} hérite de {@code parent} ou l'implémente.
     *
     * @param type       nom qualifié du type
     * @param parent     nom qualifié de sa superclasse ou de son interface
     * @param implementsInterface vrai pour une interface implémentée, faux pour une superclasse
     */
    public record Link(String type, String parent, boolean implementsInterface) {
    }

    private Inheritance() {
    }

    /**
     * @param dir    dossier des faits
     * @param prefix préfixe du jeu de données
     * @return le fichier JSON attendu, {@code <dir>/<prefix>.json}
     */
    public static Path jsonFile(Path dir, String prefix) {
        return dir.resolve(prefix + ".json");
    }

    /**
     * Lit les liens d'héritage du bloc {@code extra.inheritance}.
     *
     * <p>On garde tous les liens, y compris ceux vers des types hors du projet
     * ({@code java.lang.Enum}, {@code java.lang.Exception}) : c'est le calcul du
     * couplage qui les écarte, parce que seul lui connaît la liste des classes.</p>
     *
     * @param json le fichier {@code <préfixe>.json}
     * @return les liens, dans l'ordre du fichier
     * @throws IOException fichier absent, JSON invalide, ou bloc
     *                     {@code extra.inheritance} manquant ou mal formé
     */
    public static List<Link> load(Path json) throws IOException {
        if (!Files.isRegularFile(json)) {
            throw new IOException("fichier introuvable : " + json.toAbsolutePath()
                    + " (l'option --heritage lit le bloc extra.inheritance du JSON)");
        }
        String name = json.getFileName().toString();
        JsonElement root;
        try (Reader in = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(in);
        } catch (JsonParseException e) {
            throw new IOException(name + " : JSON invalide (" + e.getMessage() + ")");
        }
        try {
            JsonObject extra = root.getAsJsonObject().getAsJsonObject("extra");
            if (extra == null || !extra.has("inheritance")) {
                throw new IOException(name + " : pas de bloc extra.inheritance");
            }
            List<Link> out = new ArrayList<>();
            for (JsonElement e : extra.getAsJsonArray("inheritance")) {
                JsonObject o = e.getAsJsonObject();
                String type = o.get("type").getAsString();
                addAll(out, type, o.getAsJsonArray("superclasses"), false);
                addAll(out, type, o.getAsJsonArray("interfaces"), true);
            }
            return out;
        } catch (IllegalStateException | ClassCastException | NullPointerException
                 | UnsupportedOperationException e) {
            // on tombe ici si un champ manque ou n'a pas le bon type
            throw new IOException(name + " : bloc extra.inheritance mal formé");
        }
    }

    private static void addAll(List<Link> out, String type, JsonArray parents, boolean iface) {
        if (parents == null) {
            return;                                  // champ absent : aucun parent de ce genre
        }
        for (JsonElement p : parents) {
            out.add(new Link(type, p.getAsString(), iface));
        }
    }
}
