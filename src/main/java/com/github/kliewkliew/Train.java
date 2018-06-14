package com.github.kliewkliew;

import akka.japi.Pair;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import scala.Tuple4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;

public class Train {

    private static final int basecols = 2;

    public static void main(String[] args) throws Exception {
        final Listing[] listings = DAO.getListings();
        final HashMap<String,Integer> enumNbh = encodeNeighborhoods(listings);
        final Pair<List<Listing>,List<Listing>> trainTest = split(listings);

        final Tuple4<long[],int[],float[],float[]> train = convertListingToCSR(trainTest.first(), enumNbh);
        final Tuple4<long[],int[],float[],float[]> test = convertListingToCSR(trainTest.second(), enumNbh);

        final DMatrix trainDm = new DMatrix(
                train._1(), train._2(), train._3(), DMatrix.SparseType.CSR);
        final DMatrix testDm = new DMatrix(
                test._1(), test._2(), test._3(), DMatrix.SparseType.CSR);
        trainDm.setLabel(train._4());
        testDm.setLabel(test._4());

        final Map<String,Object> params = new HashMap<>();
        //params.put("eta", 1.0);
        //params.put("max_depth", 10);
        params.put("objective", "reg:linear");
        params.put("booster", "gblinear");
        params.put("nthread", 8);
        final int round = 2;
        final Map<String,DMatrix> watch = new HashMap<>(); {
            watch.put("train", trainDm);
            watch.put("valid", testDm);
        }
        final Booster booster = XGBoost.train(trainDm, params, round, watch, null, null, null, 2);
        booster.saveModel("model.bin");

        final FileOutputStream f = new FileOutputStream(new File("location-encoding.txt"));
        final ObjectOutputStream o = new ObjectOutputStream(f);
        o.writeObject(enumNbh);
    }

    /**
     * Split listings into training set and testing set.
     * @param listings
     * @return
     */
    private static Pair<List<Listing>,List<Listing>> split(Listing[] listings) {
        final Random rng = new Random();
        final List<Listing> train = new ArrayList<>();
        final List<Listing> valid = new ArrayList<>();

        for (final Listing l : listings) {
            if (rng.nextInt(10) < 7)
                train.add(l);
            else
                valid.add(l);
        }
        return new Pair<>(train, valid);
    }

    /**
     * Create one-hot encoding map for neighborhoods.
     * @param listings
     * @return
     */
    private static HashMap<String,Integer> encodeNeighborhoods(Listing[] listings) {
        final Set<String> hood = new HashSet<>();
        for (final Listing l : listings)
            hood.add(normalize(l.neighborhood));

        final HashMap<String,Integer> enumH = new HashMap<>();
        int i = basecols;
        for (final String h : hood)
            enumH.put(h, i++);
        return enumH;
    }

    private static String normalize(String nbh) {
        return nbh;
    }

    /**
     * Convert listings to Compressed Space Row (CSR) format sparse matrix.
     * @param listings
     * @param neighborhood One-hot encoding for neighborhoods.
     * @return Triplet of row index, column index, data, and label arrays.
     */
    private static Tuple4<long[],int[],float[],float[]> convertListingToCSR(List<Listing> listings, Map<String,Integer> neighborhood) {
        final float[] converted = new float[listings.size() * 3];
        final long[] rowIndex = new long[listings.size() + 1];
        final int[] colIndex = new int[converted.length];
        final float[] label = new float[listings.size()];

        int i = 0;
        int ri = 1;
        int li = 0;
        for (final Listing l : listings) {
            colIndex[i] = 0;
            converted[i++] = l.bedrooms;
            colIndex[i] = 1;
            converted[i++] = l.sqft;
            colIndex[i] = neighborhood.get(normalize(l.neighborhood));
            converted[i++] = 1;
            rowIndex[ri] = i + 1;
            ri++;
            label[li++] = l.price;
        }
        return new Tuple4<>(rowIndex, colIndex, converted, label);
    }

}
