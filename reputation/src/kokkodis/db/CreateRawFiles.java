package kokkodis.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.omg.CORBA.Principal;

import kokkodis.odesk.Reputation;
import kokkodis.utils.PrintToFile;

public class CreateRawFiles {

	private static Connection conn;
	

	private void mainQ() {
		try {

			PrintToFile pf = new PrintToFile();
		//	pf.openFile(new File(Train.dataPath+"allData.csv"));
			pf.writeToFile("Developer,JobCategory, Date,Comments,AvailabilityScore," +
					"CommunicationScore,CooperationScore,DeadlinesScore,QualityScore," +
					"SkillsScore,TotalScore,FeedbackId,Assignment");
			
			String selectString = "select t1.\"Record ID#\" as \"feedbackId\", \"Developer (ref)\",\"Related Assignment\","
					+ "\"Provider Availability Score\", \"Related JobCategory\", t2.\"EndDate\", "
					+ "\"Provider Total Score\", \"Provider Comment\",\"Provider Communication Score\","
					+ "\"Provider Cooperation Score\",\"Provider Deadlines Score\","
					+ " \"Provider Quality Score\", \"Provider Skills Score\" "
					+ "from \"oDesk DB\".\"Feedbacks\"  t1,  "
					+ "\"oDesk DB\".\"Assignments\"  t2 where "
					+ "t1.\"Related Assignment\"=t2.\"Record ID#\" "
					+ " and \"Related JobCategory\" is not null "
					+ " and \"Provider Total Score\"!=0 ";
			System.out.println(selectString);
			PreparedStatement stmt = conn.prepareStatement(selectString);
			// System.out.println("Executing...");
		//	stmt.execute();
			// System.out.println("Query executed...");
			ResultSet rs = stmt.getResultSet();

			while (rs.next()) {
				String text = rs.getString("Provider Comment");
				if (text != null) {
					text = text.replaceAll("\\s+", " ");
					text = text.replaceAll("\"", " ");
					text = text.replaceAll("\'+", " ");
					text = text.replaceAll("\n+", " ");
					text = "\"" + text + "\"";
					
				}else
					text = "\"null\"";
				pf.writeToFile(rs.getString("Developer (ref)") + ","
						+ rs.getString("Related JobCategory") + ","
						+ rs.getString("EndDate") + "," + text + ","
						+ rs.getString("Provider Availability Score") + ","
						+ rs.getString("Provider Communication Score") + ","
						+ rs.getString("Provider Cooperation Score") + ","
						+ rs.getString("Provider Deadlines Score") + ","
						+ rs.getString("Provider Quality Score") + ","
						+ rs.getString("Provider Skills Score") + ","
						+ rs.getString("Provider Total Score") + ","
						+ rs.getString("feedbackId") + ","
						+ rs.getString("Related Assignment"));
			}
			rs.close();
			stmt.close();
			conn.close();

		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			conn = ConnectionFactory.getInstance().getConnection();
			createTrain();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		
	}

	private static void createTrain() {
		// TODO Auto-generated method stub
		
	}

}
