package com.github.kliewkliew;

public class Listing {
    final long id;
    final Short price;
    final Short bedrooms;
    final Short sqft;
    final String neighborhood;

    Listing(long i, Short p, Short b, Short s, String n) {
        id = i;
        price = p;
        bedrooms = b;
        sqft = s;
        neighborhood = n == null ? null : n.toLowerCase();
    }

    String sqlInsertValueString() {
        return String.format("(%d, %s, %s, %s, '%s')",
                id, nullOrValue(price), nullOrValue(bedrooms), nullOrValue(sqft), neighborhood);
    }

    private static <T> String nullOrValue(T t) {
        return t == null ? "NULL" : t.toString();
    }

}
