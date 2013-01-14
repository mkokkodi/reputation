/**
 * Author Marios Kokkodis
 * Last update 01/17/2012
 * -------------------------------------------------------
 * Categories ategories:
 * 
 * Technical Level: (m=3+1), '1 - web-development', '2 - Software-development', '3 - design and multimedia'
 * Non - Technical Level: (m=3+1), '1 - writing', '2 - administrative', '3 - Sales & marketing'
 * Generic: (m=2+1), '1 - technical' , '2 - non tech'
 * 
 * --------------------------------------------------------
 *  

 * 
 * Binomial model with history and score (helpfulness) thresholds. 
 * Point Estimate (PE) and  Random Sampling  (RS) aproaches.
 * 
 * Training stream. Outputs tuples ready for regressions
 * Output dir: ~/outFiles/training/$scoreTh/$model_$approach_$level 
 * 
 */

package kokkodis.odesk;

import java.util.HashMap;

import kokkodis.factory.PropertiesFactory;
import kokkodis.utils.AverageResults;
import kokkodis.utils.CreateTrainTest;
import kokkodis.utils.Evaluate;
import kokkodis.utils.GlobalVariables;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.RunRegressions;
import kokkodis.utils.Utils;

public class Reputation {

	private static GlobalVariables globalVars;
	private static boolean evaluate = false;
	private static boolean train = false;
	private static boolean create = false;
	public static boolean crossValidation = false;
	private static boolean splitFiles = false;
	private static boolean showAveragesCV = true;
	public static HashMap<Integer, Integer> developerToSet = null;

	public static void main(String[] args) {

		globalVars = GlobalVariables.getInstance();

		if (args[0].contains("-h"))
			Utils.printHelp();
		else {
			for (int i = 0; i < args.length; i++) {
				if (args[i].contains("-t"))
					train = true;
				if (args[i].contains("-g"))
					create = true;
				if (args[i].contains("-e"))
					evaluate = true;
				if (args[i].contains("-w"))
					GlobalVariables.printFiles = true;
				if (args[i].contains("-cv"))
					crossValidation = true;
				if (args[i].contains("-f")) {
					GlobalVariables.folds = Integer
							.parseInt(args[i + 1].trim());
					i++;
				}
				if (args[i].contains("-s"))
					splitFiles = true;
				if (args[i].contains("-a"))
					showAveragesCV = true;
				if (args[i].contains("-n"))
					GlobalVariables.evaluateOnTrain = true;
				if (args[i].contains("-p"))
					GlobalVariables.outputPredictions = true;
				if (args[i].contains("-yc")) {
					GlobalVariables.syntheticCluster = true;
				}

				else if (args[i].contains("-y"))
					GlobalVariables.synthetic = true;

			}

			if (!create && !train && !evaluate && !crossValidation) {
				System.out.println("Wrong option. Please run --help.");
			} else {
				if (crossValidation)
					crossValidate();
				else {
					if (create)
						generateSets();
					if (train)
						train();
					if (evaluate) {
						if (GlobalVariables.evaluateOnTrain)
							System.out.println("Running evaluation on Train.");
						initEvalFiles();
						// initPredictionsFile();
						runEval();
						closeEvalFiles();
					}
				}

				System.out.println("Completed.");
			}
		}
	}

	private static void crossValidate() {

		if (splitFiles) {
			if (GlobalVariables.folds == -1) {
				System.out
						.println("You have to provide the number of folds in order to split the files.\n"
								+ "Run --help for more information.");

			} else {
				System.out
						.println("Spliting....Folds:" + GlobalVariables.folds);
				System.out.println(GlobalVariables.line);
				for (int i = 1; i <= GlobalVariables.folds; i++) {
					System.out.println("Creating fold " + i);
					CreateTrainTest.createDeveloperSets(i);
				}
			}
		}
		if (evaluate) {
			initEvalFiles();
			showAveragesCV = true;
		}
		for (int i = 1; i <= GlobalVariables.folds; i++) {
			GlobalVariables.currentFold = i;
			if (create)
				generateSets();
			if (train)
				train();
			if (evaluate) {
				System.out
						.println("Evaluation starts..."
								+ ((GlobalVariables.currentFold != null) ? "Current Fold:"
										+ GlobalVariables.currentFold
										: ""));

				runEval();

			}

		}
		if (evaluate)
			closeEvalFiles();
		if (showAveragesCV)
			AverageResults.averageAndPrint();

	}

	private static void initPredictionsFile() {

		if (GlobalVariables.printFiles && GlobalVariables.outputPredictions) {
			String resultPath = PropertiesFactory.getInstance().getProps()
					.getProperty("results");

			GlobalVariables.predictions = new PrintToFile();
			String predictionFile = resultPath
					+ "predictions"
					+ ((GlobalVariables.currentFold != null) ? "_cv"
							+ GlobalVariables.currentFold : "")
					+ ((GlobalVariables.curModel.equals("Binomial")) ? ("_" + GlobalVariables.currentBinomialThreshold)
							: "") + ".csv";
			System.out.println("Prediction file:" + predictionFile);
			GlobalVariables.predictions.openFile(predictionFile);

			GlobalVariables.predictions
					.writeToFile("model,approach,ScoreThreshold,"
							+ "HistoryThreshold,actual,prediction,baseline,average,r,rl,rr,EMPrediction");
		}

	}

	private static void initEvalFiles() {
		if (GlobalVariables.printFiles) {
			String resultPath = PropertiesFactory.getInstance().getProps()
					.getProperty("results");
			GlobalVariables.allResultsFile = new PrintToFile();
			GlobalVariables.allResultsFile.openFile(resultPath
					+ "results"
					+ (crossValidation ? "_cv" : "")+(GlobalVariables.syntheticCluster?"_syn_cluster":"")
					+ ((GlobalVariables.synthetic) ? ("_syn" + (globalVars
							.getClusterCategories().get("r").length - 1)) : "")
					+ ".csv");

			GlobalVariables.allResultsFile
					.writeToFile(((GlobalVariables.synthetic) ? "categories,"
							: "")
							+ "model,exactModel,approach,ScoreThreshold,HistoryThreshold,MAEModel"
							+ ",MAEBaseline");

			globalVars.openFile(resultPath + "coeffs"
					+ (crossValidation ? "_cv" : "") + ".csv");

		}

	}

	private static void closeEvalFiles() {

		if (GlobalVariables.printFiles) {

			GlobalVariables.allResultsFile.closeFile();
			globalVars.getOutputFile().closeFile();
			if (GlobalVariables.outputPredictions)
				GlobalVariables.predictions.closeFile();
		}
	}

	private static void runEval() {

		for (String model : globalVars.getModels()) {

			GlobalVariables.curModel = model;

			if (model.equals("Binomial")) {
				for (float currentBinomialThreshold : globalVars
						.getScoreThresholds()) {
					GlobalVariables.currentBinomialThreshold = currentBinomialThreshold;
					initPredictionsFile();
					for (String approach : globalVars.getApproaches()) {
						GlobalVariables.curApproach = approach;

						runEvalCluster();
					}
				}
			} else {
				initPredictionsFile();
				for (String approach : globalVars.getApproaches()) {
					GlobalVariables.curApproach = approach;
					
					runEvalCluster();
				}

			}

		}

	}

	private static void runEvalCluster() {
		for (String cluster : globalVars.getHierarchyStracture()) {
			GlobalVariables.curCluster = cluster;
			GlobalVariables.getCurCoeffs().put(cluster,
					RunRegressions.readCoeffs());
		}

		Reputation.print("Running evaluation for model:"
				+ GlobalVariables.curModel + ", approach:"
				+ GlobalVariables.curApproach + " cor cluster:"
				+ GlobalVariables.curCluster);

		Evaluate.evaluate();

	}

	private static void generateSets() {
		System.out.println("Generating Sets...");

		for (String model : globalVars.getModels()) {
			for (String approach : globalVars.getApproaches()) {
				GlobalVariables.curModel = model;
				GlobalVariables.curApproach = approach;
				if (model.equals("Binomial")) {
					for (float currentBinomialThreshold : globalVars
							.getScoreThresholds()) {
						GlobalVariables.currentBinomialThreshold = currentBinomialThreshold;

						runModel();
					}
				} else
					runModel();
			}

		}
	}

	private static void train() {
		System.out.println("Training...");
		for (String model : globalVars.getModels()) {

			for (String approach : globalVars.getApproaches()) {

				GlobalVariables.curModel = model;
				GlobalVariables.curApproach = approach;
				if (model.equals("Binomial")) {
					for (float currentBinomialThreshold : globalVars
							.getScoreThresholds()) {
						GlobalVariables.currentBinomialThreshold = currentBinomialThreshold;
						runRegressions();
					}
				} else {
					runRegressions();

				}
			}
		}

	}

	private static void runRegressions() {
		for (String cluster : globalVars.getHierarchyStracture()) {
			GlobalVariables.curCluster = cluster;
			RunRegressions.createRegressionFiles();
			RunRegressions.getCoeffs(true);
		}

	}

	private static void runModel() {

		for (String cluster : globalVars.getHierarchyStracture()) {
			GlobalVariables.curCluster = cluster;
			CreateTrainTest.generateTrainTestSets();
		}

	}

	public static void print(String str) {
		System.out.println(str);
	}
}
