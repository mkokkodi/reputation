package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import kokkodis.factory.PropertiesFactory;

public class EMComputeLambdas {

	/**
	 * @param args
	 */
	/*
	 * A succesful prediction is one with less than 0.01 error.
	 */
	private static final double initialEpsilonForSuccess = 0.05;
	private static double epsilonForSuccess;
	private static HashMap<String, ArrayList<HashMap<String, Boolean>>> data;
	private static HashMap<String, HashMap<String, Double>> lambdas;
	private static double numberOfLevels;
	private static HashMap<String, HashMap<String, Double>> alphas;
	private static HashMap<String, Boolean> convergedModels;
	private static final double convergenceCritirion = 0.001;
	private static boolean allConverged = false;
	private static final int maxIterations = 500;
	private static int categories = -1;
	private static boolean hierarchyFlag;
	private static int folds;
	private static boolean crossValidation = false;
	private static PrintToFile outputFile;
	private static boolean real = false;

	public static void main(String[] args) {

		String[] hier = PropertiesFactory.getInstance().getProps()
				.getProperty("hierarchyStructure").split(",");
		if (hier.length == 3) {
			System.out.println("We are dealing with hierarchies.");
			numberOfLevels = 3;
			hierarchyFlag = true;
		} else {
			numberOfLevels = 2;
			hierarchyFlag = false;
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i].contains("-c"))

				categories = Integer.parseInt(args[i + 1].trim());

			if (args[i].contains("-f")) {
				crossValidation = true;
				folds = Integer.parseInt(args[i + 1].trim());
				System.out.println("Cross validation: folds:" + folds);
			}
			if (args[i].contains("-r")) {
				real = true;
			}
		}
		System.out.println("Categories:" + categories);

		String[] scoreThresholds = PropertiesFactory.getInstance().getProps()
				.getProperty("scoreThresholds").split(",");
		if (crossValidation) {
			for (int i = 2; i <= folds; i++) {
				initFile(i);

				// For binomial.
				for (String threshold : scoreThresholds) {
					allConverged = false;
					runEM(i, threshold);
				}
				// for multinomial
				allConverged = false;
				runEM(i, null);
				outputFile.closeFile();
			}
		} else if (real) {
			initFile(null);
			for (String threshold : scoreThresholds) {
				allConverged = false;
				runEM(null, threshold);
			}
			allConverged = false;
			runEM(null, null); //for multinomial.
			outputFile.closeFile();
		} else {
			allConverged = false;
			initFile(null);
			runEM(null, null);
			outputFile.closeFile();
		}

		System.out.println("Completed.");

	}

	private static void initFile(Integer currentFold) {
		outputFile = new PrintToFile();
		String outFile = PropertiesFactory.getInstance().getProps()
				.getProperty("results");
		String f = outFile + "lambdas"
				+ ((categories != -1) ? "" + categories : "")
				+ (currentFold != null ? ("_cv" + currentFold) : "") + ".csv";
		System.out.println("Output File:" + f);
		outputFile.openFile(f);

		outputFile
				.writeToFile("model,approach,ScoreThreshold,  cluster, averageLambda, rLambda, clusterLambda");

	}

	private static void runEM(Integer currentFold, String threshold) {

		loadData(currentFold, threshold);
		initializeLambdas();
		System.out.println("Data loaded. Current Fold:" + currentFold
				+ " currentThreshold:" + threshold);
		int iteration = 0;
		while (!allConverged && iteration < maxIterations) {
			allConverged = true;
			iteration++;
			System.out.println("Iteration:" + iteration);
			for (Entry<String, ArrayList<HashMap<String, Boolean>>> e : data
					.entrySet()) {
				String key = e.getKey();

				if (!convergedModels.get(key)) {
					allConverged = false;
					String curCluster = getCurClusterFromKey(key);
					HashMap<String, Double> currentLambdas = lambdas.get(key);
					// System.out.println("Estimating Lambdas for "+key);
					HashMap<String, Double> currentAplhas = alphas.get(key);
					HashMap<String, Double> probSuccessGivenM = new HashMap<String, Double>();
					probSuccessGivenM.put("average",
							estimateProbSuccessgivenM("average", e.getValue()));
					probSuccessGivenM.put("r",
							estimateProbSuccessgivenM("r", e.getValue()));
					if (hierarchyFlag) { // i.e. if we have hierarchical
											// model.
						if (!curCluster.equals("r")) {
							probSuccessGivenM.put(
									curCluster,
									estimateProbSuccessgivenM(curCluster,
											e.getValue()));
						}
					}
					double denom = estimateAlphaDenom(probSuccessGivenM,
							currentLambdas);
					currentAplhas.put(
							"average",
							estimateAlpha(probSuccessGivenM, currentLambdas,
									"average", denom));
					currentAplhas.put(
							"r",
							estimateAlpha(probSuccessGivenM, currentLambdas,
									"r", denom));
					if (hierarchyFlag) {
						if (!curCluster.equals("r"))
							currentAplhas.put(
									curCluster,
									estimateAlpha(probSuccessGivenM,
											currentLambdas, curCluster, denom));
					}
					estimateNewLambdas(key, currentAplhas, currentLambdas);
				}
			}
		}
		for (Entry<String, HashMap<String, Double>> eOut : lambdas.entrySet()) {
			System.out
					.println("-----------------------------------------------");
			String key = eOut.getKey();
			System.out.println(key);
			System.out.println("Converged:" + convergedModels.get(key));
			Double aveLambda = eOut.getValue().get("average");
			Double rLambda = eOut.getValue().get("r");
			if (hierarchyFlag) {
				String curCluster = getCurClusterFromKey(key);
				Double clusterLambda = eOut.getValue().get(curCluster);

				System.out.println(curCluster);

				System.out.println(" avg : " + aveLambda + " rLambda:"
						+ rLambda + " cluster:" + clusterLambda);
				String[] tmpAr = key.split("_");

				outputFile.writeToFile(tmpAr[0] + "," + tmpAr[1] + ","
						+ tmpAr[2] + "," + curCluster + "," + aveLambda + ","
						+ rLambda + "," + clusterLambda);
			} else {

				System.out.println(" avg : " + aveLambda + " rLambda:"
						+ rLambda);
				String[] tmpAr = key.split("_");

				outputFile.writeToFile(tmpAr[0] + "," + tmpAr[1] + ","
						+ tmpAr[2] + "," + "r" + "," + aveLambda + ","
						+ rLambda);
			}
		}

	}

	private static Double estimateAlpha(
			HashMap<String, Double> probSuccessGivenM,
			HashMap<String, Double> currentLambdas, String curCluster,
			double denom) {

		double nom = currentLambdas.get(curCluster)
				* probSuccessGivenM.get(curCluster);
		return nom / denom;
	}

	private static double estimateAlphaDenom(
			HashMap<String, Double> probSuccessGivenM,
			HashMap<String, Double> currentLambdas) {
		double denom = 0;
		for (Entry<String, Double> e : probSuccessGivenM.entrySet()) {
			denom += (currentLambdas.get(e.getKey()) * e.getValue());
		}

		return denom;
	}

	private static void estimateNewLambdas(String key,
			HashMap<String, Double> currentAplhas,
			HashMap<String, Double> currentLambdas) {
		double denom = 0;

		HashMap<String, Double> newCurLambdas = new HashMap<String, Double>();

		for (Entry<String, Double> e : currentAplhas.entrySet()) {
			denom += e.getValue();
		}

		for (Entry<String, Double> e : currentAplhas.entrySet()) {
			newCurLambdas.put(e.getKey(), e.getValue() / denom);
		}
		updateConvergence(key, newCurLambdas, currentLambdas);

	}

	private static void updateConvergence(String key,
			HashMap<String, Double> newCurLambdas,
			HashMap<String, Double> currentLambdas) {
		double diff = 0;
		for (Entry<String, Double> e : currentLambdas.entrySet()) {

			double newLambda = newCurLambdas.get(e.getKey());
			diff += Math.abs(e.getValue() - newLambda);
		}
		// System.out.println("Key:"+key+" Difference:" + diff);
		if (diff < convergenceCritirion)
			convergedModels.put(key, true);
		else {
			lambdas.put(key, newCurLambdas);
		}

	}

	private static void initializeLambdas() {
		lambdas = new HashMap<String, HashMap<String, Double>>();
		alphas = new HashMap<String, HashMap<String, Double>>();
		convergedModels = new HashMap<String, Boolean>();
		double initValue = 1.0 / numberOfLevels;
		for (String key : data.keySet()) {
			HashMap<String, Double> curLambda = new HashMap<String, Double>();
			String curCluster = getCurClusterFromKey(key);
			curLambda.put("average", initValue);
			curLambda.put("r", initValue);
			if (hierarchyFlag) {
				if (curCluster.equals("rr"))
					curLambda.put("rr", initValue);
				else if (curCluster.equals("rl"))
					curLambda.put("rl", initValue);
			}
			lambdas.put(key, curLambda);
			alphas.put(key, new HashMap<String, Double>());
			convergedModels.put(key, false);
		}

	}

	private static String getCurClusterFromKey(String key) {
		String[] tmpAr = key.split("_");
		return tmpAr[tmpAr.length - 1];
	}

	private static String getModelFromKey(String key) {
		String[] tmpAr = key.split("_");
		return tmpAr[0];
	}

	private static double estimateProbSuccessgivenM(String cluster,
			ArrayList<HashMap<String, Boolean>> curData) {
		double successes = 0;
		for (HashMap<String, Boolean> hm : curData) {
			if (hm.get(cluster))
				successes++;
		}
		double prob = successes / curData.size();
		// System.out.println("Probability of success of "+cluster+" :"+prob);
		return prob;

	}

	private static HashMap<String, ArrayList<HashMap<String, Boolean>>> loadData(
			Integer currentFold, String threshold) {
		System.out.println("Loading data...");

		/**
		 * key = model + approach + ScoreThreshold + cluster (rl or rr)
		 */

		data = new HashMap<String, ArrayList<HashMap<String, Boolean>>>();

		try {
			String inFile = PropertiesFactory.getInstance().getProps()
					.getProperty("results");
			String inputFile = inFile + "predictions"
					+ (currentFold != null ? ("_cv" + currentFold) : "")
					+ (threshold != null ? ("_" + threshold) : "") + ".csv";
			BufferedReader input = new BufferedReader(new FileReader(inputFile));

			System.out.println("Reading from input File:" + inputFile);
			String line;
			line = input.readLine();
			/**
			 * Predictions file:
			 * model,approach,ScoreThreshold,HistoryThreshold,actual,
			 * prediction,baseline,average,r,rl,rr,EMPrediction From this, I
			 * need average, r, rl, rr.
			 */
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");

				/*
				 * Key = model + approach + cluster
				 */
				String key = tmpAr[0] + "_" + tmpAr[1] + "_" + tmpAr[2];

				epsilonForSuccess = initialEpsilonForSuccess;// exclude history.
				if (!tmpAr[0].equals("Binomial")) {
					epsilonForSuccess = initialEpsilonForSuccess / 1;
					// System.out.println("Running multinomial. Epsilon:"+epsilonForSuccess);
				}

				double actualQuality = Double.parseDouble(tmpAr[4].trim());
				HashMap<String, Boolean> curInstance = new HashMap<String, Boolean>();
				curInstance.put(
						"average",
						getSuccess(Double.parseDouble(tmpAr[7].trim()),
								actualQuality));

				curInstance.put(
						"r",
						getSuccess(Double.parseDouble(tmpAr[8].trim()),
								actualQuality));

				double rl = Double.parseDouble(tmpAr[9].trim());
				double rr = Double.parseDouble(tmpAr[10].trim());

				String curCluster = "r";
				if (rl != -1) {
					curInstance.put("rl", getSuccess(rl, actualQuality));
					curCluster = "rl";
				} else if (rr != -1) {
					curInstance.put("rr", getSuccess(rr, actualQuality));
					curCluster = "rr";
				}

				key += "_" + curCluster;

				ArrayList<HashMap<String, Boolean>> list = data.get(key);
				if (list == null) {
					list = new ArrayList<HashMap<String, Boolean>>();
					data.put(key, list);
				}

				list.add(curInstance);

			}
			input.close();
			return data;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Boolean getSuccess(double prediction, double actualQuality) {
		double diff = Math.abs(prediction - actualQuality);
		return (diff <= epsilonForSuccess) ? true : false;
	}

}
