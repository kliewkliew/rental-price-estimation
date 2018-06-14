package com.github.kliewkliew;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;

public class Estimate {
    public static void main(String[] args) throws Exception {
        final CommandLine cli = getCommandLineArgs(args);
        final Booster booster = XGBoost.loadModel("model.bin");

        final FileInputStream fi = new FileInputStream(new File("location-encoding.txt"));
        final ObjectInputStream oi = new ObjectInputStream(fi);
        final HashMap<String,Integer> enumNbh = (HashMap<String,Integer>)oi.readObject();

        int features = 0;
        if (cli.hasOption("bedrooms")) {
            features++;
        }
        if (cli.hasOption("square_feet")) {
            features++;
        }
        if (cli.hasOption("neighborhood")) {
            features++;
        }

        final float[] data = new float[features];
        final long[] rowIndex = new long[2];
        final int[] colIndex = new int[features];
        int i = 0;
        if (cli.hasOption("bedrooms")) {
            data[i] = Float.parseFloat(cli.getOptionValue("bedrooms"));
            colIndex[i++] = 0;
        }
        if (cli.hasOption("square_feet")) {
            data[i] = Float.parseFloat(cli.getOptionValue("square_feet"));
            colIndex[i++] = 1;
        }
        if (cli.hasOption("neighborhood") && enumNbh.containsKey(cli.getOptionValue("neighborhood"))) {
            data[i] = 1;
            colIndex[i++] = enumNbh.get(cli.getOptionValue("neighborhood"));
        }
        rowIndex[1] = i;

        final DMatrix trainDm = new DMatrix(
                rowIndex, colIndex, data, DMatrix.SparseType.CSR);
        final float[][] predict = booster.predict(trainDm);
        System.out.println(predict[0][0]);
    }

    private static CommandLine getCommandLineArgs(String[] args) throws Exception {
        final CommandLineParser parser = new DefaultParser();
        final Options options = new Options();
        options.addOption("b", "bedrooms", true, "");
        options.addOption("s", "square_feet", true, "");
        options.addOption("n", "neighborhood", true, "");
        return parser.parse(options, args);
    }
}
