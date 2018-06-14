package com.github.kliewkliew;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scrape {
    private static int minPrice = 500, maxPrice = 15000;
    private static final String default_url = "https://sfbay.craigslist.org/search/apa";
    private static final int resultsPerPage = 120, maxResults = 3000;

    public static void main(String[] args) throws Exception {
        final CommandLine cli = getCommandLineArgs(args);
        if (cli.hasOption("r")) DAO.resetTable();
        final String url = cli.hasOption("u") ? cli.getOptionValue("u") : default_url;
        for (int position = 0; position < maxResults; position += resultsPerPage) processResults(url, position);
    }

    private static CommandLine getCommandLineArgs(String[] args) throws Exception {
        final CommandLineParser parser = new DefaultParser();
        final Options options = new Options();
        options.addOption("r", "reset_db", false, "");
        options.addOption("u", "url", false, "");
        return parser.parse(options, args);
    }

    /**
     * Insert all listings on page into database
     * @param url
     * @param position
     */
    private static void processResults(String url, int position) throws Exception {
        final Document doc = Jsoup.connect(url + "?s=" + position).get();
        final Elements rows = doc.getElementById("sortable-results").getElementsByClass("rows").get(0).children();

        final List<Listing> listings = new ArrayList<>();
        for (final Element row : rows) {
            try {
                listings.add(processListing(row));
            } catch (Exception e) {
                // Incomplete data, do not use this listing.
                System.out.println(e.getMessage());
            }
        }

        DAO.insertToDatabase(listings);
        System.out.println("Processed " + listings.size() + " listings.");
    }

    /**
     * Return Listing for row.
     */
    private static final Pattern brPattern = Pattern.compile("(\\d+)br.*");
    private static final Pattern sqftPattern = Pattern.compile(".*?(\\d+)ft.*");
    private static Listing processListing(Element row) throws Exception {
        //final String listingUrl = row.getElementsByAttribute("href").attr("href");
        //final Document listDoc = Jsoup.connect(listingUrl).get();
        final long id = Long.parseLong(row.attr("data-pid"));
        Short pr = null;
        Short br = null;
        Short sqft = null;
        String nbhd = null;

        for (final Element elem : row.getElementsByClass("result-info").get(0).getElementsByClass("result-meta").get(0).children()) {
            switch (elem.className()) {
                case "result-price":
                    pr = Short.parseShort(elem.text().substring(1));
                    if (pr < minPrice || pr > maxPrice) throw new Exception("Invalid price: " + pr);
                    break;
                case "housing": {
                    final Matcher brMatcher = brPattern.matcher(elem.text());
                    if (brMatcher.matches())
                        br = Short.parseShort(brMatcher.toMatchResult().group(1));
                    final Matcher sqftMatcher = sqftPattern.matcher(elem.text());
                    if (sqftMatcher.matches())
                        sqft = Short.parseShort(sqftMatcher.toMatchResult().group(1));
                    break;
                }
                case "result-hood":
                    if (elem.text().length() > 0) nbhd = elem.text().substring(1, elem.text().length() - 1);
            }
        }

        return new Listing(id, pr, br, sqft, nbhd);
    }

}
