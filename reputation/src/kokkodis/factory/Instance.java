package kokkodis.factory;

import java.util.ArrayList;

public class Instance {

	private  int contractor;
	private  int curCategory;
	/*
	 * History in all categories up to now.
	 */
	private  ArrayList<String> curHistory;
	private  double curTaskLogit;
	
	public Instance(){
		curHistory = new ArrayList<String>();
	}

	public  int getContractor() {
		return contractor;
	}

	public  void setContractor(int contractor) {
		this.contractor = contractor;
	}

	public  int getCurCategory() {
		return curCategory;
	}

	public  void setCurCategory(int curCategory) {
		this.curCategory = curCategory;
	}

	public  double getCurTaskLogit() {
		return curTaskLogit;
	}

	public  void setCurTaskLogit(double curTaskLogit) {
		this.curTaskLogit = curTaskLogit;
	}

	public  ArrayList<String> getCurHistory() {
		return curHistory;
	}
	
	
	
}
