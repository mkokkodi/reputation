package kokkodis.db;

import java.io.File;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import kokkodis.factory.EvalWorker;
import kokkodis.factory.ModelCategory;
import kokkodis.odesk.Reputation;
import kokkodis.odesk.cv.Train;
import kokkodis.utils.PrintToFile;
import kokkodis.utils.odesk.TestUtils;
import kokkodis.utils.odesk.TrainUtils;

public class MySQLoDeskQueries extends Queries {

	public void connect() {
		try {
			dbname = "tagsdb";
			// Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			Properties props = new Properties();
			props.put("user", username);
			props.put("password", password);
			props.put("databaseName", dbname);
			conn = DriverManager
					.getConnection("jdbc:mysql://128.122.201.104/odesk?user=root&password=11r88a4m");
			/*
			 * conn = DriverManager.getConnection(
			 * "jdbc:sqlserver://hyperion.stern.nyu.edu:1433" //
			 * "jdbc:sqlserver://localhost:2000;" , props);
			 */
		} catch (SQLException e) {
			System.err.println("SQLException: " + e.getMessage());
		} catch (java.lang.ClassNotFoundException e) {
			System.err.print("ClassNotFoundException: ");
			System.err.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private HashMap<Integer, Integer> mapCategories() {
		HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
		try {

			String selectString = "select catId,level1 "
					+ "from `odesk`.`catMapping` ";
			PreparedStatement prepStmt = conn.prepareStatement(selectString);
			prepStmt.execute();
			ResultSet rs = prepStmt.getResultSet();
			while (rs.next()) {
				hm.put(rs.getInt("catId"), rs.getInt("level1"));
			}
		} catch (SQLException e) {

			e.printStackTrace();

		}
		return hm;
	}

	public void rawDataToBinomialModel(String level, String approach,
			String model, boolean test) {

		if (!test) {
			System.out.println("Quering DB.");
			System.out.println("Level:" + level);
			System.out.println("Approch:" + approach);
		}
		HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder = null;

		HashMap<Integer, EvalWorker> dataMapHolderEval = null;

		HashMap<Integer, Integer> cats = null; //mapCategories();

		HashSet<Integer> techCats = new HashSet<Integer>();
		techCats.add(1);
		techCats.add(2);
		techCats.add(6);

		HashSet<Integer> nonTechCats = new HashSet<Integer>();
		nonTechCats.add(3);
		nonTechCats.add(4);
		nonTechCats.add(5);

		try {

			String selectString;
			if (!test) {

				dataMapHolder = new HashMap<Integer, HashMap<Integer, ModelCategory>>();
			/*	selectString = "select developer,jobCategory, "
						+ "(availability+communication+cooperation+deadlines+quality+skills+total)/7 as score "
						+ "from `odesk`.`train`   order by date";
						*/
				selectString = "select  contractor,category, " +
						"(availability+communication+cooperation+deadlines+quality+skills+total)/7 as score  " +
						"from mkokkodi.train_utility where category in (40) order by job_date ";
				
			} else {
				dataMapHolderEval = new HashMap<Integer, EvalWorker>();
				selectString = "select  contractor,category, " +
						"(availability+communication+cooperation+deadlines+quality+skills+total)/7 as score  " +
						"from mkokkodi.test_utility where category in (40) order by job_date ";
		
		/*		selectString = "select developer,jobCategory,"
						+ "(availability+communication+cooperation+deadlines+quality+skills+total)/7 as score "
						+ "from `odesk`.`test`   order by date";
						*/
						
			}
		/*	PreparedStatement prepStmt = conn.prepareStatement(selectString);
			prepStmt.execute();
			ResultSet rs = prepStmt.getResultSet();
			*/
			ConnectionFactory conn = ConnectionFactory.getInstance();
			ResultSet rs = conn.getResultSet(selectString);
			if (level.equals("Technical")) {

				manipulateData(rs, approach, model, cats, dataMapHolder, level,
						techCats);

			} else if (level.equals("Non-technical")) {

				manipulateData(rs, approach, model, cats, dataMapHolder, level,
						nonTechCats);
			} else if (level.equals("Generic")) {
				manipulateData(rs, approach, model, cats, dataMapHolder, level,
						null);
			} else if (test) {
				manipulateData(rs, approach, model, cats, dataMapHolderEval);
			}

		} catch (SQLException e) {

			e.printStackTrace();

		}

	}

	/**
	 * This is for evaluating
	 * 
	 * @param rs
	 * @param approach
	 * @param model
	 * @param cats
	 * @param dataMapHolderEval
	 * @throws SQLException
	 */

	protected void manipulateData (ResultSet rs, String approach, String model,
			HashMap<Integer, Integer> cats,
			HashMap<Integer, EvalWorker> dataMapHolderEval) throws SQLException {

		TestUtils tu = new TestUtils();
		if (model.equals("Binomial")) {
			while (rs.next()) {
				//Integer catId = cats.get(rs.getInt("jobCategory"));
				Integer catId = rs.getInt("category");
				if (catId != null) {
					//int developerId = rs.getInt("developer");
					int developerId = rs.getInt("contractor");

					double score = rs.getDouble("score");
					double actualTaskScore = (score / 5.0);

					boolean succesfullOutcome = ((actualTaskScore > Reputation.scoreTh)) ? true
							: false;

					EvalWorker tmp = dataMapHolderEval.get(developerId);
			//		String currentTask = (tu.getGenericCat(catId) == 1) ? "Technical"
				//			: "Non-technical";
		//			String workerType = null;
					if (tmp == null) {
						tmp = new EvalWorker();
			//			workerType = currentTask;
					} else {

				//		workerType = tmp.getWorkerType();
					}
			//		catId = tu.adjustODeskCategory(workerType, catId);
					
					catId = tu.adjustODeskCategory("Generic", catId);
					
					 
					
					tu.updateEvalWorker(dataMapHolderEval, developerId, catId,
							succesfullOutcome, actualTaskScore, approach,null,null,model);
							//workerType, currentTask, model);
				}
			}
		} else if (model.equals("Multinomial")) {
			while (rs.next()) {
				Integer catId = cats.get(rs.getInt("jobCategory"));

				if (catId != null) {

					int developerId = rs.getInt("developer");

					double score = rs.getDouble("score");
					double actualTaskScore = (score / 5.0);
					int bucket = tu.getBucket(actualTaskScore);

					EvalWorker tmp = dataMapHolderEval.get(developerId);
					String currentTask = (tu.getGenericCat(catId) == 1) ? "Technical"
							: "Non-technical";
					String workerType = null;
					if (tmp == null) {
						tmp = new EvalWorker();
						workerType = currentTask;
					} else {

						workerType = tmp.getWorkerType();
					}
					catId = tu.adjustODeskCategory(workerType, catId);

					tu.updateEvalWorker(dataMapHolderEval, developerId, catId,
							bucket, actualTaskScore, approach, workerType,
							currentTask, model);
				}

			}

		}
	}

	/**
	 * This is for training!
	 * 
	 * @param rs
	 * @param approach
	 * @param model
	 * @param cats
	 *            Categories mapping
	 * @param dataMapHolder
	 * @param level
	 *            Technical, nontechnical or generic
	 * @param catMapping
	 *            hashmap for adjusting levels
	 * @throws SQLException
	 */

	protected void manipulateData(ResultSet rs, String approach, String model,
			HashMap<Integer, Integer> cats,
			HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder,
			String level, HashSet<Integer> catMapping) throws SQLException {

		TrainUtils tu = new TrainUtils();
		if (model.equals("Binomial")) {
			while (rs.next()) {
				//Integer catId = cats.get(rs.getInt("jobCategory"));

				Integer catId = rs.getInt("category");

				if (catId != null) {

				//	if (catMapping == null || catMapping.contains(catId)) {

						catId = tu.adjustODeskCategory(level, catId);

					//int developerId = rs.getInt("developer");

						int developerId = rs.getInt("contractor");

						double score = rs.getDouble("score");
						double actualTaskScore = (score / 5.0);

						boolean succesfullOutcome = ((actualTaskScore > Reputation.scoreTh)) ? true
								: false;

						tu.updateWorkerHistoryAndPrintTuple(dataMapHolder,
								developerId, catId, succesfullOutcome,
								actualTaskScore, approach, model);
					}
			//	}
			}
		} else {
			System.out.println("Multinomial!");
			while (rs.next()) {
				Integer catId = cats.get(rs.getInt("jobCategory"));

				if (catId != null) {

					if (catMapping == null || catMapping.contains(catId)) {

						catId = tu.adjustODeskCategory(level, catId);

						int developerId = rs.getInt("developer");

						double score = rs.getDouble("score");
						double actualTaskScore = (score / 5.0);

						int bucket = tu.getBucket(actualTaskScore);

						tu.updateWorkerHistoryAndPrintTuple(dataMapHolder,
								developerId, catId, bucket, actualTaskScore,
								approach, model);
					}
				}

			}
		}
	}

	public static void main(String[] args) {
		MySQLoDeskQueries q = new MySQLoDeskQueries();
		q.connect();
		q.createCVtable(10);
	}

	public ResultSet getResultSet(String selectString) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(selectString);
		stmt.execute();
		return stmt.getResultSet();
	}

	private void createCVtable(int folds) {
		String selectString = "Select distinct(developer) from "
				+ "odesk.testtrain_atleast6";
		try {
			ResultSet rs = getResultSet(selectString);
			HashMap<Integer, PrintToFile> hm = new HashMap<Integer, PrintToFile>();
			for (int i = 1; i <= folds; i++) {
				PrintToFile pf = new PrintToFile();
				pf.openFile(new File(
						"/Users/mkokkodi/git/kdd12/cv_data/developers/set" + i));
				hm.put(i, pf);
			}
			while (rs.next()) {
				double rnd = Math.random();
				if (rnd <= 0.1) {
					hm.get(1).writeToFile(rs.getString("developer"));
				} else if (rnd <= 0.2) {
					hm.get(2).writeToFile(rs.getString("developer"));
				} else if (rnd <= 0.3) {
					hm.get(3).writeToFile(rs.getString("developer"));
				} else if (rnd <= 0.4) {
					hm.get(4).writeToFile(rs.getString("developer"));
				} else if (rnd <= 0.5) {
					hm.get(5).writeToFile(rs.getString("developer"));
				} else if (rnd <= 0.6) {
					hm.get(6).writeToFile(rs.getString("developer"));
				} else if (rnd <= 0.7) {
					hm.get(7).writeToFile(rs.getString("developer"));
				} else if (rnd <= 0.8) {
					hm.get(8).writeToFile(rs.getString("developer"));
				} else if (rnd <= 0.9) {
					hm.get(9).writeToFile(rs.getString("developer"));
				} else {
					hm.get(10).writeToFile(rs.getString("developer"));
				}
			}
			for (int i = 1; i <= folds; i++) {
				hm.get(i).closeFile();
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void printTransitions() {
		try {
			int[][] transitions = new int[6][6];
			HashMap<Integer, Integer> cats = mapCategories();
			HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
			String selectString;

			selectString = "select developer,jobCategory "

			+ "from `odesk`.`train`   order by date";

			PreparedStatement prepStmt = conn.prepareStatement(selectString);
			prepStmt.execute();
			ResultSet rs = prepStmt.getResultSet();
			while (rs.next()) {
				int developerId = rs.getInt("developer");
				Integer catId = cats.get(rs.getInt("jobCategory"));
				if (catId != null) {
					catId--;
					Integer prevCat = hm.get(developerId);
					if (prevCat == null)
						hm.put(developerId, catId);
					else {
						transitions[prevCat][catId]++;
						hm.put(developerId, catId);
					}
				}
			}

			for (int i = 0; i < 6; i++) {
				double linesum = 0;
				for (int j = 0; j < 6; j++) {
					linesum += transitions[i][j];

				}
				for (int j = 0; j < 6; j++) {
					DecimalFormat myFormatter = new DecimalFormat("#.###");
					String output = myFormatter
							.format((double) transitions[i][j] / linesum);
					System.out.print(output + " & ");

				}

				System.out.println("//");
			}

		} catch (SQLException e) {

			e.printStackTrace();

		}
	}

	public void rawDataToBinomialModelCV(String level, String approach,
			String model, boolean test) {

		System.out.println("Quering DB.");
		System.out.println("Level:" + level);
		System.out.println("Approch:" + approach);
		System.out.println("Cross Validation");

		HashMap<Integer, Integer> cats = mapCategories();

		HashSet<Integer> techCats = new HashSet<Integer>();
		techCats.add(1);
		techCats.add(2);
		techCats.add(6);

		HashSet<Integer> nonTechCats = new HashSet<Integer>();
		nonTechCats.add(3);
		nonTechCats.add(4);
		nonTechCats.add(5);

		try {

			String selectString;

			selectString = "select developer,jobCategory, "
					+ "(availability+communication+cooperation+deadlines+quality+skills+total)/7 as score "
					+ "from `odesk`.`testtrain_atleast6`   order by date";

			for (int fold = 1; fold < 11; fold++) {
				ResultSet rs = getResultSet(selectString);
				System.out.println("Running Fold " + fold);

				runOnFold(level, approach, model, fold, cats, techCats,
						nonTechCats, test, rs);
			}

		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private void runOnFold(String level, String approach, String model,
			int fold, HashMap<Integer, Integer> cats,
			HashSet<Integer> techCats, HashSet<Integer> nonTechCats,
			boolean test, ResultSet rs) {

		HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder = null;

		HashMap<Integer, EvalWorker> dataMapHolderEval = null;
		dataMapHolder = new HashMap<Integer, HashMap<Integer, ModelCategory>>();
		dataMapHolderEval = new HashMap<Integer, EvalWorker>();
		if (!test) {
			System.out.println("Choosing file " + fold);
			Reputation.outputFile = Train.outputFiles.get(fold);
		}
		try {

			if (level.equals("Technical")) {

				manipulateData(rs, approach, model, cats, dataMapHolder, level,
						techCats, fold);

			} else if (level.equals("Non-technical")) {

				manipulateData(rs, approach, model, cats, dataMapHolder, level,
						nonTechCats, fold);
			} else if (level.equals("Generic")) {
				manipulateData(rs, approach, model, cats, dataMapHolder, level,
						null, fold);
			} else if (test) {
				manipulateData(rs, approach, model, cats, dataMapHolderEval,
						fold);
			}
		} catch (SQLException se) {
			se.printStackTrace();
		}

	}

	private void manipulateData(ResultSet rs, String approach, String model,
			HashMap<Integer, Integer> cats,
			HashMap<Integer, HashMap<Integer, ModelCategory>> dataMapHolder,
			String level, HashSet<Integer> catMapping, int fold)
			throws SQLException {

		TrainUtils tu = new TrainUtils();
		if (model.equals("Binomial")) {
			while (rs.next()) {

				int developerId = rs.getInt("developer");

				Integer tmpInt = Train.developerToSet.get(developerId);
				if (tmpInt != null && tmpInt != fold) {
					// System.out.println(tmpInt);
					Integer catId = cats.get(rs.getInt("jobCategory"));
					if (catId != null) {

						if (catMapping == null || catMapping.contains(catId)) {

							catId = tu.adjustODeskCategory(level, catId);

							double score = rs.getDouble("score");
							double actualTaskScore = (score / 5.0);

							boolean succesfullOutcome = ((actualTaskScore > Reputation.scoreTh)) ? true
									: false;

							tu.updateWorkerHistoryAndPrintTuple(dataMapHolder,
									developerId, catId, succesfullOutcome,
									actualTaskScore, approach, model);
						}
					}
				}
			}
		} else {
			System.out.println("Multinomial!");
			while (rs.next()) {
				int developerId = rs.getInt("developer");
				Integer tmpInt = Train.developerToSet.get(developerId);
				if (tmpInt != null && tmpInt != fold) {
					Integer catId = cats.get(rs.getInt("jobCategory"));

					if (catId != null) {

						if (catMapping == null || catMapping.contains(catId)) {

							catId = tu.adjustODeskCategory(level, catId);

							double score = rs.getDouble("score");
							double actualTaskScore = (score / 5.0);

							int bucket = tu.getBucket(actualTaskScore);

							tu.updateWorkerHistoryAndPrintTuple(dataMapHolder,
									developerId, catId, bucket,
									actualTaskScore, approach, model);
						}
					}

				}
			}
		}
	}

	protected void manipulateData(ResultSet rs, String approach, String model,
			HashMap<Integer, Integer> cats,
			HashMap<Integer, EvalWorker> dataMapHolderEval, int fold)
			throws SQLException {

		TestUtils tu = new TestUtils();
		if (model.equals("Binomial")) {
			while (rs.next()) {
				int developerId = rs.getInt("developer");

				Integer tmpInt = Train.developerToSet.get(developerId);
				if (tmpInt != null && tmpInt == fold) {

					Integer catId = cats.get(rs.getInt("jobCategory"));

					if (catId != null) {

						double score = rs.getDouble("score");
						double actualTaskScore = (score / 5.0);

						boolean succesfullOutcome = ((actualTaskScore > Reputation.scoreTh)) ? true
								: false;

						EvalWorker tmp = dataMapHolderEval.get(developerId);
						String currentTask = (tu.getGenericCat(catId) == 1) ? "Technical"
								: "Non-technical";
						String workerType = null;
						if (tmp == null) {
							tmp = new EvalWorker();
							workerType = currentTask;
						} else {

							workerType = tmp.getWorkerType();
						}
						catId = tu.adjustODeskCategory(workerType, catId);
						tu.updateEvalWorkerCV(dataMapHolderEval, developerId,
								catId, succesfullOutcome, actualTaskScore,
								approach, workerType, currentTask, model,fold);
					}
				}
			}
		} else if (model.equals("Multinomial")) {
			while (rs.next()) {
				int developerId = rs.getInt("developer");

				Integer tmpInt = Train.developerToSet.get(developerId);
				if (tmpInt != null && tmpInt == fold) {

					Integer catId = cats.get(rs.getInt("jobCategory"));

					if (catId != null) {

						double score = rs.getDouble("score");
						double actualTaskScore = (score / 5.0);
						int bucket = tu.getBucket(actualTaskScore);

						EvalWorker tmp = dataMapHolderEval.get(developerId);
						String currentTask = (tu.getGenericCat(catId) == 1) ? "Technical"
								: "Non-technical";
						String workerType = null;
						if (tmp == null) {
							tmp = new EvalWorker();
							workerType = currentTask;
						} else {

							workerType = tmp.getWorkerType();
						}
						catId = tu.adjustODeskCategory(workerType, catId);

						tu.updateEvalWorkerCV(dataMapHolderEval, developerId,
								catId, bucket, actualTaskScore, approach,
								workerType, currentTask, model,fold);
					}

				}
			}
		}
	}

	public void rawDataToBinomialModelCV(String level, String approach,
			String model, boolean test, int fold) {
		System.out.println("Quering DB.");
		System.out.println("Level:" + level);
		System.out.println("Approch:" + approach);
		System.out.println("Cross Validation");

		HashMap<Integer, Integer> cats = mapCategories();

		HashSet<Integer> techCats = new HashSet<Integer>();
		techCats.add(1);
		techCats.add(2);
		techCats.add(6);

		HashSet<Integer> nonTechCats = new HashSet<Integer>();
		nonTechCats.add(3);
		nonTechCats.add(4);
		nonTechCats.add(5);

		try {

			String selectString;

			selectString = "select developer,jobCategory, "
					+ "(availability+communication+cooperation+deadlines+quality+skills+total)/7 as score "
					+ "from `odesk`.`testtrain_atleast6`   order by date";

				ResultSet rs = getResultSet(selectString);
				System.out.println("Running Fold " + fold);

				runOnFold(level, approach, model, fold, cats, techCats,
						nonTechCats, test, rs);
			

		} catch (SQLException se) {
			se.printStackTrace();
		}
	}
}
