package imperial.modaclouds.fg.fg_report;

import java.util.ArrayList;
import java.util.HashMap;

public class ReportData {

	private int nbUsers;
	
	private int nbClasses;
	
	private ArrayList<String> className;
	
	private ArrayList<String> time;

	private HashMap<String, ComplexData> responseTime; 
	
	private HashMap<String, ComplexData> throughput;
	
	private HashMap<String, Double> demand;
	
	private HashMap<String, Double> thinkTime;
	
	private HashMap<String, Integer> nbUsersPerClass;
	
	
	public HashMap<String, Integer> getNbUsersPerClass() {
		return nbUsersPerClass;
	}

	public void setNbUsersPerClass(HashMap<String, Integer> nbUsersPerClass) {
		this.nbUsersPerClass = nbUsersPerClass;
	}

	public HashMap<String, Double> getThinkTime() {
		return thinkTime;
	}
	
	public ArrayList<String> getTime() {
		return time;
	}

	public void setTime(ArrayList<String> time) {
		this.time = time;
	}

	public void setThinkTime(HashMap<String, Double> thinkTime) {
		this.thinkTime = thinkTime;
	}

	public int getNbUsers() {
		return nbUsers;
	}

	public void setNbUsers(int nbUsers) {
		this.nbUsers = nbUsers;
	}

	public int getNbClasses() {
		return nbClasses;
	}

	public void setNbClasses(int nbClasses) {
		this.nbClasses = nbClasses;
	}

	public ArrayList<String> getClassName() {
		return className;
	}

	public void setClassName(ArrayList<String> className) {
		this.className = className;
	}

	public HashMap<String, ComplexData> getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(HashMap<String, ComplexData> responseTime) {
		this.responseTime = responseTime;
	}

	public HashMap<String, ComplexData> getThroughput() {
		return throughput;
	}

	public void setThroughput(HashMap<String, ComplexData> throughput) {
		this.throughput = throughput;
	}

	public HashMap<String, Double> getDemand() {
		return demand;
	}

	public void setDemand(HashMap<String, Double> demand) {
		this.demand = demand;
	}
	
}
