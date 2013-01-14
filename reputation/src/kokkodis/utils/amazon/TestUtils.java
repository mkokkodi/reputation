package kokkodis.utils.amazon;

import java.util.HashMap;

import kokkodis.amazon.AmazonRegressions;
import kokkodis.amazon.AmazonTest;
import kokkodis.amazon.AmazonTrain;
import kokkodis.factory.BinCategory;
import kokkodis.factory.EvalWorker;
import kokkodis.factory.ModelCategory;
import kokkodis.factory.MultCategory;
import kokkodis.utils.Utils;

public class TestUtils extends Utils {

	public TestUtils() {
	}

	/**
	 * This is for Binomial Model!
	 * 
	 * @param dataMapHolderEval
	 * @param developerId
	 * @param catId
	 * @param successfulOutcome
	 * @param score
	 * @param approach
	 */
	public void updateEvalWorker(
			HashMap<String, EvalWorker> dataMapHolderEval, String developerId,
			Integer catId, boolean successfulOutcome, double score,
			String approach) {

		EvalWorker evalWorker = dataMapHolderEval.get(developerId);
		HashMap<Integer, ModelCategory> genericHistoryMapHolder;

		double currentReviews = 0;
		BinCategory genericOveralCategory = null;
		BinCategory genericCurTaskCat = null;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */
		if (evalWorker == null) {

			evalWorker = new EvalWorker();
			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();

			genericCurTaskCat = new BinCategory();
			genericOveralCategory = new BinCategory();

			genericHistoryMapHolder.put(0, genericOveralCategory);

			genericHistoryMapHolder.put(catId, genericCurTaskCat);

			dataMapHolderEval.put(developerId, evalWorker);

		} else {

			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();

				genericCurTaskCat = (BinCategory) genericHistoryMapHolder
						.get(catId);
			genericOveralCategory = (BinCategory) genericHistoryMapHolder
					.get(0);

			currentReviews = genericOveralCategory.getN();
			if (currentReviews > AmazonTest.historyThreshold) {

				if (approach.equals("RS")) {
					for (int j = 0; j < 30; j++) {
						updateErrors(score, evalWorker, approach, catId,
								 "Binomial");

					}
				} else
					updateErrors(score, evalWorker, approach, catId,
							 "Binomial");
			}
		}

	

		if (genericCurTaskCat == null) {
			genericCurTaskCat = new BinCategory();

				genericHistoryMapHolder.put(catId, genericCurTaskCat);
		}

		addTaskOutcomeToCategory(genericCurTaskCat, successfulOutcome);
		addTaskOutcomeToCategory(genericOveralCategory, successfulOutcome);


	}

	private void updateErrors(double actualInstanceQuality,
			EvalWorker evalWorker, String approach, Integer catId,
			 String model) {

		AmazonTest.errorHolder.setTotalEvaluations(AmazonTest.errorHolder
				.getTotalEvaluations() + 1);

		double modelQuality;
		double modelAbsoluteError;
		double baselineEstimatedQuality;
		double baselineAbsoluteError;

		if (model.equals("Binomial")) {
			modelQuality = predictBinomialModelQuality(catId, evalWorker,
					approach);
			modelAbsoluteError = (Math
					.abs(modelQuality - actualInstanceQuality));

			baselineEstimatedQuality = estimateBinomialBaselineQuality(
					evalWorker);
			baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
					- actualInstanceQuality));
		} else {
			modelQuality = predictMultinomialModelQuality(catId, evalWorker,
					approach);
			modelAbsoluteError = (Math
					.abs(modelQuality - actualInstanceQuality));

			baselineEstimatedQuality = estimateMultinomialBaselineQuality(
					evalWorker);
			baselineAbsoluteError = (Math.abs(baselineEstimatedQuality
					- actualInstanceQuality));
		}

		AmazonTest.errorHolder.setBinomialModelMAESum(AmazonTest.errorHolder
				.getBinomialModelMAESum() + modelAbsoluteError);

		AmazonTest.errorHolder.setBinomialModelMSESum(AmazonTest.errorHolder
				.getBinomialModelMSESum() + (Math.pow(modelAbsoluteError, 2)));

		AmazonTest.errorHolder.setBaselineMAESum(AmazonTest.errorHolder.getBaselineMAESum()
				+ baselineAbsoluteError);

		AmazonTest.errorHolder.setBaselineMSESum(AmazonTest.errorHolder.getBaselineMSESum()
				+ Math.pow(baselineAbsoluteError, 2));

	}

	private double estimateMultinomialBaselineQuality(EvalWorker evalWorker
			) {

			return getAverageHistory(((MultCategory) evalWorker
					.getGenericHistoryMap().get(0)).getBucketSuccesses(),
					((MultCategory) evalWorker.getGenericHistoryMap().get(0))
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
			EvalWorker evalWorker, String approach) {

		Double[] coeffs;

		HashMap<String, Double[]> tmpCoeff = new HashMap<String, Double[]>();
		HashMap<Integer, ModelCategory> hm = new HashMap<Integer, ModelCategory>();

			tmpCoeff = AmazonTest.coeffs;
		
			AmazonRegressions.basedOn = "_BasedOn_0_1_2_3_4";
			hm = evalWorker.getGenericHistoryMap();
	

		double modelQuality = 0;

		coeffs = tmpCoeff.get((catId) + AmazonRegressions.basedOn);

		if (approach.equals("PE")) {
			for (int i = 0; i < AmazonTrain.mPlus1; i++) {
				MultCategory bc = (MultCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i] * getLogit(0.8);// getLogit(getCatMeans(i));
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletPointEstimate(bc
									.getQ_ijk())));

			}
		} else {
			for (int i = 0; i < AmazonTrain.mPlus1; i++) {
				MultCategory bc = (MultCategory) hm.get(i);
				if (bc == null) {
					double[] prior = { 0, 2, 4, 6, 8};
					modelQuality += coeffs[i]
							* getLogit(fix(getDirichletDistroEstimate(prior)));//
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

	private double estimateBinomialBaselineQuality(EvalWorker evalWorker
			) {
	
			return (((BinCategory) evalWorker.getGenericHistoryMap().get(0))
					.getX() / ((BinCategory) evalWorker.getGenericHistoryMap()
					.get(0)).getN());

	

	}

	private double predictBinomialModelQuality(int catId,
			EvalWorker evalWorker, String approach) {
		Double[] coeffs;

		HashMap<String, Double[]> tmpCoeff = new HashMap<String, Double[]>();
		HashMap<Integer, ModelCategory> hm = new HashMap<Integer, ModelCategory>();

			tmpCoeff = AmazonTest.coeffs;
			hm = evalWorker.getGenericHistoryMap();
			AmazonTrain.mPlus1=5;


		double modelQuality = 0;

		coeffs = tmpCoeff.get((catId) + AmazonRegressions.basedOn);

		if (approach.equals("PE")) {
			for (int i = 0; i < AmazonTrain.mPlus1; i++) {
				BinCategory bc = (BinCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i] * getLogit(0.8);// getLogit(getCatMeans(i));
				else
					modelQuality += coeffs[i]
							* getLogit(fix(getBinomialPointEstimate(bc.getX(),
									bc.getN())));

			}
		} else {
			for (int i = 0; i < AmazonTrain.mPlus1; i++) {
				BinCategory bc = (BinCategory) hm.get(i);
				if (bc == null)
					modelQuality += coeffs[i]
							* getLogit(fix(getDistroEstimate(1,1)));//
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

	/**
	 * This is for Multinomial Model! 
	 * 
	 * @param dataMapHolderEval
	 * @param developerId
	 * @param catId
	 * @param bucket
	 * @param actualTaskScore
	 * @param approach
	 */
	public void updateEvalWorker(
			HashMap<String, EvalWorker> dataMapHolderEval, String developerId,
			Integer catId, int bucket, double actualTaskScore, String approach
			) {

		EvalWorker evalWorker = dataMapHolderEval.get(developerId);
		HashMap<Integer, ModelCategory> genericHistoryMapHolder;

		double currentReviews = 0;
		MultCategory genericOveralCategory = null;

		MultCategory genericCurTaskCat = null;

		/*
		 * Creates the necessary objects in order to add history in the end of
		 * the procedure The update follows the conditions!!
		 */
		if (evalWorker == null) {

			evalWorker = new EvalWorker();
			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();

			genericCurTaskCat = new MultCategory();
			genericOveralCategory = new MultCategory();

			genericHistoryMapHolder.put(0, genericOveralCategory);

				genericHistoryMapHolder.put(catId, genericCurTaskCat);

			dataMapHolderEval.put(developerId, evalWorker);

		} else {

			genericHistoryMapHolder = evalWorker.getGenericHistoryMap();
				genericCurTaskCat = (MultCategory) genericHistoryMapHolder
						.get(catId);
			genericOveralCategory = (MultCategory) genericHistoryMapHolder
					.get(0);

			currentReviews = genericHistoryMapHolder.get(0).getN();
			if (currentReviews > AmazonTest.historyThreshold) {

				if (approach.equals("RS")) {
					for (int j = 0; j < 30; j++) {
						updateErrors(actualTaskScore, evalWorker, approach,
								catId, "Multinomial");

					}
				} else
					updateErrors(actualTaskScore, evalWorker, approach, catId,
						 "Multinomial");
			}
		}


		if (genericCurTaskCat == null) {
			genericCurTaskCat = new MultCategory();

				genericHistoryMapHolder.put(catId, genericCurTaskCat);
		}

		addTaskOutcomeToCategory(genericCurTaskCat, bucket);
		addTaskOutcomeToCategory(genericOveralCategory, bucket);


	}

}
