package kokkodis.utils.amazon;

import java.util.HashMap;

import kokkodis.amazon.AmazonTrain;
import kokkodis.factory.BinCategory;
import kokkodis.factory.ModelCategory;
import kokkodis.factory.MultCategory;
import kokkodis.odesk.Reputation;
import kokkodis.utils.Utils;

public class TrainUtils extends Utils {

	public TrainUtils() {
		// TODO Auto-generated constructor stub
	}

	public void updateWorkerHistoryAndPrintTuple(
			HashMap<String, HashMap<Integer, ModelCategory>> dataMapHolder,
			String developerId, int catId, boolean succesfullOutcome,
			double actualTaskScore, String approach, String model) {

		HashMap<Integer, ModelCategory> workerHistoryMapHolder = dataMapHolder
				.get(developerId);
		if (workerHistoryMapHolder == null) {
			workerHistoryMapHolder = new HashMap<Integer, ModelCategory>();
			BinCategory curTaskCat = new BinCategory();
			BinCategory overalCategory = new BinCategory();
			addTaskOutcomeToCategory(curTaskCat, succesfullOutcome);
			addTaskOutcomeToCategory(overalCategory, succesfullOutcome);

			workerHistoryMapHolder.put(0, overalCategory);
			workerHistoryMapHolder.put(catId, curTaskCat);

			dataMapHolder.put(developerId, workerHistoryMapHolder);
		} else {
			BinCategory currentTaslCat = (BinCategory) workerHistoryMapHolder
					.get(catId);
			BinCategory overalCategory = (BinCategory) workerHistoryMapHolder
					.get(0);
			if (overalCategory.getN() > Reputation.historyThr) {
				if (approach.equals("PE"))
					addNewTupleToTrainingFile(workerHistoryMapHolder,
							developerId, catId, actualTaskScore, approach,
							model);
				else {
					for (int i = 0; i < 30; i++)
						addNewTupleToTrainingFile(workerHistoryMapHolder,
								developerId, catId, actualTaskScore, approach,
								model);
				}
			}
			if (currentTaslCat == null) {
				currentTaslCat = new BinCategory();
				workerHistoryMapHolder.put(catId, currentTaslCat);
			}
			addTaskOutcomeToCategory(currentTaslCat, succesfullOutcome);
			addTaskOutcomeToCategory(workerHistoryMapHolder.get(0),
					succesfullOutcome);
		}

	}

	private void addNewTupleToTrainingFile(
			HashMap<Integer, ModelCategory> workerHistoryMapHolder,
			String developerId, int catId, double actualTaskScore,
			String approach, String model) {

		String str = "" + developerId;

		if (model.equals("Binomial")) {
			for (int i = 0; i < AmazonTrain.mPlus1; i++) {

				BinCategory bc = (BinCategory) workerHistoryMapHolder.get(i);
				double qij = 0;
				if (bc == null) {
					/*
					 * Null values for training!! Our model is trained only on
					 * full tuples!!
					 */
					str += ",NH";
				} else {
					if (approach.equals("PE"))
						qij = getBinomialPointEstimate(bc.getX(), bc.getN());
					else if (approach.equals("RS"))
						qij = getDistroEstimate(bc.getX(), bc.getN());

					str += "," + getLogit(fix(qij));
				}
			}
		} else {
			for (int i = 0; i < AmazonTrain.mPlus1; i++) {

				MultCategory mc = (MultCategory) workerHistoryMapHolder.get(i);
				double qij = 0;
				if (mc == null) {
					/*
					 * Null values for training!! Our model is trained only on
					 * full tuples!!
					 */
					str += ",NH";
				} else {
					if (approach.equals("PE"))
						qij = getDirichletPointEstimate(mc.getQ_ijk());
					else if (approach.equals("RS"))
						qij = getDirichletDistroEstimate(mc
								.getBucketSuccesses());

					str += "," + getLogit(fix(qij));
				}
			}

		}
		str += "," + catId + "," + getLogit(fix(actualTaskScore));
		AmazonTrain.outputFile.writeToFile(str);
	}

	public void updateWorkerHistoryAndPrintTuple(
			HashMap<String, HashMap<Integer, ModelCategory>> dataMapHolder,
			String developerId, int catId, int bucket, double actualTaskScore,
			String approach, String model) {

		HashMap<Integer, ModelCategory> workerHistoryMapHolder = dataMapHolder
				.get(developerId);
		if (workerHistoryMapHolder == null) {
			workerHistoryMapHolder = new HashMap<Integer, ModelCategory>();
			MultCategory curTaskCat = new MultCategory();
			MultCategory overalCategory = new MultCategory();
			addTaskOutcomeToCategory(curTaskCat, bucket);
			addTaskOutcomeToCategory(overalCategory, bucket);

			workerHistoryMapHolder.put(0, overalCategory);
			workerHistoryMapHolder.put(catId, curTaskCat);

			dataMapHolder.put(developerId, workerHistoryMapHolder);
		} else {
			MultCategory currentTaslCat = (MultCategory) workerHistoryMapHolder
					.get(catId);
			MultCategory overalCategory = (MultCategory) workerHistoryMapHolder
					.get(0);
			if (overalCategory.getN() > Reputation.historyThr) {
				if (approach.equals("PE"))
					addNewTupleToTrainingFile(workerHistoryMapHolder,
							developerId, catId, actualTaskScore, approach,
							model);
				else {
					for (int i = 0; i < 30; i++)
						addNewTupleToTrainingFile(workerHistoryMapHolder,
								developerId, catId, actualTaskScore, approach,
								model);
				}
			}
			if (currentTaslCat == null) {
				currentTaslCat = new MultCategory();
				workerHistoryMapHolder.put(catId, currentTaslCat);
			}
			addTaskOutcomeToCategory(currentTaslCat, bucket);
			addTaskOutcomeToCategory(
					(MultCategory) workerHistoryMapHolder.get(0), bucket);
		}
	}

}
