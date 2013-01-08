package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import kokkodis.factory.BinCategory;
import kokkodis.factory.ErrorHolder;
import kokkodis.factory.EvalWorker;
import kokkodis.factory.ModelCategory;
import kokkodis.factory.MultCategory;
import kokkodis.factory.PropertiesFactory;
import kokkodis.factory.RawInstance;

public class Evaluate {

	private static ErrorHolder errorHolder;
	private static int historyThreshold;
	private static GlobalVariables globalVariables;
	private static ArrayList<Integer> curCatIds;
	private static String basedon;
	private static HashMap<String, HashMap<String, Double>> lambdas;

	public static void evaluate() {

		if (!GlobalVariables.evaluateOnTrain)
			readLambdas();
		globalVariables = GlobalVariables.getInstance();
		if (GlobalVariables.printFiles)
			printCoefficients();

		System.out.println(GlobalVariables.line);

		System.out
				.println("model | approach |  ScoreThreshold | HistoryThreshold | MAE-model"
						+ " | MAE-Baseline" + " | MAE-EM"
				// + " | MSE-model"
				// + " | MSE-Baseline"
				);

		for (historyThreshold = 9; historyThreshold <= 40; historyThreshold += 2) {

			errorHolder = new ErrorHolder();

			readAndEvaluate();

			double maeBaseline = errorHolder.getBaselineMAESum()
					/ errorHolder.getTotalEvaluations();
			double maeBinomialModel = errorHolder.getBinomialModelMAESum()
					/ errorHolder.getTotalEvaluations();

			/*
			 * double mseBinomialModel = errorHolder.getBinomialModelMSESum() /
			 * errorHolder.getTotalEvaluations();
			 * 
			 * double mseBaseline = errorHolder.getBaselineMSESum() /
			 * errorHolder.getTotalEvaluations();
			 */
			double maeEMModel = errorHolder.getEMModelMAESum()
					/ errorHolder.getTotalEvaluations();

			String resStr = GlobalVariables.curModel + " | "
					+ GlobalVariables.curApproach + " | "
					+ GlobalVariables.currentBinomialThreshold + " | "
					+ historyThreshold + " | " + maeBinomialModel + " | "
					+ maeBaseline + " | " + maeEMModel;

			// mseBinomialModel + " | "
			// + mseBaseline;
			System.out.println(resStr);
			if (GlobalVariables.printFiles)

				GlobalVariables.allResultsFile.writeToFile(resStr.replaceAll(
						" \\| ", ","));

		}

	}

	private static void readLambdas() {
		if (lambdas == null) {
			lambdas = new HashMap<String, HashMap<String, Double>>();

			try {
				String f = PropertiesFactory.getInstance().getProps()
						.getProperty("results");
				f += "lambdas.csv";

				BufferedReader input = new BufferedReader(new FileReader(f));
				String line;
				line = input.readLine();

				/**
				 * model,approach, cluster, averageLambda, rLambda,
				 * clusterLambda
				 */
				while ((line = input.readLine()) != null) {
					String[] tmpAr = line.split(",");
					String key = createKey(tmpAr[0], tmpAr[1], tmpAr[2]);
					HashMap<String, Double> curLambdas = new HashMap<String, Double>();
					curLambdas.put("average",
							Double.parseDouble(tmpAr[3].trim()));
					curLambdas.put("r", Double.parseDouble(tmpAr[4].trim()));
					curLambdas.put(tmpAr[2],
							Double.parseDouble(tmpAr[5].trim()));
					lambdas.put(key, curLambdas);

				}
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static String createKey(String model, String approach,
			String cluster) {

		return model + "_" + approach + "_" + cluster;
	}

	private static void printCoefficients() {
		int maxCoeffs = -1;
		int totalCoeffs = 0;
		for (Entry<String, String[]> e : GlobalVariables.getInstance()
				.getClusterCategories().entrySet()) {
			totalCoeffs = e.getValue().length * (e.getValue().length - 1);
			maxCoeffs = Math.max(maxCoeffs, totalCoeffs);
		}

		String header = "model,Approach,cluster,ScoreThreshold";
		for (int i = 0; i < maxCoeffs; i++) {
			header += ",coeff" + i;
		}
		globalVariables.getOutputFile().writeToFile(header);
		maxCoeffs += 4;
		System.out.println("Max Coeffs:" + maxCoeffs);

		for (Entry<String, HashMap<String, HashMap<Integer, Double>>> eout : GlobalVariables
				.getCurCoeffs().entrySet()) {
			String str = "";
			str += GlobalVariables.curModel
					+ ","
					+ GlobalVariables.curApproach
					+ ","
					+ eout.getKey()
					+ ","
					+ (GlobalVariables.curModel.equals("Binomial") ? GlobalVariables.currentBinomialThreshold
							: "");
			ArrayList<Integer> cats = Utils.getCurCatIds(eout.getKey());
			String basedon = globalVariables.getClusterToBasedOn().get(
					eout.getKey());

			for (int cat : cats) {
				if (cat != 0) {
					HashMap<Integer, Double> hm;
					hm = eout.getValue().get(cat + basedon);
					for (int cat1 : cats) {

						str += "," + hm.get(cat1);
					}
				}

			}
			int length = str.split(",").length;
			for (int i = length; i < maxCoeffs; i++) {
				str += ",";
			}
			globalVariables.getOutputFile().writeToFile(str);
		}

	}

	private static void readAndEvaluate() {

		HashMap<Integer, EvalWorker> dataMapHolderEval = new HashMap<Integer, EvalWorker>();
		String inputDirectory = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath");
		inputDirectory += (GlobalVariables.evaluateOnTrain ? "train" : "test")
				+ ((GlobalVariables.currentFold != null) ? GlobalVariables.currentFold
						: "") + ".csv";

		try {
			BufferedReader input = new BufferedReader(new FileReader(
					inputDirectory));
			String line;
			line = input.readLine();

			/**
			 * contractor,category,score "
			 */

			while ((line = input.readLine()) != null) {
				RawInstance ri = Utils.stringToRawInstance(line);
				/*
				 * adjust category changes cat only if cat in root.
				 */
				updateEvalWorker(dataMapHolderEval, ri);
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void updateEvalWorker(
			HashMap<Integer, EvalWorker> dataMapHolderEval, RawInstance ri) {

		EvalWorker evalWorker = dataMapHolderEval.get(ri.getContractor());
		String catName = globalVariables.getCatIntToName()
				.get(ri.getCategory());
		String currentTaskCluster = globalVariables.getCategoriesToClusters()
				.get(catName);
		String workerType;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */

		if (evalWorker == null) {
			workerType = currentTaskCluster;
			evalWorker = initEvalWorker(ri, currentTaskCluster);
			dataMapHolderEval.put(ri.getContractor(), evalWorker);

		} else {// if not null, the worker is of one type, rl or rr.
			workerType = evalWorker.getWorkerType();
		}

		double numberPastTasks = evalWorker.getGenericHistoryMap().get(0)
				.getN();
		if (numberPastTasks > historyThreshold) {

			if (GlobalVariables.curApproach.equals("RS")) {
				for (int j = 0; j < GlobalVariables.rsTrials; j++) {
					updateErrors(evalWorker, ri, workerType, currentTaskCluster);

				}
			} else
				updateErrors(evalWorker, ri, workerType, currentTaskCluster);
		}

		updateEvalWorker(evalWorker, ri, currentTaskCluster);

	}

	private static void updateEvalWorker(EvalWorker evalWorker, RawInstance ri,
			String currentTaskCluster) {
		/**
		 * Adding the other category holders in case is absent.
		 */
		int genericCategory = Utils.adjustCategoryToRoot(ri.getCategory());

		ModelCategory specializedCurTaskCat = null;
		ModelCategory specializedOveralCategory = null;
		ModelCategory genericCurTask;
		ModelCategory genericOveral;
		if (GlobalVariables.hierarchicalFlag) {
			if (currentTaskCluster.equals("rr")) {
				specializedOveralCategory = evalWorker.getTechnicalHistoryMap()
						.get(0);
				if (specializedOveralCategory == null) {
					specializedOveralCategory = initBucketAndAddToMap(
							specializedOveralCategory,
							evalWorker.getTechnicalHistoryMap(), 0);
					specializedCurTaskCat = initBucketAndAddToMap(
							specializedCurTaskCat,
							evalWorker.getTechnicalHistoryMap(),
							ri.getCategory());

				} else {
					specializedCurTaskCat = evalWorker.getTechnicalHistoryMap()
							.get(ri.getCategory());
					if (specializedCurTaskCat == null) {
						specializedCurTaskCat = initBucketAndAddToMap(
								specializedCurTaskCat,
								evalWorker.getTechnicalHistoryMap(),
								ri.getCategory());

					}
				}

			} else {
				/* Non technical, rl */
				specializedOveralCategory = evalWorker.getNonTechHistoryMap()
						.get(0);
				if (specializedOveralCategory == null) {

					specializedCurTaskCat = initBucketAndAddToMap(
							specializedCurTaskCat,
							evalWorker.getNonTechHistoryMap(), ri.getCategory());
					specializedOveralCategory = initBucketAndAddToMap(
							specializedOveralCategory,
							evalWorker.getNonTechHistoryMap(), 0);

				} else {
					specializedCurTaskCat = evalWorker.getNonTechHistoryMap()
							.get(ri.getCategory());
					if (specializedCurTaskCat == null) {
						specializedCurTaskCat = initBucketAndAddToMap(
								specializedCurTaskCat,
								evalWorker.getNonTechHistoryMap(),
								ri.getCategory());
					}
				}

			}
		}
		genericCurTask = evalWorker.getGenericHistoryMap().get(genericCategory);
		genericOveral = evalWorker.getGenericHistoryMap().get(0);
		if (genericOveral == null) {

			genericOveral = initBucketAndAddToMap(genericOveral,
					evalWorker.getGenericHistoryMap(), 0);
			genericCurTask = initBucketAndAddToMap(genericCurTask,
					evalWorker.getGenericHistoryMap(), genericCategory);

		} else {
			if (genericCurTask == null) {
				genericCurTask = initBucketAndAddToMap(genericCurTask,
						evalWorker.getGenericHistoryMap(), genericCategory);
			}

		}

		Utils.addTaskOutcomeToCategory(genericOveral, ri.getScore());
		Utils.addTaskOutcomeToCategory(genericCurTask, ri.getScore());
		if (GlobalVariables.hierarchicalFlag) {
			Utils.addTaskOutcomeToCategory(specializedOveralCategory,
					ri.getScore());
			Utils.addTaskOutcomeToCategory(specializedCurTaskCat, ri.getScore());

			if (currentTaskCluster.equals("rr"))
				evalWorker.increaseTech();
			else
				evalWorker.increaseNonTech();
		}
	}

	private static ModelCategory initBucketAndAddToMap(ModelCategory mc,
			HashMap<Integer, ModelCategory> hashMap, int category) {
		if (GlobalVariables.curModel.equals("Binomial"))
			mc = new BinCategory();
		else

			mc = new MultCategory();
		hashMap.put(category, mc);
		return mc;

	}

	private static void updateErrors(EvalWorker evalWorker, RawInstance ri,
			String workerType, String currentTaskCluster) {

		errorHolder.setTotalEvaluations(errorHolder.getTotalEvaluations() + 1);

		double modelQuality = 0;
		double modelAbsoluteError = 0;
		double baselineEstimatedQuality = 0;
		double baselineAbsoluteError = 0;
		double emAbsoluteError = 0;

		double rIndependent = 0;
		double rlIndependent = -1;
		double rrIndependent = -1;
		double average = 0;

		double emquality = 0;

		modelQuality = predictModelQuality(evalWorker, ri, workerType,
				currentTaskCluster);
		rIndependent = predictIndependent(evalWorker, ri, "r");
		if (currentTaskCluster.equals("rr")) {
			rrIndependent = predictIndependent(evalWorker, ri,
					currentTaskCluster);
		} else {
			rlIndependent = predictIndependent(evalWorker, ri,
					currentTaskCluster);
		}

		if (!GlobalVariables.evaluateOnTrain) {
			emquality = estimateEMQuality(average, rIndependent,
					(rlIndependent != -1) ? rlIndependent : rrIndependent,
					currentTaskCluster);

			emAbsoluteError = Math.abs(emquality - ri.getScore());
			errorHolder.setEMModelMAESum(errorHolder.getEMModelMAESum()
					+ emAbsoluteError);

		}
		modelAbsoluteError = (Math.abs(modelQuality - ri.getScore()));

		if (GlobalVariables.curModel.equals("Binomial")) {
			baselineEstimatedQuality = estimateBinomialBaselineQuality(
					evalWorker, workerType, currentTaskCluster);
			average = estimateBinomialPlaneAverage(evalWorker);
		} else {
			baselineEstimatedQuality = estimateMultinomialBaselineQuality(
					evalWorker, workerType, currentTaskCluster);
			average = estimateMultinomialPlaneAverage(evalWorker);

		}
		baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
				- ri.getScore()));

		errorHolder.setBinomialModelMAESum(errorHolder.getBinomialModelMAESum()
				+ modelAbsoluteError);

		errorHolder.setBinomialModelMSESum(errorHolder.getBinomialModelMSESum()
				+ (Math.pow(modelAbsoluteError, 2)));

		errorHolder.setBaselineMAESum(errorHolder.getBaselineMAESum()
				+ baselineAbsoluteError);

		errorHolder.setBaselineMSESum(errorHolder.getBaselineMSESum()
				+ Math.pow(baselineAbsoluteError, 2));
		if (GlobalVariables.printFiles && GlobalVariables.outputPredictions) {

			GlobalVariables.predictions
					.writeToFile(GlobalVariables.curModel
							+ ","
							+ GlobalVariables.curApproach
							+ ","
							+ (GlobalVariables.curModel.equals("Binomial") ? GlobalVariables.currentBinomialThreshold
									: "-") + "," + historyThreshold + ","
							+ ri.getScore() + "," + modelQuality + ","
							+ baselineEstimatedQuality + "," + average + ","
							+ rIndependent + "," + rlIndependent + ","
							+ rrIndependent + "," + emquality);
		}

	}

	private static double estimateEMQuality(double average,
			double rIndependent, double clusterPrediction,
			String currentTaskCluster) {

		HashMap<String, Double> hm = lambdas.get(createKey(
				GlobalVariables.curModel, GlobalVariables.curApproach,
				currentTaskCluster));

		return (hm.get("average") * average) + (hm.get("r") * rIndependent)
				+ (hm.get(currentTaskCluster) * clusterPrediction);
	}

	private static double estimateMultinomialPlaneAverage(EvalWorker evalWorker) {
		return getAverageHistory(((MultCategory) evalWorker
				.getGenericHistoryMap().get(0)).getBucketSuccesses(),
				((MultCategory) evalWorker.getGenericHistoryMap().get(0))
						.getN());

	}

	private static double estimateBinomialPlaneAverage(EvalWorker evalWorker) {
		// Utils.printEvalWorker(evalWorker);
		return (((BinCategory) evalWorker.getGenericHistoryMap().get(0)).getX() / ((BinCategory) evalWorker
				.getGenericHistoryMap().get(0)).getN());

	}

	private static double predictIndependent(EvalWorker evalWorker,
			RawInstance ri, String cluster) {
		HashMap<Integer, ModelCategory> hm;
		HashMap<Integer, Double> coeffs;
		/**
		 * Use of root model.
		 */
		int catId = ri.getCategory();
		if (cluster.equals("r"))
			catId = Utils.adjustCategoryToRoot(ri.getCategory());

		coeffs = getCurrentCoeffsAndSetBasedOn(cluster, catId);
		hm = getAppropriateMap(cluster, evalWorker);
		curCatIds = Utils.getCurCatIds(cluster);

		return finalModelEstimation(coeffs, hm);
	}

	private static double predictModelQuality(EvalWorker evalWorker,
			RawInstance ri, String workerType, String currentTaskCluster) {
		HashMap<Integer, ModelCategory> hm;
		HashMap<Integer, Double> coeffs;
		/**
		 * Use of root model.
		 */
		if (!GlobalVariables.hierarchicalFlag
				|| !workerType.equals(currentTaskCluster)) {
			coeffs = getCurrentCoeffsAndSetBasedOn("r",
					Utils.adjustCategoryToRoot(ri.getCategory()));
			hm = getAppropriateMap("r", evalWorker);
			curCatIds = Utils.getCurCatIds("r");
			// System.out.println(" curCluster:R");
		} else {
			coeffs = getCurrentCoeffsAndSetBasedOn(currentTaskCluster,
					ri.getCategory());
			hm = getAppropriateMap(currentTaskCluster, evalWorker);
			curCatIds = Utils.getCurCatIds(currentTaskCluster);
		}

		return finalModelEstimation(coeffs, hm);
	}

	private static HashMap<Integer, ModelCategory> getAppropriateMap(
			String cluster, EvalWorker evalWorker) {
		if (cluster.equals("r"))
			return evalWorker.getGenericHistoryMap();
		if (cluster.equals("rl"))
			return evalWorker.getNonTechHistoryMap();
		return evalWorker.getTechnicalHistoryMap();
	}

	private static double finalModelEstimation(HashMap<Integer, Double> coeffs,
			HashMap<Integer, ModelCategory> hm) {
		double modelQuality = 0;

		if (GlobalVariables.curModel.equals("Binomial")) {
			if (GlobalVariables.curApproach.equals("PE"))

				modelQuality = binomialPointEstimate(coeffs, hm);
			else
				modelQuality = binomialDistroEstimate(coeffs, hm);
		} else {
			if (GlobalVariables.curApproach.equals("PE"))

				modelQuality = multinomialPointEstimate(coeffs, hm);
			else
				modelQuality = multinomialDistroEstimate(coeffs, hm);

		}
		return Utils.inverseLogit(modelQuality);

	}

	private static HashMap<Integer, Double> getCurrentCoeffsAndSetBasedOn(
			String cluster, int cat) {
		HashMap<String, HashMap<Integer, Double>> tmpCoeff = GlobalVariables
				.getCurCoeffs().get(cluster);
		basedon = globalVariables.getClusterToBasedOn().get(cluster);
		HashMap<Integer, Double> tmp = tmpCoeff.get(cat + basedon);
		if (tmp == null) {
			System.out.println("Cluster:" + cluster + " Cat:" + cat
					+ " basedOn:" + basedon + " gives me null coeffs.");
			for (String key : tmpCoeff.keySet()) {
				System.out.println(key);
			}
			System.exit(-1);

		}
		return tmp;

	}

	private static double multinomialDistroEstimate(
			HashMap<Integer, Double> coeffs, HashMap<Integer, ModelCategory> hm) {

		double modelQuality = 0;
		for (int i : curCatIds) {

			MultCategory bc = (MultCategory) hm.get(i);
			if (bc == null) {
				bc = new MultCategory();
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDirichletDistroEstimate(bc
										.getBucketSuccesses())));//
				/*
				 * Parameters estimated by fitting beta matlab (betafit)
				 * getLogit(getCatMeans(i));
				 */
			} else
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDirichletDistroEstimate(bc
										.getBucketSuccesses())));
		}
		return modelQuality;
	}

	private static double multinomialPointEstimate(
			HashMap<Integer, Double> coeffs, HashMap<Integer, ModelCategory> hm) {
		double modelQuality = 0;
		for (int i : curCatIds) {

			MultCategory mc = (MultCategory) hm.get(i);
			if (mc == null) {
				mc = new MultCategory();
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDirichletPointEstimate(mc.getQ_ijk())));
				// System.out.println(Utils
				// .getDirichletPointEstimate(mc.getQ_ijk()));

			}// getLogit(getCatMeans(i));
			else
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDirichletPointEstimate(mc.getQ_ijk())));

		}
		return modelQuality;
	}

	private static double binomialDistroEstimate(
			HashMap<Integer, Double> coeffs, HashMap<Integer, ModelCategory> hm) {
		double modelQuality = 0;
		for (int i : curCatIds) {
			BinCategory bc = (BinCategory) hm.get(i);
			if (bc == null)
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getDistroEstimate(0, 0)));//
			/*
			 * Parameters estimated by fitting beta matlab (betafit)
			 * getLogit(getCatMeans(i));
			 */
			else
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils.getDistroEstimate(
								bc.getX(), bc.getN())));
		}
		return modelQuality;

	}

	private static double binomialPointEstimate(
			HashMap<Integer, Double> coeffs, HashMap<Integer, ModelCategory> hm) {
		double modelQuality = 0;
		if (coeffs == null) {
			System.out.println("Null coefficients!");
		}
		for (int i : curCatIds) {

			BinCategory bc = (BinCategory) hm.get(i);
			if (bc == null)
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.getBinomialPointEstimate(0, 0));// getLogit(getCatMeans(i));
			else
				modelQuality += coeffs.get(i)
						* Utils.getLogit(Utils.fix(Utils
								.getBinomialPointEstimate(bc.getX(), bc.getN())));
		}
		return modelQuality;

	}

	private static EvalWorker initEvalWorker(RawInstance ri,
			String currentTaskCluster) {

		ModelCategory specializedOveralCategory = null;
		ModelCategory specializedCurTaskCat = null;

		ModelCategory genericCurTaskCat = null;
		ModelCategory genericOveralCategory = null;

		EvalWorker evalWorker = new EvalWorker();
		evalWorker.setWorkerId(ri.getContractor());

		if (GlobalVariables.curModel.equals("Binomial")) {
			specializedCurTaskCat = new BinCategory();
			genericCurTaskCat = new BinCategory();
			specializedOveralCategory = new BinCategory();
			genericOveralCategory = new BinCategory();
		} else {
			specializedCurTaskCat = new MultCategory();
			genericCurTaskCat = new MultCategory();
			specializedOveralCategory = new MultCategory();
			genericOveralCategory = new MultCategory();

		}
		evalWorker.getGenericHistoryMap().put(0, genericOveralCategory);
		evalWorker.getGenericHistoryMap()
				.put(Utils.adjustCategoryToRoot(ri.getCategory()),
						genericCurTaskCat);
		if (GlobalVariables.hierarchicalFlag) {
			if (currentTaskCluster.equals("rr")) {
				evalWorker.getTechnicalHistoryMap().put(ri.getCategory(),
						specializedCurTaskCat);
				evalWorker.getTechnicalHistoryMap().put(0,
						specializedOveralCategory);
			} else {
				evalWorker.getNonTechHistoryMap().put(0,
						specializedOveralCategory);
				evalWorker.getNonTechHistoryMap().put(ri.getCategory(),
						specializedCurTaskCat);
			}
		}
		return evalWorker;

	}

	private static double estimateBinomialBaselineQuality(
			EvalWorker evalWorker, String workerType, String currentTask) {

		// Utils.printEvalWorker(evalWorker);
		if (!GlobalVariables.hierarchicalFlag
				|| !workerType.equals(currentTask)) {
			return (((BinCategory) evalWorker.getGenericHistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getGenericHistoryMap()
					.get(0)).getN());

		} else if (workerType.equals("rr")) {

			return (((BinCategory) evalWorker.getTechnicalHistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker
					.getTechnicalHistoryMap().get(0)).getN());
		} else {

			return (((BinCategory) evalWorker.getNonTechHistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getNonTechHistoryMap()
					.get(0)).getN());
		}

	}

	private static double estimateMultinomialBaselineQuality(
			EvalWorker evalWorker, String workerType, String currentTask) {
		if (!GlobalVariables.hierarchicalFlag
				|| !workerType.equals(currentTask)) {
			return getAverageHistory(((MultCategory) evalWorker
					.getGenericHistoryMap().get(0)).getBucketSuccesses(),
					((MultCategory) evalWorker.getGenericHistoryMap().get(0))
							.getN());
		} else if (workerType.equals("rr")) {
			return getAverageHistory(((MultCategory) evalWorker
					.getTechnicalHistoryMap().get(0)).getBucketSuccesses(),
					((MultCategory) evalWorker.getTechnicalHistoryMap().get(0))
							.getN());
		} else
			return getAverageHistory(((MultCategory) evalWorker
					.getNonTechHistoryMap().get(0)).getBucketSuccesses(),
					((MultCategory) evalWorker.getNonTechHistoryMap().get(0))
							.getN());
	}

	private static double getAverageHistory(double[] bucketSuccesses, double n) {
		double sum = 0;
		for (int i = 0; i < bucketSuccesses.length; i++) {
			sum += bucketSuccesses[i] * GlobalVariables.qualities[i];
			// System.out.println("Bucket successes:"+bucketSuccesses[i]+" qualities:"+GlobalVariables.qualities[i]);
		}
		// System.out.println("Bucket successes length:"+bucketSuccesses.length+" n:"+n);
		// System.out.println( sum / n);
		return sum / n;
	}

}
