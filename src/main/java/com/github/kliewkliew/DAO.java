package com.github.kliewkliew;

import java.sql.*;
import java.util.List;

public class DAO {

    private static final String tableName = "apartment_listings";
    private static final Connection conn;
    static {
        Connection temp = null;
        try {
            temp = DriverManager.getConnection("jdbc:sqlite:craigslist.db");
        } catch (Exception e) {
            temp = null;
        }
        conn = temp;
    }

    static void resetTable() throws Exception {
        final Statement stmt = conn.createStatement();
        stmt.executeUpdate("delete from " + tableName);
    }

    /**
     * Insert new listings into the database (does not update existing listings)
     * @param listings
     * @throws Exception
     */
    static void insertToDatabase(List<Listing> listings) throws Exception {
        if (listings.isEmpty()) return;

        final Statement stmt = conn.createStatement();
        for (final Listing l : listings) {
            try {
                final ResultSet rs = stmt.executeQuery("select count(1) from " + tableName + " where listing_id = " + l.id);
                if (rs.getInt(1) > 0) continue;
                stmt.executeUpdate("insert into " + tableName + " values " + l.sqlInsertValueString() + ";");
            } catch (Exception e) {
                // TODO: parameterized SQL statements to avoid exceptions by automatically escaping characters that result in invalid SQL query strings.
            }
        }
    }

    /**
     * Requires a primary key on the id column.
     * @param listings
     * @throws Exception
     */
    static void multiRowInsertToDatabase(List<Listing> listings) throws Exception {
        if (listings.isEmpty()) return;

        final Statement stmt = conn.createStatement();
        final StringBuilder sb = new StringBuilder("insert into " + tableName + " values ");
        for (final Listing l : listings) {
            sb.append(l.sqlInsertValueString());
            sb.append(", ");
        }
        sb.setLength(sb.length() - 2);

        stmt.executeUpdate(sb.toString());
    }

    static Listing[] getListings() throws Exception {
        final Statement stmt = conn.createStatement();
        final ResultSet nolRS = stmt.executeQuery("select count(1) from " + tableName);
        final int numberOfListings = nolRS.getInt(1);
        final ResultSet lsRS = stmt.executeQuery("select * from " + tableName);

        final Listing[] listings = new Listing[numberOfListings];
        for (int i = 0; i < numberOfListings && lsRS.next(); i++) {
            listings[i] = new Listing(
                    lsRS.getLong(1),
                    lsRS.getShort(2),
                    lsRS.getShort(3),
                    lsRS.getShort(4),
                    lsRS.getString(5)
            );
        }
        return listings;
    }
}
