package kokkodis.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.sf.doodleproject.numerics4j.random.BetaRandomVariable;
import net.sf.doodleproject.numerics4j.random.GammaRandomVariable;

import kokkodis.factory.BinCategory;
import kokkodis.factory.EvalWorker;
import kokkodis.factory.ModelCategory;
import kokkodis.factory.MultCategory;
import kokkodis.factory.RawInstance;
import kokkodis.utils.odesk.GlobalVariables;

public class Utils {

	/**
	 * For Binomial
	 * 
	 * @param modelCategory
	 * @param succesfulOutcome
	 */
	public void addTaskOutcomeToCategory(ModelCategory modelCategory,
			boolean succesfulOutcome) {

		if (succesfulOutcome) {
			((BinCategory) modelCategory).setX(((BinCategory) modelCategory)
					.getX() + 1);
			((BinCategory) modelCategory).setN(((BinCategory) modelCategory)
					.getN() + 1);
		} else {
			((BinCategory) modelCategory).setN(((BinCategory) modelCategory)
					.getN() + 1);
		}
	}

	/**
	 * For Multinomial
	 * 
	 * @param curTaskCat
	 * @param bucket
	 */
	public void addTaskOutcomeToCategory(MultCategory curTaskCat, int bucket) {
		curTaskCat.getBucketSuccesses()[bucket]++;
		curTaskCat.increaseTotalTrials();

	}

	public static double fix(double l) {
		if (l < 0.01)
			return 0.01;
		if (l > 0.99)
			return 0.99;
		return l;
	}

	public static double getLogit(double q) {
		return Math.log(q / (1 - q));

	}

	public void createDirs(File f) {
		if (!f.exists()) {
			System.out.println("Creating directory..:" + f);
			System.out.println("New dir.." + f.mkdir());
		}

	}

	public static double inverseLogit(double r) {
		double eTor = Math.exp(r);
		return eTor / (1 + eTor);
	}

	public static double getBinomialPointEstimate(double x, double n) {

		return (x + GlobalVariables.binomialPrior[0])
				/ (n + GlobalVariables.binomialPrior[0] + GlobalVariables.binomialPrior[1]);
	}

	public static double getDirichletPointEstimate(double[] q_ijk) {
		double sum = 0;

		for (int i = 0; i < q_ijk.length; i++) {
			sum += q_ijk[i] * GlobalVariables.qualities[i];
		}

		return sum;
	}

	public static double getDistroEstimate(double x, double n) {
		double alpha = x + GlobalVariables.binomialPrior[0];
		double beta = n - x + GlobalVariables.binomialPrior[1];
		double res = (new BetaRandomVariable(alpha, beta)).nextRandomVariable();
		// System.out.println("Res:"+res);
		return res;
	}

	/**
	 * 
	 * @param bucketSuccesses
	 * @return
	 */
	public static double getDirichletDistroEstimate(double[] bucketSuccesses) {

		// double [] prior = {4,1, 2, 2, 2,3, 2.5, 10,20};

		double[] newDistro = new double[(int) GlobalVariables.K];
		double sum = 0;
		for (int i = 0; i < bucketSuccesses.length; i++) {
			double alpha_k = bucketSuccesses[i]
					+ GlobalVariables.multinomialPrior[i];
			/**
			 * Assign zero probability to some events, in the case where they have zero priors and
			 * they haven't been observed up to that point.
			 */
			if (alpha_k == 0)
				newDistro[i] = 0;
			else
				newDistro[i] = new GammaRandomVariable(alpha_k, alpha_k)
						.nextRandomVariable();
			sum += newDistro[i];
		}
		for (int i = 0; i < newDistro.length; i++) {
			newDistro[i] = newDistro[i] / sum;
		}

		double de = getExpectation(newDistro);
		return de;
	}

	private static double getExpectation(double[] newDistro) {
		double res = 0;

		for (int i = 0; i < GlobalVariables.K; i++)
			res += newDistro[i] * GlobalVariables.qualities[i];
		return res;
	}

	public Integer getGenericCat(Integer catId) {
		if (catId == 1)
			return 1;
		else if (catId == 2)
			return 1;
		else if (catId == 3)
			return 2;
		else if (catId == 4)
			return 2;
		else if (catId == 5)
			return 2;
		/*
		 * For utility
		 */
		else if (catId == 20)
			return 1;
		else if (catId == 60)
			return 2;
		else if (catId == 80)
			return 1;
		else if (catId == 90)
			return 2;

		return 1;
	}

	public int adjustODeskCategory(String level, int catId) {

		if (level.equals("Technical")) {
			if (catId == 6)
				return 3;
			else
				return catId;
		}// Just to fit in the model with m=4;
		if (level.equals("Non-technical"))
			return catId - 2;
		else
			return getGenericCat(catId);
	}

	/**
	 * Returns bucket of the score.
	 * 
	 * @param actualTaskScore
	 * @return
	 */
	public static int getBucket(double actualTaskScore) {
		for (int i = 0; i < GlobalVariables.qualities.length; i++)
			if (actualTaskScore <= GlobalVariables.qualities[i])
				return i;

		return (int) (GlobalVariables.K) - 1;

	}

	public static String createFileName() {
		String res = GlobalVariables.curModel
				+ "_"
				+ GlobalVariables.curApproach
				+ (GlobalVariables.curCluster != null ? "_"
						+ GlobalVariables.curCluster : "")
				+ (GlobalVariables.curModel.equals("Binomial") ? "_"
						+ GlobalVariables.currentBinomialThreshold : "")
				+((GlobalVariables.currentFold!=null)?"_"+GlobalVariables.currentFold:"")+ ".csv";
		return res;
	}

	/**
	 * contractor,category,score "
	 */
	public static RawInstance stringToRawInstance(String line) {
		RawInstance ri = new RawInstance();
		String[] tmpAr = line.split(",");
		ri.setScore(Double.parseDouble(tmpAr[2].trim()) / 5);
		ri.setContractor(Integer.parseInt(tmpAr[0].trim()));
		ri.setCategory(Integer.parseInt(tmpAr[1].trim()));
		return ri;

	}

	public static int adjustCategory(int cat) {
		if (GlobalVariables.curCluster.equals("r")) {
			return adjustCategoryToRoot(cat);
		}
		return cat;
	}

	public static Integer adjustCategoryToRoot(int cat) {
		if (cat == 0)
			return 0;
		GlobalVariables globalVariables = GlobalVariables.getInstance();
	/*	String catName = globalVariables.getCatIntToName().get(cat);
		String cluster = globalVariables.getCategoriesToClusters().get(catName);
		String rootCategory = globalVariables.getClusterToRealName().get(
				cluster);
		return globalVariables.getCatNameToInt().get(rootCategory);
		*/
		return globalVariables.getCategoriesToRoot().get(cat);
				

	}

	public static ArrayList<Integer> getCurCatIds() {
		GlobalVariables globalVariables = GlobalVariables.getInstance();
		String[] curCategories = globalVariables.getClusterCategories().get(
				GlobalVariables.curCluster);

		return getIds(curCategories);
	}

	private static ArrayList<Integer> getIds(String[] curCategories) {
		GlobalVariables globalVariables = GlobalVariables.getInstance();

		ArrayList<Integer> res = new ArrayList<Integer>();
		for (int i = 0; i < curCategories.length; i++) {
			int catId = globalVariables.getCatNameToInt().get(curCategories[i]);
			res.add(catId);

		}

		Collections.sort(res);
		return res;

	}

	public static ArrayList<Integer> getCurCatIds(String cluster) {
		GlobalVariables globalVariables = GlobalVariables.getInstance();
		String[] curCategories = globalVariables.getClusterCategories().get(
				cluster);

		return getIds(curCategories);
	}

	/**
	 * Add outcome to modelcategory mc.
	 * 
	 * @param mc
	 *            : can be either BinCategory or MultCategory
	 * @param score
	 *            : actual feedback score
	 */
	public static void addTaskOutcomeToCategory(ModelCategory mc, double score) {
		if (GlobalVariables.curModel.equals("Binomial")) {

			if (score > GlobalVariables.currentBinomialThreshold) {
				((BinCategory) mc).setX(((BinCategory) mc).getX() + 1);
				((BinCategory) mc).setN(((BinCategory) mc).getN() + 1);
			} else {
				((BinCategory) mc).setN(((BinCategory) mc).getN() + 1);
			}
		} else {
			((MultCategory) mc).getBucketSuccesses()[Utils.getBucket(score)]++;
			((MultCategory) mc).increaseTotalTrials();
		}

	}

	public static void printEvalWorker(EvalWorker evalWorker) {
		try {
			System.out.println(GlobalVariables.line);
			System.out.println("Printing eval worker.");
			System.out.println("Worker type:" + evalWorker.getWorkerType());
			System.out.println("Generic Map overal size:"
					+ evalWorker.getGenericHistoryMap().get(0).getN());
			System.out.println("RR map overal size:"
					+ evalWorker.getTechnicalHistoryMap().get(0).getN());
			System.out.println("RL map overal size:"
					+ evalWorker.getNonTechHistoryMap().get(0).getN());
		} catch (NullPointerException ne) {
			System.err.println("At least one was null.");
		}

	}

	public static void printHelp() {
			String s = "-t		   	Train: crete regression files.>>"
					+ "-g 			Generate the necessary training files from the raw instances.  >>"
					+ "-e			Run evaluation. >>"
					+ "-w			Print results to files.  >>"
					+ "-cv			Run cross validation>>"
					+ "-f			Number of folds for cross validation.>>"
					+ "-s			Split files to -f number of folds. The spliting is by contractor.>>"
			;

			System.out.println("Available options:");
			System.out
					.println("------------------------------------------------------------------");
			for (String str : s.split(">>"))
				System.out.println(str);
			System.out
					.println("------------------------------------------------------------------");
		}

		

}
