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
                return predictionPage(prediction);
            } catch (Exception e) {
                e.printStackTrace();
                return "Error during classification: " + e.getMessage();
            }
        });
    }

    public static String predictionPage(String prediction) {
        String color = prediction.equals("Gap") ? "#e74c3c" : "#2ecc71";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Prediction Result</title>" +
                "<style>" +
                "body {" +
                "    margin: 0;" +
                "    font-family: 'Arial', sans-serif;" +
                "    background: linear-gradient(135deg, #74ebd5, #acb6e5);" +
                "    height: 100vh;" +
                "    display: flex;" +
                "    justify-content: center;" +
                "    align-items: center;" +
                "}" +
                ".card {" +
                "    background: white;" +
                "    padding: 40px 30px;" +
                "    border-radius: 16px;" +
                "    box-shadow: 0 10px 30px rgba(0,0,0,0.15);" +
                "    text-align: center;" +
                "    width: 320px;" +
                "}" +
                "h1 {" +
                "    margin: 0 0 20px 0;" +
                "    font-size: 26px;" +
                "}" +
                ".btn {" +
                "    display: inline-block;" +
                "    margin-top: 20px;" +
                "    padding: 12px 24px;" +
                "    background: #007BFF;" +
                "    color: white;" +
                "    text-decoration: none;" +
                "    border-radius: 8px;" +
                "    transition: 0.3s ease;" +
                "}" +
                ".btn:hover {" +
                "    background: #0056b3;" +
                "    transform: translateY(-2px);" +
                "}" +
                ".badge {" +
                "    display: inline-block;" +
                "    padding: 6px 12px;" +
                "    border-radius: 20px;" +
                "    background: " + color + ";" +
                "    color: white;" +
                "    font-weight: bold;" +
                "    margin-top: 10px;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='card'>" +
                "<h1>Prediction Result</h1>" +
                "<div class='badge'>" + prediction + "</div>" +
                "<br>" +
                "<a class='btn' href='/index.html'>Back to Form</a>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
