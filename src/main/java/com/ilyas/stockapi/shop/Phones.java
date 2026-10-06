package com.ilyas.stockapi.shop;

/**
 * Phone numbers in one form, so "06 12 34 56 78", "0612345678" and "+212 6 12 34 56 78" are the
 * same customer: separators are dropped, "00" becomes "+", and a Moroccan local number (0 and
 * 9 digits) gets the +212 country code.
 */
public final class Phones {

    private Phones() {
    }

    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[\\s().\\-]", "");
        if (digits.startsWith("00")) {
            digits = "+" + digits.substring(2);
        }
        if (digits.matches("0\\d{9}")) {
            digits = "+212" + digits.substring(1);
        }
        return digits;
    }
}
