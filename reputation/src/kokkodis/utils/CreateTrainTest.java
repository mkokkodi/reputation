package kokkodis.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import kokkodis.factory.BinCategory;
import kokkodis.factory.ModelCategory;
import kokkodis.factory.MultCategory;
import kokkodis.factory.PropertiesFactory;
import kokkodis.factory.RawInstance;

public class CreateTrainTest {

	private static GlobalVariables globalVariables;
	private static String inputDirectory;

	public static void generateTrainTestSets() {
		globalVariables = GlobalVariables.getInstance();
		String trainFile = "train.csv";
		if (GlobalVariables.synthetic) {

			System.out.println("Running on synthetic data...");
			int categories = globalVariables.getClusterCategories().get("r").length - 1;
			trainFile = "syn_train_cat" + categories + ".csv";
		} else if (GlobalVariables.syntheticCluster) {

			System.out.println("Running on synthetic clusters of data...");
			trainFile = "syn_cluster_train.csv";

		}
		initInput("rawPath", trainFile);
		initAndRun("trainingOutPath");

	}

	/**
	 * 
	 * @param inputVar
	 *            the input path variable of config.properties
	 */
	private static void initAndRun(String outputVar) {

		String fileName = Utils.createFileName();

		System.out.println(GlobalVariables.line);
		globalVariables.openFile(PropertiesFactory.getInstance().getProps()
				.getProperty(outputVar)
				+ fileName);

		System.out.println("Current output filename:/" + fileName);
		String header = "id";
		for (String cat : globalVariables.getClusterCategories().get(
				GlobalVariables.curCluster))
			header += ",logit(" + cat + ")";
		header += ",cat,logit(q_cat(t+1))";
		globalVariables.getOutputFile().writeToFile(header);

		createSet();
		globalVariables.getOutputFile().closeFile();

	}

	private static void initInput(String inputVar, String inputFile) {

		inputDirectory = PropertiesFactory.getInstance().getProps()
				.getProperty(inputVar)
				+ inputFile;

	}

	/**
	 */
	private static void createSet() {

		HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder = new HashMap<Integer, HashMap<Integer, ModelCategory>>();
		try {
			BufferedReader input = new BufferedReader(new FileReader(
					inputDirectory));
			String line;
			System.out.println("Reading from file:" + inputDirectory);
			while ((line = input.readLine()).contains("#")) {
			}

			/**
			 * contractor,category,score "
			 */
			while ((line = input.readLine()) != null) {
				RawInstance ri = Utils.stringToRawInstance(line);

				if (catInCluster(ri.getCategory())
						|| (GlobalVariables.curCluster.equals("r") && GlobalVariables.hierarchicalFlag)) {
					/*
					 * adjust category changes cat only if cat in root.
					 */

					{
						ri.setCategory(Utils.adjustCategory(ri.getCategory()));
					}
					updateWorkerHistoryAndPrintTuple(dataMapHolder,
							ri.getContractor(), ri.getCategory(), ri.getScore());
				}
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static boolean catInCluster(int cat) {
		String catCluster = globalVariables.getCategoriesToClusters().get(
				globalVariables.getCatIntToName().get(cat));
		if (catCluster.equals(GlobalVariables.curCluster))
			return true;
		return false;
	}

	/**
	 * 
	 * @param dataMapHolder
	 *            : Keeps history of every contractor
	 * @param contractor
	 * @param cat
	 * @param score
	 */
	private static void updateWorkerHistoryAndPrintTuple(
			HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder,
			int contractor, int cat, double score) {

		HashMap<Integer, ModelCategory> workerHistoryMapHolder = dataMapHolder
				.get(contractor);
		ModelCategory curTaskCat;
		ModelCategory overalCategory;

		if (workerHistoryMapHolder == null) {
			workerHistoryMapHolder = new HashMap<Integer, ModelCategory>();
			curTaskCat = initCategory(score);
			overalCategory = initCategory(score);
			workerHistoryMapHolder.put(0, overalCategory);
			workerHistoryMapHolder.put(cat, curTaskCat);

			dataMapHolder.put(contractor, workerHistoryMapHolder);
		} else {
			curTaskCat = workerHistoryMapHolder.get(cat);
			overalCategory = workerHistoryMapHolder.get(0);
			// System.out.println(overalCategory.getN());

			if (overalCategory.getN() > GlobalVariables.getGlobaleVars()
					.getHistoryThr()) {
				// System.out.println("history cool..");
				if (GlobalVariables.curApproach.equals("PE"))
					addNewTupleToTrainingFile(workerHistoryMapHolder,
							contractor, cat, score);
				else {
					for (int i = 0; i < GlobalVariables.rsTrials; i++)
						addNewTupleToTrainingFile(workerHistoryMapHolder,
								contractor, cat, score);
				}
			}
			if (curTaskCat == null) {
				if (GlobalVariables.curModel.equals("Binomial"))
					curTaskCat = new BinCategory();
				else
					curTaskCat = new MultCategory();
				workerHistoryMapHolder.put(cat, curTaskCat);
			}
			Utils.addTaskOutcomeToCategory(curTaskCat, score);
			Utils.addTaskOutcomeToCategory(overalCategory, score);
		}

	}

	/**
	 * 
	 * @param workerHistoryMapHolder
	 *            : keeps outcomes of all categories for the contractor
	 * @param contractor
	 * @param cat
	 * @param score
	 */
	private static void addNewTupleToTrainingFile(
			HashMap<Integer, ModelCategory> workerHistoryMapHolder,
			int contractor, int cat, double score) {
		String str = "" + contractor;

		for (String curCat : globalVariables.getClusterCategories().get(
				GlobalVariables.curCluster)) {
			int curCatId = globalVariables.getCatNameToInt().get(curCat);

			if (GlobalVariables.curModel.equals("Binomial")) {
				str += getBinomialLine(workerHistoryMapHolder, curCatId);

			} else {

				str += getMultinomialLine(workerHistoryMapHolder, curCatId);

			}

		}
		str += "," + cat + "," + Utils.getLogit(Utils.fix(score));
		// System.out.println("Adding tupple:"+str);
		globalVariables.getOutputFile().writeToFile(str);

	}

	/**
	 * 
	 * @param workerHistoryMapHolder
	 * @param catId
	 * @return
	 */
	private static String getMultinomialLine(
			HashMap<Integer, ModelCategory> workerHistoryMapHolder, int catId) {
		String str = "";
		MultCategory mc = (MultCategory) workerHistoryMapHolder.get(catId);
		double qij = 0;
		if (mc == null) {
			/*
			 * Null values for training!! Our model is trained only on full
			 * tuples!!
			 */
			str += ",NH";
		} else {
			if (GlobalVariables.curApproach.equals("PE"))
				qij = Utils.getDirichletPointEstimate(mc.getQ_ijk());
			else
				qij = Utils.getDirichletDistroEstimate(mc.getBucketSuccesses());

			str += "," + Utils.getLogit(Utils.fix(qij));
		}
		return str;
	}

	/**
	 * 
	 * @param workerHistoryMapHolder
	 * @param catId
	 * @return
	 */
	private static String getBinomialLine(
			HashMap<Integer, ModelCategory> workerHistoryMapHolder, int catId) {

		String str = "";
		BinCategory bc = (BinCategory) workerHistoryMapHolder.get(catId);
		double qij = 0;
		if (bc == null) {
			/*
			 * Null values for training!! Our model is trained only on full
			 * tuples!!
			 */
			str += ",NH";
		} else {
			if (GlobalVariables.curApproach.equals("PE"))
				qij = Utils.getBinomialPointEstimate(bc.getX(), bc.getN());
			else if (GlobalVariables.curApproach.equals("RS"))
				qij = Utils.getDistroEstimate(bc.getX(), bc.getN());

			str += "," + Utils.getLogit(Utils.fix(qij));
		}
		return str;
	}

	/**
	 * Initializes Bin or Mult category. It also adds the outcome result to the
	 * respective category.
	 * 
	 * @param score
	 * @return
	 */
	private static ModelCategory initCategory(double score) {
		ModelCategory mc;
		if (GlobalVariables.curModel.equals("Binomial"))
			mc = new BinCategory();
		else
			mc = new MultCategory();

		Utils.addTaskOutcomeToCategory(mc, score);

		return mc;

	}

	public static void createDeveloperSets(int i) {
		initInput("rawPath", "allData.csv");
		System.out.println("Reading from directory:" + inputDirectory);
		String outDir = PropertiesFactory.getInstance().getProps()
				.getProperty("rawPath");
		globalVariables.openFile(outDir + "train" + i + ".csv");

		PrintToFile testOut = new PrintToFile();
		testOut.openFile(outDir + "test" + i + ".csv");
		try {
			BufferedReader input = new BufferedReader(new FileReader(
					inputDirectory));
			String line;
			line = input.readLine();

			globalVariables.getOutputFile().writeToFile(line);
			/**
			 * contractor,category,score "
			 */
			while ((line = input.readLine()) != null) {
				String[] tmpAr = line.split(",");
				int mod = Integer.parseInt(tmpAr[0].trim())
						% (GlobalVariables.folds);
				if ((mod == i) || (mod == 0 && i == GlobalVariables.folds)) {
					testOut.writeToFile(line);
				} else {
					globalVariables.getOutputFile().writeToFile(line);
				}
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		globalVariables.getOutputFile().closeFile();
		testOut.closeFile();

	}

}
