package com.research.erp;

import weka.classifiers.Classifier;
import weka.core.*;
import java.util.*;
import static spark.Spark.*;

public class App {
    private static Classifier model;
    private static Instances datasetStructure;

    public static void main(String[] args) {
        port(8080);
        staticFiles.location("/public");

        // 1. Initialize the dataset structure and load the model
        try {
            // Load the model from the project root
            model = (Classifier) SerializationHelper.read("models/usage_gap.model");
            System.out.println("Model 'usage_gap.model' loaded successfully.");

            // Build the attribute list in the EXACT order of your uploaded model
            ArrayList<Attribute> attrs = new ArrayList<>();
            attrs.add(new Attribute("USG_PERIOD", Arrays.asList(" <6m ", " 6m_1y ", " >5y ", " 1_2y ", " 2_5y ")));
            attrs.add(new Attribute("ERP_SYS", Arrays.asList(" Oracle ", " Microsoft Dynamics ", " SAP ", " Odoo ")));
            attrs.add(new Attribute("GAP", Arrays.asList(" No_Gap ", " Gap ")));

            List<String> hl = Arrays.asList(" High ", " Low ");
            attrs.add(new Attribute("TRAINING", hl));
            attrs.add(new Attribute("COMPLEXITY", hl));
            attrs.add(new Attribute("PERFORMANCE", hl));
            attrs.add(new Attribute("MGT_SUPPORT", hl));
            attrs.add(new Attribute("EXTERNAL_TOOLS", hl));
            attrs.add(new Attribute("MONITOR", hl));

            datasetStructure = new Instances("UsageGapPrediction", attrs, 0);
            datasetStructure.setClassIndex(2); // GAP is at index 2

            System.out.println("Dataset structure initialized. Ready for predictions.");

        } catch (Exception e) {
            System.err.println("FATAL ERROR: Could not initialize Weka model or attributes!");
            System.err.println("Make sure 'usage_gap.model' is in the same folder as your pom.xml");
            e.printStackTrace();
            return; // Stop the server if initialization fails
        }

        // 2. The Prediction Route
        post("/predict", (req, res) -> {
            // Safety check: if datasetStructure is null, don't try to use it
            if (datasetStructure == null) {
                return "Error: Server failed to initialize the prediction model. Check terminal logs.";
            }

            try {
                Instance inst = new DenseInstance(9);
                inst.setDataset(datasetStructure);

                // Setting values with the required spaces
                inst.setValue(datasetStructure.attribute("USG_PERIOD"), " " + req.queryParams("USG_PERIOD") + " ");
                inst.setValue(datasetStructure.attribute("ERP_SYS"), " " + req.queryParams("ERP_SYS") + " ");
                inst.setValue(datasetStructure.attribute("TRAINING"), " " + req.queryParams("TRAINING") + " ");
                inst.setValue(datasetStructure.attribute("COMPLEXITY"), " " + req.queryParams("COMPLEXITY") + " ");
                inst.setValue(datasetStructure.attribute("PERFORMANCE"), " " + req.queryParams("PERFORMANCE") + " ");
                inst.setValue(datasetStructure.attribute("MGT_SUPPORT"), " " + req.queryParams("MGT_SUPPORT") + " ");
                inst.setValue(datasetStructure.attribute("EXTERNAL_TOOLS"),
                        " " + req.queryParams("EXTERNAL_TOOLS") + " ");
                inst.setValue(datasetStructure.attribute("MONITOR"), " " + req.queryParams("MONITOR") + " ");

                double result = model.classifyInstance(inst);
                String prediction = datasetStructure.classAttribute().value((int) result).trim();

                // Inside your App.java post route
                return "<h1>Result: " + prediction + "</h1><br><a href='/analyzer.html'>Go Back to Analyzer</a>";
            } catch (Exception e) {
                e.printStackTrace();
                return "Error during classification: " + e.getMessage();
            }
        });
    }
}