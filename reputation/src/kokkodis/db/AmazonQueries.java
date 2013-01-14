package kokkodis.db;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import kokkodis.amazon.AmazonTrain;
import kokkodis.factory.EvalWorker;
import kokkodis.factory.ModelCategory;
import kokkodis.utils.Utils;
import kokkodis.utils.amazon.TestUtils;
import kokkodis.utils.amazon.TrainUtils;

public class AmazonQueries extends Queries {

	public AmazonQueries() {
	}

	@Override
	public void connect() {
		try {
			
			 dbname = "ReviewsMak";
		
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

			Properties props = new Properties();
			props.put("user", username);
			props.put("password", password);
			props.put("databaseName", dbname);
			System.out.println("Trying to connect to db.");
			
			conn = DriverManager.getConnection(
					"jdbc:sqlserver://vpanos.stern.nyu.edu:1433"
					// "jdbc:sqlserver://localhost:2000;"
					, props);
			System.out.println("Connected!");
		} catch (SQLException e) {
			System.err.println("SQLException: " + e.getMessage());
		} catch (java.lang.ClassNotFoundException e) {
			System.err.print("ClassNotFoundException: ");
			System.err.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void rawDataToBinomialModel(String level, String approach,
			String model, boolean test) {

		Utils u = new Utils();
		HashMap<Integer, Integer> cats = u.mapCategories();
		HashMap<String, HashMap<Integer, ModelCategory>> dataMapHolder = null;

		HashMap<String, EvalWorker> dataMapHolderEval = null;
		TrainUtils trainUtils = null;
		TestUtils testUtils = null;
		try {
			String selectString;
			if (!test) {
				selectString = "select ReviewerId,HelpfulVotes,TotalVotes, Id "
						+ "from " + "Train_20_to_100 order by Date";
				trainUtils = new TrainUtils();
				dataMapHolder = new HashMap<String, HashMap<Integer,ModelCategory>>();

			} else{
				selectString = "select ReviewerId,HelpfulVotes,TotalVotes, Id "
						+ "from " + " Test_20_to_100 order by Date";
				testUtils = new TestUtils();
				dataMapHolderEval = new HashMap<String, EvalWorker>();
			}

			PreparedStatement prepStmt = conn.prepareStatement(selectString);
			prepStmt.execute();
			ResultSet rs = prepStmt.getResultSet();
			System.out.println("Mapping...");
			int i = 0;
			while (rs.next()) {
				i++;
				double helpfulVotes = rs.getDouble("HelpfulVotes");
				double totalVotes = rs.getDouble("TotalVotes");
				String developerId = rs.getString("ReviewerId");
				double actualTaskScore = (helpfulVotes / totalVotes);
				int catId = cats.get(rs.getInt("Id"));
				if (model.equals("Binomial")) {

					if (totalVotes >= AmazonTrain.votesTh) {
						boolean successfulOutcome = ((totalVotes > 0) && 
								(actualTaskScore > AmazonTrain.scoreTh)) ? true
								: false;
						if (!test) {

							trainUtils.updateWorkerHistoryAndPrintTuple(dataMapHolder,
									developerId, catId, successfulOutcome,
									actualTaskScore, approach, model);
						}else{
							testUtils.updateEvalWorker(dataMapHolderEval, developerId, catId,
									successfulOutcome, actualTaskScore, approach);
							
						}

					}
				} else {
					int bucket = trainUtils.getBucket(actualTaskScore);
					if (!test) {
						trainUtils.updateWorkerHistoryAndPrintTuple(dataMapHolder,
								developerId, catId, bucket, actualTaskScore,
								approach, model);
					}else{
						testUtils.updateEvalWorker(dataMapHolderEval, 
								developerId, catId, bucket, actualTaskScore, approach);
					}
				}
			}

		} catch (SQLException e) {

			e.printStackTrace();

		}

	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AmazonQueries aq = new AmazonQueries();
		aq.connect();

	}

}
