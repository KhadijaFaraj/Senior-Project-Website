package com.research.erp;

import weka.classifiers.Classifier;
import weka.core.*;
import java.io.InputStream;
import java.util.*;

import static spark.Spark.*;

public class App {

    static Classifier model;
    static Instances data;

    public static void main(String[] args) {

        port(Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")));

        staticFiles.location("/public");

        // =========================
        // LOAD MODEL
        // =========================
        try {

            InputStream stream = App.class.getResourceAsStream(
                    "/models/usage_gap.model");

            if (stream == null) {
                throw new RuntimeException(
                        "Model file not found in resources!");
            }

            model = (Classifier) SerializationHelper.read(stream);

            List<String> hl = List.of(" High ", " Low ");

            List<Attribute> attrs = new ArrayList<>(List.of(

                    new Attribute(
                            "USG_PERIOD",
                            List.of(
                                    " <6m ",
                                    " 6m_1y ",
                                    " >5y ",
                                    " 1_2y ",
                                    " 2_5y ")),

                    new Attribute(
                            "ERP_SYS",
                            List.of(
                                    " Oracle ",
                                    " Microsoft Dynamics ",
                                    " SAP ",
                                    " Odoo ")),

                    new Attribute(
                            "GAP",
                            List.of(
                                    " No_Gap ",
                                    " Gap "))));

            for (String f : List.of(
                    "TRAINING",
                    "COMPLEXITY",
                    "PERFORMANCE",
                    "MGT_SUPPORT",
                    "EXTERNAL_TOOLS",
                    "MONITOR")) {

                attrs.add(new Attribute(f, hl));
            }

            data = new Instances(
                    "UsageGapPrediction",
                    new ArrayList<>(attrs),
                    0);

            data.setClassIndex(2);

            System.out.println("Model loaded successfully.");

        } catch (Exception e) {

            e.printStackTrace();

            return;
        }

        // =========================
        // HOME
        // =========================
        get("/", (q, r) -> {

            r.redirect("/index.html");

            return null;
        });

        // =========================
        // PREDICTION
        // =========================
        post("/predict", (req, res) -> {

            try {

                Instance i = new DenseInstance(data.numAttributes());

                i.setDataset(data);

                // Usage period mapping
                String period;

                switch (req.queryParams("USG_PERIOD")) {

                    case "lt6m":
                        period = "<6m";
                        break;

                    case "gt5y":
                        period = ">5y";
                        break;

                    default:
                        period = req.queryParams("USG_PERIOD");
                        break;
                }

                set(i, "USG_PERIOD", period);

                set(i,
                        "ERP_SYS",
                        req.queryParams("ERP_SYS"));

                for (String f : List.of(
                        "TRAINING",
                        "COMPLEXITY",
                        "PERFORMANCE",
                        "MGT_SUPPORT",
                        "EXTERNAL_TOOLS",
                        "MONITOR")) {

                    set(i, f, req.queryParams(f));
                }

                String prediction = data.classAttribute()
                        .value(
                                (int) model.classifyInstance(i))
                        .trim();

                return resultPage(prediction);

            } catch (Exception e) {

                e.printStackTrace();

                res.type("text/html");

                String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";

                // Detect likely input-related issues
                if (msg.contains("null") || msg.contains("Attribute")) {

                    res.status(400);

                    return errorPage(
                            "Invalid Input Data",
                            "Some required fields are missing or invalid.");
                }

                // Default → system/model issue
                res.status(500);

                return errorPage(
                        "System Error",
                        "An unexpected error occurred during prediction.");
            }
        });

        // =========================
        // 404 ERROR
        // =========================
        notFound((req, res) -> {

            res.type("text/html");
            res.status(404);

            return errorPage(
                    "Page Not Found",
                    "The requested URL does not exist on this server.");
        });

        // =========================
        // INTERNAL ERROR
        // =========================
        internalServerError((req, res) -> {

            res.type("text/html");
            res.status(500);

            return errorPage(
                    "Server Error",
                    "Something went wrong on the server side.");
        });

        // =========================
        // EXCEPTION HANDLER
        // =========================
        exception(Exception.class, (e, req, res) -> {

            res.status(500);

            res.body(
                    errorPage(
                            "Application Error",
                            e.getMessage()));
        });
    }

    // =========================
    // SET ATTRIBUTE
    // =========================
    static void set(Instance i, String attr, String val) {

        i.setValue(
                data.attribute(attr),
                " " + val + " ");
    }

    // =========================
    // RESULT PAGE
    // =========================
    static String resultPage(String p) {

        String badgeClass = p.equals("Gap")
                ? "badge badge-gap"
                : "badge badge-no-gap";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "\n" +
                "<head>\n" +
                "\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "\n" +
                "    <title>Prediction Result</title>\n" +
                "\n" +
                "    <link rel=\"stylesheet\" href=\"/theme.css\">\n" +
                "\n" +
                "</head>\n" +
                "\n" +
                "<body class=\"result-page\">\n" +
                "\n" +
                "    <div class=\"result-card\">\n" +
                "\n" +
                "        <h1>Prediction Result</h1>\n" +
                "\n" +
                "        <p>\n" +
                "            The machine learning model has completed\n" +
                "            the ERP usage gap analysis.\n" +
                "        </p>\n" +
                "\n" +
                "        <div class=\"" + badgeClass + "\">\n" +
                "            " + p + "\n" +
                "        </div>\n" +
                "\n" +
                "        <br><br>\n" +
                "\n" +
                "        <a href=\"/index.html\" class=\"btn\">\n" +
                "            Back to Home\n" +
                "        </a>\n" +
                "\n" +
                "    </div>\n" +
                "\n" +
                "</body>\n" +
                "\n" +
                "</html>";
    }

    // =========================
    // ERROR PAGE
    // =========================
    static String errorPage(String title, String msg) {
        String template = "<!doctype html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\" />\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n    <title>%s</title>\n    <link rel=\"stylesheet\" href=\"/theme.css\"/>\n</head>\n\n<body>\n\n<header class=\"hero\" style=\"background: linear-gradient(135deg, #ef4444, #991b1b);\">\n    <h1>⚠ System Alert</h1>\n    <p>An error occurred while processing your request</p>\n</header>\n\n<main class=\"container\">\n\n    <div class=\"section\">\n\n        <div class=\"error-card\">\n\n            <div style=\"font-size:48px; margin-bottom:10px;\">⚠️</div>\n\n            <h1>%s</h1>\n\n            <p style=\"margin-top:10px;\">\n                We encountered a problem while processing your request.\n            </p>\n\n            <div class=\"details\" style=\"margin-top:20px;\">\n                %s\n            </div>\n\n            <div style=\"margin-top:25px;\">\n                <a href=\"/index.html\" class=\"btn\">Return to Home</a>\n            </div>\n\n        </div>\n\n    </div>\n\n</main>\n\n</body>\n</html>";
        return String.format(template, title, title, msg);
    }
}