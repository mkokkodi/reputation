package kokkodis.synthetic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

import kokkodis.factory.EvalWorker;
import kokkodis.factory.EvalWorkerSynthetic;
import kokkodis.factory.ModelCategory;
import kokkodis.odesk.Reputation;
import kokkodis.utils.odesk.TestUtils;
import kokkodis.utils.odesk.TrainUtils;

public class Utils {

	public Utils() {
		// TODO Auto-generated constructor stub
	}

	public void rawDataToBinomialModel(String approach, String model,
			int categories, boolean test) {

		if (!test) {
			
			System.out.println("Approch:" + approach);
		}
		HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder = null;

		HashMap<Integer, EvalWorker> dataMapHolderEval = null;

		if (!test) {

			dataMapHolder = new HashMap<Integer, HashMap<Integer, ModelCategory>>();

			manipulateDataTrain(approach, model, dataMapHolder);

		} else {
			dataMapHolderEval = new HashMap<Integer, EvalWorker>();
			manipulateData(approach, model, dataMapHolderEval);
		}

	}

	/**
	 * This is for evaluating
	 * 
	 * @param approach
	 * @param model
	 * @param dataMapHolderEval
	 * @param categories
	 */

	protected void manipulateData(String approach, String model,
			HashMap<Integer, EvalWorker> dataMapHolderEval) {

		TestUtils tu = new TestUtils();
		try {

			BufferedReader input = new BufferedReader(new FileReader(new File(
					"/Users/mkokkodi/Desktop/bigFiles/kdd/synthetic/rawData/RS/clustered3test9"
							+".csv")));
			String line;

			while ((line = input.readLine()).contains("#"))
				;
			if (model.equals("Binomial")) {

				while ((line = input.readLine()) != null) {
					String[] tmpAr = line.split(",");

					int catId = Integer.parseInt(tmpAr[2].trim());

					int developerId = Integer.parseInt(tmpAr[0].trim());

					double actualTaskScore = Double
							.parseDouble(tmpAr[3].trim());

					boolean succesfullOutcome = ((actualTaskScore > Reputation.scoreTh)) ? true
							: false;
					EvalWorker tmp = dataMapHolderEval.get(developerId);
					if (tmp == null) {
						tmp = new EvalWorker();
					}
					tu.updateEvalWorker(dataMapHolderEval, developerId, catId,
							succesfullOutcome, actualTaskScore, approach, model);
				}
			}

			else if (model.equals("Multinomial")) {
				while ((line = input.readLine()) != null) {

					String[] tmpAr = line.split(",");

					int catId = Integer.parseInt(tmpAr[2].trim());

					int developerId = Integer.parseInt(tmpAr[0].trim());

					double actualTaskScore = Double
							.parseDouble(tmpAr[3].trim());

					int bucket = tu.getBucket(actualTaskScore);

					EvalWorker tmp = dataMapHolderEval.get(developerId);
					if (tmp == null) {
						tmp = new EvalWorker();
					}

					tu.updateEvalWorker(dataMapHolderEval, developerId, catId,
							bucket, actualTaskScore, approach, model);

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This is for training!
	 * 
	 * @param approach
	 * @param model
	 * @param dataMapHolder
	 * @throws SQLException
	 */

	protected void manipulateDataTrain(String approach, String model,
			HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder) {

		TrainUtils tu = new TrainUtils();
		try {

			BufferedReader input = new BufferedReader(new FileReader(new File(
					DataGenerationHierarchy.rawData + "train"
							+ SyntheticTrain.categories + ".csv")));
			System.out.println("Reading file:"+DataGenerationHierarchy.rawData + "train"
					+ SyntheticTrain.categories + ".csv");
			String line;

			while ((line = input.readLine()).contains("#"))
				;
			if (model.equals("Binomial")) {

				while ((line = input.readLine()) != null) {
					String[] tmpAr = line.split(",");

					int catId = Integer.parseInt(tmpAr[2].trim());

					int developerId = Integer.parseInt(tmpAr[0].trim());

					double actualTaskScore = Double
							.parseDouble(tmpAr[3].trim());

					boolean succesfullOutcome = ((actualTaskScore > Reputation.scoreTh)) ? true
							: false;

					tu.updateWorkerHistoryAndPrintTuple(dataMapHolder,
							developerId, catId, succesfullOutcome,
							actualTaskScore, approach, model);
				}
			} else {
				System.out.println("Multinomial!");

				while ((line = input.readLine()) != null) {

					String[] tmpAr = line.split(",");

					int catId = Integer.parseInt(tmpAr[2].trim());

					int developerId = Integer.parseInt(tmpAr[0].trim());

					double actualTaskScore = Double
							.parseDouble(tmpAr[3].trim());

					int bucket = tu.getBucket(actualTaskScore);

					tu.updateWorkerHistoryAndPrintTuple(dataMapHolder,
							developerId, catId, bucket, actualTaskScore,
							approach, model);
				}
			}

		} catch (IOException e) {
		}
	}

	public void rawDataToBinomialModelHier(String approach, String model,
			int categories, boolean test, int cluster) {

		if (!test) {
			System.out.println("Reading file.");
			System.out.println("Approch:" + approach);
			System.out.println("Cluster:" + cluster);
		}
		HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder = null;

		HashMap<Integer, EvalWorkerSynthetic> dataMapHolderEval = null;

		HashSet<Integer> cluster0cats = new HashSet<Integer>();
		cluster0cats.add(1);
		cluster0cats.add(2);
		cluster0cats.add(3);
		HashSet<Integer> cluster1cats = new HashSet<Integer>();
		cluster1cats.add(4);
		cluster1cats.add(5);
		cluster1cats.add(6);

		HashSet<Integer> cluster2cats = new HashSet<Integer>();
		cluster2cats.add(7);
		cluster2cats.add(8);
		cluster2cats.add(9);

		if (!test) {

			dataMapHolder = new HashMap<Integer, HashMap<Integer, ModelCategory>>();

			if (cluster == 0) {
				// System.out.println("cluster 0");
				manipulateData(approach, model, dataMapHolder, cluster,
						cluster0cats);
			} else if (cluster == 1) {
				// System.out.println("cluster 1");
				manipulateData(approach, model, dataMapHolder, cluster,
						cluster1cats);

			} else if (cluster == 2) {
				manipulateData(approach, model, dataMapHolder, cluster,
						cluster2cats);
			} else {
				manipulateData(approach, model, dataMapHolder, cluster, null);
			}
		} else {
			dataMapHolderEval = new HashMap<Integer, EvalWorkerSynthetic>();
			manipulateDataHier(approach, model, dataMapHolderEval);
		}

	}

	private void manipulateDataHier(String approach, String model,
			HashMap<Integer, EvalWorkerSynthetic> dataMapHolderEval) {

		TestUtils tu = new TestUtils();
		try {

			BufferedReader input = new BufferedReader(new FileReader(new File(
					DataGenerationHierarchy.rawData + "test"
							+ SyntheticTestHier.globalCategories
							+ ".csv")));
			String line;

			while ((line = input.readLine()).contains("#"))
				;
			if (model.equals("Binomial")) {

				while ((line = input.readLine()) != null) {
					
					String[] tmpAr = line.split(",");

					int catId = Integer.parseInt(tmpAr[2].trim());

					int developerId = Integer.parseInt(tmpAr[0].trim());

					double actualTaskScore = Double
							.parseDouble(tmpAr[3].trim());

					boolean succesfullOutcome = ((actualTaskScore > Reputation.scoreTh)) ? true
							: false;
					EvalWorkerSynthetic tmp = dataMapHolderEval.get(developerId);
					int cluster = tu.getCluster(catId);

					String workerType = null;
					if (tmp == null) {
						tmp = new EvalWorkerSynthetic();
						workerType = "" + cluster;
					} else {

						workerType = tmp.getWorkerType();
					}

					catId = adjustGenericCatToCluster(cluster, catId);

					tu.updateEvalWorkerSyntheticHier(dataMapHolderEval,
							developerId, catId, succesfullOutcome,
							actualTaskScore, approach, workerType, cluster,
							model);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void manipulateData(String approach, String model,
			HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder,
			int cluster, HashSet<Integer> clusterCats) {

		System.out.println("Manipulating data/");

		TrainUtils tu = new TrainUtils();
		try {

			BufferedReader input = new BufferedReader(new FileReader(new File(
					DataGenerationHierarchy.rawData + "train"
							+ SyntheticTrainHierarchical.globalCategories
							+ ".csv")));
			String line;

			System.out.println("File:" + DataGenerationHierarchy.rawData
					+ "train" + SyntheticTrainHierarchical.globalCategories
					+ ".csv");
			while ((line = input.readLine()).contains("#"))
				;
			if (model.equals("Binomial")) {

				int index = 0;
				while ((line = input.readLine()) != null) {
					String[] tmpAr = line.split(",");

					int catId = Integer.parseInt(tmpAr[2].trim());
					// if ( catId >6)
					// System.out.println("exist");
					if (clusterCats == null || clusterCats.contains(catId)) {
						index++;
						// System.out.println(line);
						catId = adjustGenericCatToCluster(cluster, catId);
						int developerId = Integer.parseInt(tmpAr[0].trim());

						double actualTaskScore = Double.parseDouble(tmpAr[3]
								.trim());
						boolean succesfullOutcome = ((actualTaskScore > Reputation.scoreTh)) ? true
								: false;

						tu.updateWorkerHistoryAndPrintTuple(dataMapHolder,
								developerId, catId, succesfullOutcome,
								actualTaskScore, approach, model);

					}

				}
				System.out.println("Total:" + index);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private int adjustGenericCatToCluster(int cluster, int catId) {
		if (cluster != 3) {
			
			return ((catId - 1) % 3 + 1);
		} else
			return ((catId - 1) / 3) + 1;
	}

}