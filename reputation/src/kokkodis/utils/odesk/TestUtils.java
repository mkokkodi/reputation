package kokkodis.utils.odesk;

import java.io.File;
import java.util.HashMap;

import kokkodis.factory.BinCategory;
import kokkodis.factory.EvalWorker;
import kokkodis.factory.EvalWorkerSynthetic;
import kokkodis.factory.ModelCategory;
import kokkodis.factory.MultCategory;
import kokkodis.odesk.ODeskRegressions;
import kokkodis.odesk.ODeskTest;
import kokkodis.odesk.Reputation;
import kokkodis.synthetic.DataGenerationHierarchy;
import kokkodis.synthetic.SyntheticTest;
import kokkodis.synthetic.SyntheticTestHier;
import kokkodis.synthetic.SyntheticTrain;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.Utils;

public class TestUtils extends Utils {



	public TestUtils() {
	
	}

	public void updateEvalWorker(
			HashMap<Integer, EvalWorker> dataMapHolderEval, int developerId,
			Integer catId, boolean successfulOutcome, double score,
			String approach, String workerType, String currentTask, String model) {

		EvalWorker evalWorker = dataMapHolderEval.get(developerId);
		HashMap<Integer, ModelCategory> genericHistoryMapHolder;
		HashMap<Integer, ModelCategory> techHistoryMapHolder;
		HashMap<Integer, ModelCategory> nonTechHistoryMapHolder;

		double currentReviews = 0;
		BinCategory specializedOveralCategory = null;
		BinCategory genericOveralCategory = null;

		BinCategory specializedCurTaskCat = null;
		BinCategory genericCurTaskCat = null;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */
		if (evalWorker == null) {

			evalWorker = new EvalWorker();
			evalWorker.setWorkerId(developerId);
			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
			techHistoryMapHolder = evalWorker.getTechnicalHistoryMap();
			nonTechHistoryMapHolder = evalWorker.getNonTechHistoryMap();

			specializedCurTaskCat = new BinCategory();
			genericCurTaskCat = new BinCategory();
			specializedOveralCategory = new BinCategory();
			genericOveralCategory = new BinCategory();

			genericHistoryMapHolder.put(0, genericOveralCategory);

			if (currentTask == null) {
				genericHistoryMapHolder.put(catId, genericCurTaskCat);
			} else if (currentTask.equals("Technical")) {
				techHistoryMapHolder.put(catId, specializedCurTaskCat);
				techHistoryMapHolder.put(0, specializedOveralCategory);
				genericHistoryMapHolder.put(1, genericCurTaskCat);
			} else {
				nonTechHistoryMapHolder.put(0, specializedOveralCategory);
				nonTechHistoryMapHolder.put(catId, specializedCurTaskCat);
				genericHistoryMapHolder.put(2, genericCurTaskCat);
			}

			dataMapHolderEval.put(developerId, evalWorker);

		} else {

			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
			techHistoryMapHolder = evalWorker.getTechnicalHistoryMap();
			nonTechHistoryMapHolder = evalWorker.getNonTechHistoryMap();

			if (currentTask == null) {
				genericCurTaskCat = (BinCategory) genericHistoryMapHolder
						.get(catId);
			} else if (currentTask.equals("Technical")) {
				specializedCurTaskCat = (BinCategory) techHistoryMapHolder
						.get(catId);
				genericCurTaskCat = (BinCategory) genericHistoryMapHolder
						.get(1);
				specializedOveralCategory = (BinCategory) techHistoryMapHolder
						.get(0);
			} else {
				specializedCurTaskCat = (BinCategory) nonTechHistoryMapHolder
						.get(catId);
				genericCurTaskCat = (BinCategory) genericHistoryMapHolder
						.get(2);
				specializedOveralCategory = (BinCategory) nonTechHistoryMapHolder
						.get(0);
			}
			genericOveralCategory = (BinCategory) genericHistoryMapHolder
					.get(0);

			currentReviews = genericOveralCategory.getN();
			if (currentReviews > ODeskTest.historyThreshold) {

				if (approach.equals("RS")) {
					for (int j = 0; j < 20; j++) {
						updateErrors(score, evalWorker, approach, catId,
								workerType, currentTask, model);

					}
				} else
					updateErrors(score, evalWorker, approach, catId,
							workerType, currentTask, model);
			}
		}

		if (currentTask != null && specializedCurTaskCat == null) {
			specializedCurTaskCat = new BinCategory();
			if (currentTask.equals("Technical")) {

				techHistoryMapHolder.put(catId, specializedCurTaskCat);
			} else {
				nonTechHistoryMapHolder.put(catId, specializedCurTaskCat);
			}

		}

		if (currentTask != null && specializedOveralCategory == null) {
			specializedOveralCategory = new BinCategory();
			if (currentTask.equals("Technical")) {

				techHistoryMapHolder.put(0, specializedOveralCategory);
			} else {
				nonTechHistoryMapHolder.put(0, specializedOveralCategory);
			}

		}

		if (genericCurTaskCat == null) {
			genericCurTaskCat = new BinCategory();
			if (currentTask == null)
				genericHistoryMapHolder.put(catId, genericCurTaskCat);
			else if (currentTask.equals("Technical")) {

				genericHistoryMapHolder.put(1, genericCurTaskCat);
			} else {
				genericHistoryMapHolder.put(2, genericCurTaskCat);
			}
		}

		if (currentTask != null) {
			addTaskOutcomeToCategory(specializedCurTaskCat, successfulOutcome);
			addTaskOutcomeToCategory(specializedOveralCategory,
					successfulOutcome);
		}
		addTaskOutcomeToCategory(genericCurTaskCat, successfulOutcome);
		addTaskOutcomeToCategory(genericOveralCategory, successfulOutcome);

		if (currentTask != null) {
			if (currentTask.equals("Technical"))
				evalWorker.increaseTech();
			else
				evalWorker.increaseNonTech();
		}

	}

	private void updateErrors(double actualInstanceQuality,
			EvalWorker evalWorker, String approach, Integer catId,
			String workerType, String currentTask, String model) {

		ODeskTest.errorHolder.setTotalEvaluations(ODeskTest.errorHolder
				.getTotalEvaluations() + 1);

		double modelQuality = 0;
		double modelAbsoluteError = 0;
		double baselineEstimatedQuality = 0;
		double baselineAbsoluteError = 0;

		if (model.equals("Binomial")) {
			modelQuality = predictBinomialModelQuality(catId, evalWorker,
					approach, workerType, currentTask);
			modelAbsoluteError = (Math
					.abs(modelQuality - actualInstanceQuality));

			baselineEstimatedQuality = estimateBinomialBaselineQuality(
					evalWorker, workerType, currentTask);
			baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
					- actualInstanceQuality));
		} else if (model.equals("Multinomial")) {
			modelQuality = predictMultinomialModelQuality(catId, evalWorker,
					approach, workerType, currentTask);
			modelAbsoluteError = (Math
					.abs(modelQuality - actualInstanceQuality));

			baselineEstimatedQuality = estimateMultinomialBaselineQuality(
					evalWorker, workerType, currentTask);
			baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
					- actualInstanceQuality));
		} else {
			System.out.println("Error in models!");
		}

		ODeskTest.errorHolder.setBinomialModelMAESum(ODeskTest.errorHolder
				.getBinomialModelMAESum() + modelAbsoluteError);

		ODeskTest.errorHolder.setBinomialModelMSESum(ODeskTest.errorHolder
				.getBinomialModelMSESum() + (Math.pow(modelAbsoluteError, 2)));

		ODeskTest.errorHolder.setBaselineMAESum(ODeskTest.errorHolder
				.getBaselineMAESum() + baselineAbsoluteError);

		ODeskTest.errorHolder.setBaselineMSESum(ODeskTest.errorHolder
				.getBaselineMSESum() + Math.pow(baselineAbsoluteError, 2));

	}

	private void updateErrors(double actualInstanceQuality,
			EvalWorkerSynthetic evalWorker, String approach, Integer catId,
			String workerType, int currentCluster, String model) {

		ODeskTest.errorHolder.setTotalEvaluations(ODeskTest.errorHolder
				.getTotalEvaluations() + 1);

		double modelQuality = 0;
		double modelAbsoluteError = 0;
		double baselineEstimatedQuality = 0;
		double baselineAbsoluteError = 0;

		if (model.equals("Binomial")) {
			modelQuality = predictBinomialModelQuality(catId, evalWorker,
					approach, workerType, currentCluster);
			modelAbsoluteError = (Math
					.abs(modelQuality - actualInstanceQuality));

			baselineEstimatedQuality = estimateBinomialBaselineQuality(
					evalWorker, workerType, currentCluster);
			baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
					- actualInstanceQuality));
		} else {
			System.out.println("Error in models!");
		}

		ODeskTest.errorHolder.setBinomialModelMAESum(ODeskTest.errorHolder
				.getBinomialModelMAESum() + modelAbsoluteError);

		ODeskTest.errorHolder.setBinomialModelMSESum(ODeskTest.errorHolder
				.getBinomialModelMSESum() + (Math.pow(modelAbsoluteError, 2)));

		ODeskTest.errorHolder.setBaselineMAESum(ODeskTest.errorHolder
				.getBaselineMAESum() + baselineAbsoluteError);

		ODeskTest.errorHolder.setBaselineMSESum(ODeskTest.errorHolder
				.getBaselineMSESum() + Math.pow(baselineAbsoluteError, 2));

	}

	private double estimateBinomialBaselineQuality(
			EvalWorkerSynthetic evalWorker, String workerType,
			int currentCluster) {
		if (workerType.equals("0") && currentCluster == 0) {
			return (((BinCategory) evalWorker.getCluster0HistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getCluster0HistoryMap()
					.get(0)).getN());
		} else if (workerType.equals("1") && currentCluster == 1) {
			return (((BinCategory) evalWorker.getCluster1HistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getCluster1HistoryMap()
					.get(0)).getN());

		} else if (workerType.equals("2") && currentCluster == 2) {
			return (((BinCategory) evalWorker.getCluster2HistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getCluster2HistoryMap()
					.get(0)).getN());

		} else {
			return (((BinCategory) evalWorker.getCluster3HistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getCluster3HistoryMap()
					.get(0)).getN());
		}

	}

	private double estimateMultinomialBaselineQuality(EvalWorker evalWorker,
			String workerType, String currentTask) {

		if (workerType.equals("Technical") && currentTask.equals("Technical")) {
			return getAverageHistory(((MultCategory) evalWorker
					.getTechnicalHistoryMap().get(0)).getBucketSuccesses(),
					((MultCategory) evalWorker.getTechnicalHistoryMap().get(0))
							.getN());
		} else if ((workerType.equals("Technical") && currentTask
				.equals("Non-technical"))
				|| (workerType.equals("Non-technical") && currentTask
						.equals("Technical"))) {
			return getAverageHistory(((MultCategory) evalWorker
					.getGenericHistoryMap().get(0)).getBucketSuccesses(),
					((MultCategory) evalWorker.getGenericHistoryMap().get(0))
							.getN());
		} else
			return getAverageHistory(((MultCategory) evalWorker
					.getNonTechHistoryMap().get(0)).getBucketSuccesses(),
					((MultCategory) evalWorker.getNonTechHistoryMap().get(0))
							.getN());
	}

	private double getAverageHistory(double[] bucketSuccesses, double n) {
		double sum = 0;
		for (int i = 0; i < bucketSuccesses.length; i++) {
			sum += bucketSuccesses[i] * qualities[i];
		}
		return sum / n;
	}

	private double predictMultinomialModelQuality(Integer catId,
			EvalWorker evalWorker, String approach, String workerType,
			String currentTask) {

		Double[] coeffs;

		HashMap<String, Double[]> tmpCoeff = new HashMap<String, Double[]>();
		HashMap<Integer, ModelCategory> hm = new HashMap<Integer, ModelCategory>();

		if (workerType.equals("Technical") && currentTask.equals("Technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Technical");
			Reputation.mPlus1 = 4;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
			hm = evalWorker.getTechnicalHistoryMap();
		} else if (workerType.equals("Technical")
				&& currentTask.equals("Non-technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic");
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			// System.out
			// .println(workerType + "," + currentTask + tmpCoeff.size());
			hm = evalWorker.getGenericHistoryMap();
			catId = 2;

		} else if (workerType.equals("Non-technical")
				&& currentTask.equals("Non-technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Non-technical");
			Reputation.mPlus1 = 4;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
			hm = evalWorker.getNonTechHistoryMap();

		} else if (workerType.equals("Non-technical")
				&& currentTask.equals("Technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic");
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			hm = evalWorker.getGenericHistoryMap();
			catId = 1;

		}

		double modelQuality = 0;

		if (tmpCoeff == null)
			System.out.println("The temp coeff is null.");

		coeffs = tmpCoeff.get((catId) + ODeskRegressions.basedOn);
		/*
		 * if(catId == 1 && currentTask.equals("Technical") && ODeskTest.flag){
		 * for(double d1:coeffs) System.out.println(d1); ODeskTest.flag=false; }
		 */
		if (approach.equals("PE")) {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				MultCategory bc = (MultCategory) hm.get(i);
				if (bc == null) {
					bc = new MultCategory();
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletPointEstimate(bc
									.getQ_ijk())));
				}// getLogit(getCatMeans(i));
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletPointEstimate(bc
									.getQ_ijk())));

			}
		} else {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				MultCategory bc = (MultCategory) hm.get(i);
				if (bc == null) {
					bc = new MultCategory();
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletDistroEstimate(bc
									.getBucketSuccesses())));//
					/*
					 * Parameters estimated by fitting beta matlab (betafit)
					 * getLogit(getCatMeans(i));
					 */
				} else
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletDistroEstimate(bc
									.getBucketSuccesses())));
			}
		}
		return inverseLogit(modelQuality);
	}

	private double estimateBinomialBaselineQuality(EvalWorker evalWorker,
			String workerType, String currentTask) {
		if(workerType == null ||(workerType.equals("Technical") && currentTask
				.equals("Non-technical"))){
			return (((BinCategory) evalWorker.getGenericHistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getGenericHistoryMap()
					.get(0)).getN());
		} else
		if (workerType.equals("Technical") && currentTask.equals("Technical")) {
			return (((BinCategory) evalWorker.getTechnicalHistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker
					.getTechnicalHistoryMap().get(0)).getN());
		} else
			return (((BinCategory) evalWorker.getNonTechHistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getNonTechHistoryMap()
					.get(0)).getN());

	}

	private double predictBinomialModelQuality(int catId,
			EvalWorker evalWorker, String approach, String workerType,
			String currentTask) {
		Double[] coeffs;

		HashMap<String, Double[]> tmpCoeff = new HashMap<String, Double[]>();
		HashMap<Integer, ModelCategory> hm = new HashMap<Integer, ModelCategory>();

		if (workerType == null) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic");
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			hm = evalWorker.getGenericHistoryMap();
		} else if (workerType.equals("Technical")
				&& currentTask.equals("Technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Technical");
			Reputation.mPlus1 = 4;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
			hm = evalWorker.getTechnicalHistoryMap();
		} else if (workerType.equals("Technical")
				&& currentTask.equals("Non-technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic");
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			// System.out
			// .println(workerType + "," + currentTask + tmpCoeff.size());
			hm = evalWorker.getGenericHistoryMap();
			catId = 2;

		} else if (workerType.equals("Non-technical")
				&& currentTask.equals("Non-technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Non-technical");
			Reputation.mPlus1 = 4;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
			hm = evalWorker.getNonTechHistoryMap();

		} else if (workerType.equals("Non-technical")
				&& currentTask.equals("Technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic");
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			hm = evalWorker.getGenericHistoryMap();
			catId = 1;

		}

		double modelQuality = 0;

		// System.out.println("cat:"+catId+" based on:"+
		// ODeskRegressions.basedOn);
		coeffs = tmpCoeff.get((catId) + ODeskRegressions.basedOn);

		if (approach.equals("PE")) {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				BinCategory bc = (BinCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i]
							* getLogit(getBinomialPointEstimate(0, 0));// getLogit(getCatMeans(i));
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getBinomialPointEstimate(bc.getX(),
									bc.getN())));

			}
		} else {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				BinCategory bc = (BinCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i]
							* getLogit(fix(getDistroEstimate(0, 0)));//
				/*
				 * Parameters estimated by fitting beta matlab (betafit)
				 * getLogit(getCatMeans(i));
				 */
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getDistroEstimate(bc.getX(),
									bc.getN())));
			}
		}
		ODeskTest.outputProbs.writeToFile(evalWorker.getWorkerId()+","+modelQuality);
		return inverseLogit(modelQuality);
	}

	private double predictBinomialModelQuality(int catId,
			EvalWorkerSynthetic evalWorker, String approach, String workerType,
			int currentCluster) {
		Double[] coeffs;

		HashMap<String, Double[]> tmpCoeff = new HashMap<String, Double[]>();
		HashMap<Integer, ModelCategory> hm = new HashMap<Integer, ModelCategory>();
		Reputation.mPlus1 = 4;

		ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";

		if (workerType.equals("0") && currentCluster == 0) {
			tmpCoeff = SyntheticTestHier.allModelCoeffs.get(0);

			hm = evalWorker.getCluster0HistoryMap();
		} else if (workerType.equals("1") && currentCluster == 1) {
			tmpCoeff = SyntheticTestHier.allModelCoeffs.get(1);

			hm = evalWorker.getCluster1HistoryMap();
		} else if (workerType.equals("2") && currentCluster == 2) {
			tmpCoeff = SyntheticTestHier.allModelCoeffs.get(2);

			hm = evalWorker.getCluster2HistoryMap();

		} else {
			tmpCoeff = SyntheticTestHier.allModelCoeffs.get(3);

			hm = evalWorker.getCluster3HistoryMap();
			catId = currentCluster + 1;
		}

		double modelQuality = 0;

		coeffs = tmpCoeff.get((catId) + ODeskRegressions.basedOn);

		if (approach.equals("PE")) {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				BinCategory bc = (BinCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i]
							* getLogit(getBinomialPointEstimate(0, 0));// getLogit(getCatMeans(i));
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getBinomialPointEstimate(bc.getX(),
									bc.getN())));

			}
		} else {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				BinCategory bc = (BinCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i]
							* getLogit(fix(getDistroEstimate(0, 0)));//
				/*
				 * Parameters estimated by fitting beta matlab (betafit)
				 * getLogit(getCatMeans(i));
				 */
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getDistroEstimate(bc.getX(),
									bc.getN())));
			}
		}
		return inverseLogit(modelQuality);
	}

	public void updateEvalWorker(
			HashMap<Integer, EvalWorker> dataMapHolderEval, int developerId,
			Integer catId, int bucket, double actualTaskScore, String approach,
			String workerType, String currentTask, String model) {

		EvalWorker evalWorker = dataMapHolderEval.get(developerId);
		HashMap<Integer, ModelCategory> genericHistoryMapHolder;
		HashMap<Integer, ModelCategory> techHistoryMapHolder;
		HashMap<Integer, ModelCategory> nonTechHistoryMapHolder;

		double currentReviews = 0;
		MultCategory specializedOveralCategory = null;
		MultCategory genericOveralCategory = null;

		MultCategory specializedCurTaskCat = null;
		MultCategory genericCurTaskCat = null;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */
		if (evalWorker == null) {

			evalWorker = new EvalWorker();
			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
			techHistoryMapHolder = evalWorker.getTechnicalHistoryMap();
			nonTechHistoryMapHolder = evalWorker.getNonTechHistoryMap();

			specializedCurTaskCat = new MultCategory();
			genericCurTaskCat = new MultCategory();
			specializedOveralCategory = new MultCategory();
			genericOveralCategory = new MultCategory();

			genericHistoryMapHolder.put(0, genericOveralCategory);

			if (currentTask.equals("Technical")) {
				techHistoryMapHolder.put(catId, specializedCurTaskCat);
				techHistoryMapHolder.put(0, specializedOveralCategory);
				genericHistoryMapHolder.put(1, genericCurTaskCat);
			} else {
				nonTechHistoryMapHolder.put(0, specializedOveralCategory);
				nonTechHistoryMapHolder.put(catId, specializedCurTaskCat);
				genericHistoryMapHolder.put(2, genericCurTaskCat);
			}

			dataMapHolderEval.put(developerId, evalWorker);

		} else {

			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
			techHistoryMapHolder = evalWorker.getTechnicalHistoryMap();
			nonTechHistoryMapHolder = evalWorker.getNonTechHistoryMap();

			if (currentTask.equals("Technical")) {
				specializedCurTaskCat = (MultCategory) techHistoryMapHolder
						.get(catId);
				genericCurTaskCat = (MultCategory) genericHistoryMapHolder
						.get(1);
				specializedOveralCategory = (MultCategory) techHistoryMapHolder
						.get(0);
			} else {
				specializedCurTaskCat = (MultCategory) nonTechHistoryMapHolder
						.get(catId);
				genericCurTaskCat = (MultCategory) genericHistoryMapHolder
						.get(2);
				specializedOveralCategory = (MultCategory) nonTechHistoryMapHolder
						.get(0);
			}
			genericOveralCategory = (MultCategory) genericHistoryMapHolder
					.get(0);

			currentReviews = genericHistoryMapHolder.get(0).getN();
			if (currentReviews > ODeskTest.historyThreshold) {

				if (approach.equals("RS")) {
					for (int j = 0; j < 20; j++) {
						updateErrors(actualTaskScore, evalWorker, approach,
								catId, workerType, currentTask, model);

					}
				} else
					updateErrors(actualTaskScore, evalWorker, approach, catId,
							workerType, currentTask, model);
			}
		}

		if (specializedCurTaskCat == null) {
			specializedCurTaskCat = new MultCategory();
			if (currentTask.equals("Technical")) {

				techHistoryMapHolder.put(catId, specializedCurTaskCat);
			} else {
				nonTechHistoryMapHolder.put(catId, specializedCurTaskCat);
			}

		}

		if (specializedOveralCategory == null) {
			specializedOveralCategory = new MultCategory();
			if (currentTask.equals("Technical")) {

				techHistoryMapHolder.put(0, specializedOveralCategory);
			} else {
				nonTechHistoryMapHolder.put(0, specializedOveralCategory);
			}

		}

		if (genericCurTaskCat == null) {
			genericCurTaskCat = new MultCategory();
			if (currentTask.equals("Technical")) {

				genericHistoryMapHolder.put(1, genericCurTaskCat);
			} else {
				genericHistoryMapHolder.put(2, genericCurTaskCat);
			}
		}

		addTaskOutcomeToCategory(specializedCurTaskCat, bucket);
		addTaskOutcomeToCategory(specializedOveralCategory, bucket);
		addTaskOutcomeToCategory(genericCurTaskCat, bucket);
		addTaskOutcomeToCategory(genericOveralCategory, bucket);

		if (currentTask.equals("Technical"))
			evalWorker.increaseTech();
		else
			evalWorker.increaseNonTech();

	}

	/**
	 * For Synthetic Experiment
	 * 
	 * @param
	 */

	public void updateEvalWorker(
			HashMap<Integer, EvalWorker> dataMapHolderEval, int developerId,
			int catId, boolean succesfullOutcome, double actualTaskScore,
			String approach, String model) {
		EvalWorker evalWorker = dataMapHolderEval.get(developerId);
		HashMap<Integer, ModelCategory> genericHistoryMapHolder;

		double currentReviews = 0;
		BinCategory specializedOveralCategory = null;

		BinCategory specializedCurTaskCat = null;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */
		if (evalWorker == null) {

			evalWorker = new EvalWorker();
			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();

			specializedCurTaskCat = new BinCategory();
			specializedOveralCategory = new BinCategory();

			genericHistoryMapHolder.put(0, specializedOveralCategory);

			genericHistoryMapHolder.put(catId, specializedCurTaskCat);

			dataMapHolderEval.put(developerId, evalWorker);

		} else {

			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();

			specializedCurTaskCat = (BinCategory) genericHistoryMapHolder
					.get(catId);
			specializedOveralCategory = (BinCategory) genericHistoryMapHolder
					.get(0);
		}

		currentReviews = specializedOveralCategory.getN();
		if (currentReviews > ODeskTest.historyThreshold) {

			if (approach.equals("RS")) {
				for (int j = 0; j < 20; j++) {
					updateErrors(actualTaskScore, evalWorker, approach, catId,
							model);

				}
			} else
				updateErrors(actualTaskScore, evalWorker, approach, catId,
						model);
		}

		if (specializedCurTaskCat == null)

		{
			specializedCurTaskCat = new BinCategory();

			genericHistoryMapHolder.put(catId, specializedCurTaskCat);

		}

		addTaskOutcomeToCategory(specializedCurTaskCat, succesfullOutcome);
		addTaskOutcomeToCategory(specializedOveralCategory, succesfullOutcome);

	}

	private void updateErrors(double actualInstanceQuality,
			EvalWorker evalWorker, String approach, int catId, String model) {
		SyntheticTest.errorHolder.setTotalEvaluations(SyntheticTest.errorHolder
				.getTotalEvaluations() + 1);

		double modelQuality = 0;
		double modelAbsoluteError = 0;
		double baselineEstimatedQuality = 0;
		double baselineAbsoluteError = 0;

		if (model.equals("Binomial")) {
			modelQuality = predictBinomialModelQuality(catId, evalWorker,
					approach);
			if (Double.isNaN(modelQuality)) {
				SyntheticTest.errorHolder
						.setTotalEvaluations(SyntheticTest.errorHolder
								.getTotalEvaluations() - 1);
			} else {
				modelAbsoluteError = (Math.abs(modelQuality
						- actualInstanceQuality));

				// System.out.println("modelAbsoluteError:"+modelAbsoluteError
				// +" actual:"+actualInstanceQuality+" model:"+modelQuality );
				baselineEstimatedQuality = estimateBinomialBaselineQuality(evalWorker);
				baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
						- actualInstanceQuality));
			}
		} else if (model.equals("Multinomial")) {
			modelQuality = predictMultinomialModelQuality(catId, evalWorker,
					approach);
			modelAbsoluteError = (Math
					.abs(modelQuality - actualInstanceQuality));

			baselineEstimatedQuality = estimateMultinomialBaselineQuality(evalWorker);
			baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
					- actualInstanceQuality));
		} else {
			System.out.println("Error in models!");
		}
		if (modelQuality != Double.NaN) {
			SyntheticTest.errorHolder
					.setBinomialModelMAESum(SyntheticTest.errorHolder
							.getBinomialModelMAESum() + modelAbsoluteError);

			SyntheticTest.errorHolder
					.setBinomialModelMSESum(SyntheticTest.errorHolder
							.getBinomialModelMSESum()
							+ (Math.pow(modelAbsoluteError, 2)));

			SyntheticTest.errorHolder
					.setBaselineMAESum(SyntheticTest.errorHolder
							.getBaselineMAESum() + baselineAbsoluteError);

			SyntheticTest.errorHolder
					.setBaselineMSESum(SyntheticTest.errorHolder
							.getBaselineMSESum()
							+ Math.pow(baselineAbsoluteError, 2));
		}
	}

	private double estimateMultinomialBaselineQuality(EvalWorker evalWorker) {
		// TODO Auto-generated method stub
		return 0;
	}

	private double predictMultinomialModelQuality(int catId,
			EvalWorker evalWorker, String approach) {
		// TODO Auto-generated method stub
		return 0;
	}

	private double estimateBinomialBaselineQuality(EvalWorker evalWorker) {
		return (((BinCategory) evalWorker.getGenericHistoryMap().get(0)).getX() / ((BinCategory) evalWorker
				.getGenericHistoryMap().get(0)).getN());
	}

	private double predictBinomialModelQuality(int catId,
			EvalWorker evalWorker, String approach) {
		Double[] coeffs;

		HashMap<String, Double[]> tmpCoeff = new HashMap<String, Double[]>();
		HashMap<Integer, ModelCategory> hm = new HashMap<Integer, ModelCategory>();

		tmpCoeff = SyntheticTest.coeffs;
		Reputation.mPlus1 = SyntheticTrain.categories + 1;

		ODeskRegressions.basedOn = "_BasedOn";
		for (int i = 0; i < SyntheticTrain.categories + 1; i++) {
			ODeskRegressions.basedOn += "_" + i;
		}
		hm = evalWorker.getGenericHistoryMap();

		double modelQuality = 0;

		coeffs = tmpCoeff.get((catId) + ODeskRegressions.basedOn);

		if (coeffs != null) {
			if (approach.equals("PE")) {
				for (int i = 0; i < Reputation.mPlus1; i++) {
					BinCategory bc = (BinCategory) hm.get(i);
					if (bc == null)
						modelQuality += coeffs[i]
								* getLogit(getBinomialPointEstimate(0, 0));// getLogit(getCatMeans(i));
					else
						modelQuality += coeffs[i]
								* getLogit(fix(getBinomialPointEstimate(
										bc.getX(), bc.getN())));

				}
			} else {
				for (int i = 0; i < Reputation.mPlus1; i++) {
					BinCategory bc = (BinCategory) hm.get(i);
					if (bc == null)
						modelQuality += coeffs[i]
								* getLogit(fix(getDistroEstimate(0, 0)));//
					/*
					 * Parameters estimated by fitting beta matlab (betafit)
					 * getLogit(getCatMeans(i));
					 */
					else
						modelQuality += coeffs[i]
								* getLogit(fix(getDistroEstimate(bc.getX(),
										bc.getN())));
				}
			}
		} else
			return Double.NaN;
		return inverseLogit(modelQuality);
	}

	public void updateEvalWorker(
			HashMap<Integer, EvalWorker> dataMapHolderEval, int developerId,
			int catId, int bucket, double actualTaskScore, String approach,
			String model) {
		// TODO Auto-generated method stub

	}

	public int getCluster(int catId) {
		return (catId - 1) / DataGenerationHierarchy.noOfClusters;
	}

	public void updateEvalWorkerSyntheticHier(
			HashMap<Integer, EvalWorkerSynthetic> dataMapHolderEval,
			int developerId, int catId, boolean succesfullOutcome,
			double actualTaskScore, String approach, String workerType,
			int curCluster, String model) {

		EvalWorkerSynthetic evalWorker = dataMapHolderEval.get(developerId);
		HashMap<Integer, ModelCategory> cluster3HistoryMapHolder;
		HashMap<Integer, ModelCategory> cluster0HistoryMapHolder;
		HashMap<Integer, ModelCategory> cluster1HistoryMapHolder;
		HashMap<Integer, ModelCategory> cluster2HistoryMapHolder;

		double currentReviews = 0;
		BinCategory specializedOveralCategory = null;
		BinCategory genericOveralCategory = null;

		BinCategory specializedCurTaskCat = null;
		BinCategory genericCurTaskCat = null;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */
		if (evalWorker == null) {

			evalWorker = new EvalWorkerSynthetic();
			cluster3HistoryMapHolder = evalWorker.getCluster3HistoryMap();
			cluster0HistoryMapHolder = evalWorker.getCluster0HistoryMap();
			cluster1HistoryMapHolder = evalWorker.getCluster1HistoryMap();
			cluster2HistoryMapHolder = evalWorker.getCluster2HistoryMap();

			specializedCurTaskCat = new BinCategory();
			genericCurTaskCat = new BinCategory();
			specializedOveralCategory = new BinCategory();
			genericOveralCategory = new BinCategory();

			cluster3HistoryMapHolder.put(0, genericOveralCategory);

			if (curCluster == 0) {
				cluster0HistoryMapHolder.put(catId, specializedCurTaskCat);
				cluster0HistoryMapHolder.put(0, specializedOveralCategory);
				cluster3HistoryMapHolder.put(1, genericCurTaskCat);
			} else if (curCluster == 1) {
				cluster1HistoryMapHolder.put(0, specializedOveralCategory);
				cluster1HistoryMapHolder.put(catId, specializedCurTaskCat);
				cluster3HistoryMapHolder.put(2, genericCurTaskCat);
			} else {
				cluster2HistoryMapHolder.put(0, specializedOveralCategory);
				cluster2HistoryMapHolder.put(catId, specializedCurTaskCat);
				cluster3HistoryMapHolder.put(3, genericCurTaskCat);
			}

			dataMapHolderEval.put(developerId, evalWorker);

		} else {

			cluster3HistoryMapHolder = evalWorker.getCluster3HistoryMap();
			cluster0HistoryMapHolder = evalWorker.getCluster0HistoryMap();
			cluster1HistoryMapHolder = evalWorker.getCluster1HistoryMap();
			cluster2HistoryMapHolder = evalWorker.getCluster2HistoryMap();

			if (curCluster == 0) {
				specializedCurTaskCat = (BinCategory) cluster0HistoryMapHolder
						.get(catId);
				genericCurTaskCat = (BinCategory) cluster3HistoryMapHolder
						.get(1);
				specializedOveralCategory = (BinCategory) cluster0HistoryMapHolder
						.get(0);
			}

			else if (curCluster == 1) {
				specializedCurTaskCat = (BinCategory) cluster1HistoryMapHolder
						.get(catId);
				genericCurTaskCat = (BinCategory) cluster3HistoryMapHolder
						.get(2);
				specializedOveralCategory = (BinCategory) cluster1HistoryMapHolder
						.get(0);
			} else {
				specializedCurTaskCat = (BinCategory) cluster2HistoryMapHolder
						.get(catId);
				genericCurTaskCat = (BinCategory) cluster3HistoryMapHolder
						.get(3);
				specializedOveralCategory = (BinCategory) cluster2HistoryMapHolder
						.get(0);
			}
			genericOveralCategory = (BinCategory) cluster3HistoryMapHolder
					.get(0);

			currentReviews = genericOveralCategory.getN();
			if (currentReviews > ODeskTest.historyThreshold) {

				if (approach.equals("RS")) {
					for (int j = 0; j < 20; j++) {
						updateErrors(actualTaskScore, evalWorker, approach,
								catId, workerType, curCluster, model);

					}
				} else
					updateErrors(actualTaskScore, evalWorker, approach, catId,
							workerType, curCluster, model);
			}
		}

		if (specializedCurTaskCat == null) {
			specializedCurTaskCat = new BinCategory();
			if (curCluster == 0) {

				cluster0HistoryMapHolder.put(catId, specializedCurTaskCat);
			} else if (curCluster == 1) {
				cluster1HistoryMapHolder.put(catId, specializedCurTaskCat);
			} else {
				cluster2HistoryMapHolder.put(catId, specializedCurTaskCat);
			}

		}

		if (specializedOveralCategory == null) {
			specializedOveralCategory = new BinCategory();
			if (curCluster == 0) {

				cluster0HistoryMapHolder.put(0, specializedOveralCategory);
			} else if (curCluster == 1) {
				cluster1HistoryMapHolder.put(0, specializedOveralCategory);
			} else {
				cluster2HistoryMapHolder.put(0, specializedOveralCategory);
			}

		}

		if (genericCurTaskCat == null) {
			genericCurTaskCat = new BinCategory();
			if (curCluster == 0) {

				cluster3HistoryMapHolder.put(1, genericCurTaskCat);
			} else if (curCluster == 1) {
				cluster3HistoryMapHolder.put(2, genericCurTaskCat);
			} else {
				cluster3HistoryMapHolder.put(3, genericCurTaskCat);
			}
		}

		addTaskOutcomeToCategory(specializedCurTaskCat, succesfullOutcome);
		addTaskOutcomeToCategory(specializedOveralCategory, succesfullOutcome);
		addTaskOutcomeToCategory(genericCurTaskCat, succesfullOutcome);
		addTaskOutcomeToCategory(genericOveralCategory, succesfullOutcome);

		if (curCluster == 0)
			evalWorker.increaseCluster0();
		else if (curCluster == 1)
			evalWorker.increaseCluster1();
		else
			evalWorker.increaseCluster2();
	}

	public void updateEvalWorkerCV(
			HashMap<Integer, EvalWorker> dataMapHolderEval, int developerId,
			Integer catId, boolean successfulOutcome, double score,
			String approach, String workerType, String currentTask,
			String model, int fold) {

		EvalWorker evalWorker = dataMapHolderEval.get(developerId);
		HashMap<Integer, ModelCategory> genericHistoryMapHolder;
		HashMap<Integer, ModelCategory> techHistoryMapHolder;
		HashMap<Integer, ModelCategory> nonTechHistoryMapHolder;

		double currentReviews = 0;
		BinCategory specializedOveralCategory = null;
		BinCategory genericOveralCategory = null;

		BinCategory specializedCurTaskCat = null;
		BinCategory genericCurTaskCat = null;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */
		if (evalWorker == null) {

			evalWorker = new EvalWorker();
			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
			techHistoryMapHolder = evalWorker.getTechnicalHistoryMap();
			nonTechHistoryMapHolder = evalWorker.getNonTechHistoryMap();

			specializedCurTaskCat = new BinCategory();
			genericCurTaskCat = new BinCategory();
			specializedOveralCategory = new BinCategory();
			genericOveralCategory = new BinCategory();

			genericHistoryMapHolder.put(0, genericOveralCategory);

			if (currentTask.equals("Technical")) {
				techHistoryMapHolder.put(catId, specializedCurTaskCat);
				techHistoryMapHolder.put(0, specializedOveralCategory);
				genericHistoryMapHolder.put(1, genericCurTaskCat);
			} else {
				nonTechHistoryMapHolder.put(0, specializedOveralCategory);
				nonTechHistoryMapHolder.put(catId, specializedCurTaskCat);
				genericHistoryMapHolder.put(2, genericCurTaskCat);
			}

			dataMapHolderEval.put(developerId, evalWorker);

		} else {

			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
			techHistoryMapHolder = evalWorker.getTechnicalHistoryMap();
			nonTechHistoryMapHolder = evalWorker.getNonTechHistoryMap();

			if (currentTask.equals("Technical")) {
				specializedCurTaskCat = (BinCategory) techHistoryMapHolder
						.get(catId);
				genericCurTaskCat = (BinCategory) genericHistoryMapHolder
						.get(1);
				specializedOveralCategory = (BinCategory) techHistoryMapHolder
						.get(0);
			} else {
				specializedCurTaskCat = (BinCategory) nonTechHistoryMapHolder
						.get(catId);
				genericCurTaskCat = (BinCategory) genericHistoryMapHolder
						.get(2);
				specializedOveralCategory = (BinCategory) nonTechHistoryMapHolder
						.get(0);
			}
			genericOveralCategory = (BinCategory) genericHistoryMapHolder
					.get(0);

			currentReviews = genericOveralCategory.getN();
			if (currentReviews > ODeskTest.historyThreshold) {

				if (approach.equals("RS")) {
					for (int j = 0; j < 20; j++) {
						updateErrorsCV(score, evalWorker, approach, catId,
								workerType, currentTask, model, fold);

					}
				} else
					updateErrorsCV(score, evalWorker, approach, catId,
							workerType, currentTask, model, fold);
			}
		}

		if (specializedCurTaskCat == null) {
			specializedCurTaskCat = new BinCategory();
			if (currentTask.equals("Technical")) {

				techHistoryMapHolder.put(catId, specializedCurTaskCat);
			} else {
				nonTechHistoryMapHolder.put(catId, specializedCurTaskCat);
			}

		}

		if (specializedOveralCategory == null) {
			specializedOveralCategory = new BinCategory();
			if (currentTask.equals("Technical")) {

				techHistoryMapHolder.put(0, specializedOveralCategory);
			} else {
				nonTechHistoryMapHolder.put(0, specializedOveralCategory);
			}

		}

		if (genericCurTaskCat == null) {
			genericCurTaskCat = new BinCategory();
			if (currentTask.equals("Technical")) {

				genericHistoryMapHolder.put(1, genericCurTaskCat);
			} else {
				genericHistoryMapHolder.put(2, genericCurTaskCat);
			}
		}

		addTaskOutcomeToCategory(specializedCurTaskCat, successfulOutcome);
		addTaskOutcomeToCategory(specializedOveralCategory, successfulOutcome);
		addTaskOutcomeToCategory(genericCurTaskCat, successfulOutcome);
		addTaskOutcomeToCategory(genericOveralCategory, successfulOutcome);

		if (currentTask.equals("Technical"))
			evalWorker.increaseTech();
		else
			evalWorker.increaseNonTech();
	}

	private void updateErrorsCV(double actualInstanceQuality,
			EvalWorker evalWorker, String approach, Integer catId,
			String workerType, String currentTask, String model, int fold) {

		ODeskTest.errorHolder.setTotalEvaluations(ODeskTest.errorHolder
				.getTotalEvaluations() + 1);

		double modelQuality = 0;
		double modelAbsoluteError = 0;
		double baselineEstimatedQuality = 0;
		double baselineAbsoluteError = 0;

		if (model.equals("Binomial")) {
			modelQuality = predictBinomialModelQualityCV(catId, evalWorker,
					approach, workerType, currentTask, fold);
			modelAbsoluteError = (Math
					.abs(modelQuality - actualInstanceQuality));

			baselineEstimatedQuality = estimateBinomialBaselineQuality(
					evalWorker, workerType, currentTask);
			baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
					- actualInstanceQuality));
		} else if (model.equals("Multinomial")) {
			modelQuality = predictMultinomialModelQualityCV(catId, evalWorker,
					approach, workerType, currentTask, fold);
			modelAbsoluteError = (Math
					.abs(modelQuality - actualInstanceQuality));

			baselineEstimatedQuality = estimateMultinomialBaselineQuality(
					evalWorker, workerType, currentTask);
			baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
					- actualInstanceQuality));
		} else {
			System.out.println("Error in models!");
		}

		ODeskTest.errorHolder.setBinomialModelMAESum(ODeskTest.errorHolder
				.getBinomialModelMAESum() + modelAbsoluteError);

		ODeskTest.errorHolder.setBinomialModelMSESum(ODeskTest.errorHolder
				.getBinomialModelMSESum() + (Math.pow(modelAbsoluteError, 2)));

		ODeskTest.errorHolder.setBaselineMAESum(ODeskTest.errorHolder
				.getBaselineMAESum() + baselineAbsoluteError);

		ODeskTest.errorHolder.setBaselineMSESum(ODeskTest.errorHolder
				.getBaselineMSESum() + Math.pow(baselineAbsoluteError, 2));

	}

	private double predictBinomialModelQualityCV(int catId,
			EvalWorker evalWorker, String approach, String workerType,
			String currentTask, int fold) {
		Double[] coeffs;

		HashMap<String, Double[]> tmpCoeff = new HashMap<String, Double[]>();
		HashMap<Integer, ModelCategory> hm = new HashMap<Integer, ModelCategory>();

		if (workerType.equals("Technical") && currentTask.equals("Technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Technical" + fold);
			Reputation.mPlus1 = 4;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
			hm = evalWorker.getTechnicalHistoryMap();
		} else if (workerType.equals("Technical")
				&& currentTask.equals("Non-technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic" + fold);
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			// System.out
			// .println(workerType + "," + currentTask + tmpCoeff.size());
			hm = evalWorker.getGenericHistoryMap();
			catId = 2;

		} else if (workerType.equals("Non-technical")
				&& currentTask.equals("Non-technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Non-technical" + fold);
			Reputation.mPlus1 = 4;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
			hm = evalWorker.getNonTechHistoryMap();

		} else if (workerType.equals("Non-technical")
				&& currentTask.equals("Technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic" + fold);
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			hm = evalWorker.getGenericHistoryMap();
			catId = 1;

		}

		double modelQuality = 0;

		coeffs = tmpCoeff.get((catId) + ODeskRegressions.basedOn);

		if (approach.equals("PE")) {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				BinCategory bc = (BinCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i]
							* getLogit(getBinomialPointEstimate(0, 0));// getLogit(getCatMeans(i));
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getBinomialPointEstimate(bc.getX(),
									bc.getN())));

			}
		} else {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				BinCategory bc = (BinCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i]
							* getLogit(fix(getDistroEstimate(0, 0)));//
				/*
				 * Parameters estimated by fitting beta matlab (betafit)
				 * getLogit(getCatMeans(i));
				 */
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getDistroEstimate(bc.getX(),
									bc.getN())));
			}
		}
		return inverseLogit(modelQuality);
	}

	private double predictMultinomialModelQualityCV(Integer catId,
			EvalWorker evalWorker, String approach, String workerType,
			String currentTask, int fold) {

		Double[] coeffs;

		HashMap<String, Double[]> tmpCoeff = new HashMap<String, Double[]>();
		HashMap<Integer, ModelCategory> hm = new HashMap<Integer, ModelCategory>();

		if (workerType.equals("Technical") && currentTask.equals("Technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Technical" + fold);
			Reputation.mPlus1 = 4;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
			hm = evalWorker.getTechnicalHistoryMap();
		} else if (workerType.equals("Technical")
				&& currentTask.equals("Non-technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic" + fold);
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			// System.out
			// .println(workerType + "," + currentTask + tmpCoeff.size());
			hm = evalWorker.getGenericHistoryMap();
			catId = 2;

		} else if (workerType.equals("Non-technical")
				&& currentTask.equals("Non-technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Non-technical" + fold);
			Reputation.mPlus1 = 4;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2_3";
			hm = evalWorker.getNonTechHistoryMap();

		} else if (workerType.equals("Non-technical")
				&& currentTask.equals("Technical")) {
			tmpCoeff = ODeskTest.allModelCoeffs.get("Generic" + fold);
			Reputation.mPlus1 = 3;
			ODeskRegressions.basedOn = "_BasedOn_0_1_2";
			hm = evalWorker.getGenericHistoryMap();
			catId = 1;

		}

		double modelQuality = 0;

		coeffs = tmpCoeff.get((catId) + ODeskRegressions.basedOn);
		/*
		 * if(catId == 1 && currentTask.equals("Technical") && ODeskTest.flag){
		 * for(double d1:coeffs) System.out.println(d1); ODeskTest.flag=false; }
		 */
		if (approach.equals("PE")) {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				MultCategory bc = (MultCategory) hm.get(i);
				if (bc == null) {
					bc = new MultCategory();
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletPointEstimate(bc
									.getQ_ijk())));
				}// getLogit(getCatMeans(i));
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletPointEstimate(bc
									.getQ_ijk())));

			}
		} else {
			for (int i = 0; i < Reputation.mPlus1; i++) {
				MultCategory bc = (MultCategory) hm.get(i);
				if (bc == null) {
					bc = new MultCategory();
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletDistroEstimate(bc
									.getBucketSuccesses())));//
					/*
					 * Parameters estimated by fitting beta matlab (betafit)
					 * getLogit(getCatMeans(i));
					 */
				} else
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletDistroEstimate(bc
									.getBucketSuccesses())));
			}
		}
		return inverseLogit(modelQuality);
	}

	public void updateEvalWorkerCV(
			HashMap<Integer, EvalWorker> dataMapHolderEval, int developerId,
			Integer catId, int bucket, double actualTaskScore, String approach,
			String workerType, String currentTask, String model, int fold) {

		EvalWorker evalWorker = dataMapHolderEval.get(developerId);
		HashMap<Integer, ModelCategory> genericHistoryMapHolder;
		HashMap<Integer, ModelCategory> techHistoryMapHolder;
		HashMap<Integer, ModelCategory> nonTechHistoryMapHolder;

		double currentReviews = 0;
		MultCategory specializedOveralCategory = null;
		MultCategory genericOveralCategory = null;

		MultCategory specializedCurTaskCat = null;
		MultCategory genericCurTaskCat = null;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */
		if (evalWorker == null) {

			evalWorker = new EvalWorker();
			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
			techHistoryMapHolder = evalWorker.getTechnicalHistoryMap();
			nonTechHistoryMapHolder = evalWorker.getNonTechHistoryMap();

			specializedCurTaskCat = new MultCategory();
			genericCurTaskCat = new MultCategory();
			specializedOveralCategory = new MultCategory();
			genericOveralCategory = new MultCategory();

			genericHistoryMapHolder.put(0, genericOveralCategory);

			if (currentTask.equals("Technical")) {
				techHistoryMapHolder.put(catId, specializedCurTaskCat);
				techHistoryMapHolder.put(0, specializedOveralCategory);
				genericHistoryMapHolder.put(1, genericCurTaskCat);
			} else {
				nonTechHistoryMapHolder.put(0, specializedOveralCategory);
				nonTechHistoryMapHolder.put(catId, specializedCurTaskCat);
				genericHistoryMapHolder.put(2, genericCurTaskCat);
			}

			dataMapHolderEval.put(developerId, evalWorker);

		} else {

			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
			techHistoryMapHolder = evalWorker.getTechnicalHistoryMap();
			nonTechHistoryMapHolder = evalWorker.getNonTechHistoryMap();

			if (currentTask.equals("Technical")) {
				specializedCurTaskCat = (MultCategory) techHistoryMapHolder
						.get(catId);
				genericCurTaskCat = (MultCategory) genericHistoryMapHolder
						.get(1);
				specializedOveralCategory = (MultCategory) techHistoryMapHolder
						.get(0);
			} else {
				specializedCurTaskCat = (MultCategory) nonTechHistoryMapHolder
						.get(catId);
				genericCurTaskCat = (MultCategory) genericHistoryMapHolder
						.get(2);
				specializedOveralCategory = (MultCategory) nonTechHistoryMapHolder
						.get(0);
			}
			genericOveralCategory = (MultCategory) genericHistoryMapHolder
					.get(0);

			currentReviews = genericHistoryMapHolder.get(0).getN();
			if (currentReviews > ODeskTest.historyThreshold) {

				if (approach.equals("RS")) {
					for (int j = 0; j < 20; j++) {
						updateErrorsCV(actualTaskScore, evalWorker, approach,
								catId, workerType, currentTask, model, fold);

					}
				} else
					updateErrorsCV(actualTaskScore, evalWorker, approach,
							catId, workerType, currentTask, model, fold);
			}
		}

		if (specializedCurTaskCat == null) {
			specializedCurTaskCat = new MultCategory();
			if (currentTask.equals("Technical")) {

				techHistoryMapHolder.put(catId, specializedCurTaskCat);
			} else {
				nonTechHistoryMapHolder.put(catId, specializedCurTaskCat);
			}

		}

		if (specializedOveralCategory == null) {
			specializedOveralCategory = new MultCategory();
			if (currentTask.equals("Technical")) {

				techHistoryMapHolder.put(0, specializedOveralCategory);
			} else {
				nonTechHistoryMapHolder.put(0, specializedOveralCategory);
			}

		}

		if (genericCurTaskCat == null) {
			genericCurTaskCat = new MultCategory();
			if (currentTask.equals("Technical")) {

				genericHistoryMapHolder.put(1, genericCurTaskCat);
			} else {
				genericHistoryMapHolder.put(2, genericCurTaskCat);
			}
		}

		addTaskOutcomeToCategory(specializedCurTaskCat, bucket);
		addTaskOutcomeToCategory(specializedOveralCategory, bucket);
		addTaskOutcomeToCategory(genericCurTaskCat, bucket);
		addTaskOutcomeToCategory(genericOveralCategory, bucket);

		if (currentTask.equals("Technical"))
			evalWorker.increaseTech();
		else
			evalWorker.increaseNonTech();

	}

}
