package kokkodis.utils.odesk;

import java.util.HashMap;

import kokkodis.factory.BinCategory;
import kokkodis.factory.ModelCategory;
import kokkodis.factory.MultCategory;
import kokkodis.odesk.Reputation;
import kokkodis.utils.Utils;

public class TrainUtils extends Utils {

	public TrainUtils() {
	}

	/*
	 * Updates the history of the worker. Also if worker has enough history,
	 * includes the task with its outcome in the panel.
	 */
	public void updateWorkerHistoryAndPrintTuple(
			HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder,
			int developerId, Integer catId, boolean succesfullOutcome,
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
			//System.out.println(overalCategory.getN());
			
			if (overalCategory.getN() > Reputation.historyThr) {
				//System.out.println("history cool..");
				if (approach.equals("PE"))
					addNewTupleToTrainingFile(workerHistoryMapHolder,
							developerId, catId, actualTaskScore, approach,
							model);
				else {
					for (int i = 0; i < 20; i++)
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
			int developerId, Integer catId, double jobScore, String approach,
			String model) {
		String str = "" + developerId;

		if (model.equals("Binomial")) {
			for (int i = 0; i < Reputation.mPlus1; i++) {

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
			for (int i = 0; i < Reputation.mPlus1; i++) {

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
						qij = getDirichletDistroEstimate(
								mc.getBucketSuccesses());

					str += "," + getLogit(fix(qij));
				}
			}

		}
		str += "," + catId + "," + getLogit(fix(jobScore));
	//	System.out.println("Adding tupple:"+str);
		Reputation.outputFile.writeToFile(str);

	}


	/**
	 * This is for multinomial!!!
	 * 
	 * @param dataMapHolder
	 * @param developerId
	 * @param catId
	 * @param bucket
	 * @param actualTaskScore
	 * @param approach
	 */
	public void updateWorkerHistoryAndPrintTuple(
			HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder,
			int developerId, Integer catId, int bucket, double actualTaskScore,
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
					for (int i = 0; i <20; i++)
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
