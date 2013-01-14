/****************************************************
 * @author Marios Kokkodis                          *
 * comments/questions : mkokkodi@odesk.com     		*
 *													*					  
 *  Class Description - general odesk queries  		*	
 *													*  
 * 	*************************************************									
 */

package kokkodis.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kokkodis.factory.PropertiesFactory;

/**
 * DAO from SQL to POJOs and back
 * 
 * 
 * 
 * For complicated tasks iparxoun frameworks: hibernate, jp-api (jpa -
 * 
 * 
 * @author mkokkodi
 * 
 */
public class ConnectionFactory {

	private static Properties props = null;
	private static Connection conn = null;

	private static ConnectionFactory connectionFactory = null;

	private ConnectionFactory() {

		try {
			Class.forName(props.getProperty("driver"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection() throws SQLException {
		if (conn == null)
			initializeConnection();
		return conn;
	}

	public static ConnectionFactory getInstance() {
		if (connectionFactory == null) {
			if (props == null)
				props = PropertiesFactory.getInstance().getProps();
			connectionFactory = new ConnectionFactory();
			try {
				initializeConnection();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return connectionFactory;
	}

	private static void initializeConnection() throws SQLException {
		if (conn == null)
			conn = DriverManager.getConnection(props.getProperty("url"),
					props.getProperty("dbuser"), props.getProperty("password"));

	}

	/**
	 * 
	 * @param selectString
	 *            : the select query.
	 * @return a list of HashMaps in the form "column name" -> "value"
	 */
	public List<Map<String, String>> getListOfMaps(String selectString) {
		ResultSet rs = null;
		List<Map<String, String>> dataList = new ArrayList<Map<String, String>>();
		try {
			rs = connectionFactory.getResultSet(selectString);

			ResultSetMetaData rsmd = rs.getMetaData();
			String[] columns = new String[rsmd.getColumnCount()];
			for (int i = 0; i < columns.length; i++)
				columns[i] = rsmd.getColumnName(i + 1);

			while (rs.next()) {
				HashMap<String, String> hm = new HashMap<String, String>();

				for (String column : columns) {
					hm.put(column, rs.getString(column));
				}
				dataList.add(hm);

			}
			return dataList;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}

	}

	public ResultSet getResultSet(String selectString) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(selectString);
		// Statement st = conn.createStatement();

		// Turn use of the cursor on.
		stmt.setFetchSize(1000);
		// ResultSet rs = stmt.ex;
		stmt.execute();
		return stmt.getResultSet();
	}

	
	/**
	 * 
	 * @param schema
	 * @param table
	 * @param insertString
	 */
	public void insertRow(String schema, String table, String insertString) {
		try {

			String selectString = "insert into " + schema + "." + table
					+ " values " + insertString;

			System.out.println(selectString);
			PreparedStatement stmt = conn.prepareStatement(selectString);
			stmt.executeUpdate();

		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

	}

}
