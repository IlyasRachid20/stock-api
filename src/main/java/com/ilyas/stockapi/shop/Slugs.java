package com.ilyas.stockapi.shop;

import java.text.Normalizer;
import java.util.Locale;

/** Readable names for web addresses: "Chargers & cables" -> "chargers-cables", "Écouteurs" -> "ecouteurs". */
public final class Slugs {

    private Slugs() {
    }

    public static String of(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
    }
}
