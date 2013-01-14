package kokkodis.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

import kokkodis.factory.EvalWorker;
import kokkodis.factory.ModelCategory;

public abstract class Queries {



	protected String username = "devel";
	protected String password = "developer";
	protected String dbname;
	protected Connection conn;

	

	
	public abstract void connect();
	

	/**
	 * 
	 * @param level
	 * @param approach
	 * @param model
	 * @param test
	 *            : whether or not we train or test!! Boolean, true for test!
	 */
	public abstract void rawDataToBinomialModel(String level, String approach,
			String model, boolean test);
	
	

}
