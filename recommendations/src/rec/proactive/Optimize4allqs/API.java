package rec.proactive.Optimize4allqs;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import fast.common.Bijection;
import fast.data.DataPoint;
import fast.featurehmm.FeatureHMM;
import fast.featurehmm.PdfFeatureAwareLogisticRegression;
import fast.featurehmm.Predictor;

public class API {
	private HashMap<String, FeatureHMM> featureHMMs = new HashMap<String, FeatureHMM>();
	private boolean parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit;
	private int nbHiddenStates, nbObsStates, knownState;
	private boolean allowForget, bias, verbose;
	private FASTDBInterface fastDB;

	public API(boolean parameterizing, boolean parameterizedInit, boolean parameterizedTran, boolean parameterizedEmit, int nbHiddenStates, int nbObsStates, int knownState, boolean allowForget, boolean bias, boolean verbose, FASTDBInterface fastDB) {
		this.parameterizing = parameterizing;
		this.parameterizedInit = parameterizedInit;
		this.parameterizedTran = parameterizedTran;
		this.parameterizedEmit = parameterizedEmit;
		this.nbHiddenStates = nbHiddenStates;
		this.nbObsStates = nbObsStates;
		this.allowForget = allowForget;
		this.knownState = knownState;
		this.bias = bias;
		this.verbose = verbose;
		this.fastDB = fastDB;

		if (!this.parameterizing || this.parameterizedInit || this.parameterizedTran || !this.parameterizedEmit)
			throw new RuntimeException("ERROR: currently I only support parameterizing=true && parameterizedEmit=true!");
		if (this.allowForget)
			throw new RuntimeException("ERROR: currently I only support allowForget=False!");
		if (this.knownState != 1)
			throw new RuntimeException("ERROR: currently I only support knownState=1!");
		if (!this.bias)
			throw new RuntimeException("ERROR: currently I only support bias=true!");
	}

	/**
	 * @param user
	 * @param course
	 * @param subgroup
	 * @param topic
	 * @param question
	 * @param topicOfPotentialActivity
	 * @param potentialActivity
	 * @param pastActivityToTopic
	 * @return
	 */
	public Double getProbQue(String user, int course, String group, String subgroup, String skill, String question, String skillForPotentialActivity, String potentialActivity, HashMap<String, String> pastActivityToSkill) {
		if (verbose)
			log("Getting Probability of question for user=" + user + ", course=" + course + ", group=" + group + ", subgroup=" + subgroup + ", skill=" + skill + ", question=" + question + ", skillForPotentialActivity=" + skillForPotentialActivity + ", potentialActivity=" + potentialActivity);

		
		if (pastActivityToSkill != null)
			throw new RuntimeException("ERROR: currently I only support pastActivityToSkills=null!");
		if (skill == null || skill.length() == 0)
			throw new RuntimeException("ERROR: skill == null || skill.length() == 0!");
		if (question == null || question.length() == 0)
			throw new RuntimeException("ERROR: question == null || question.length() == 0!");
		// if (!skillForPotentialActivity.equals(skill))
		// throw new RuntimeException("ERROR: currently I only support skillForPotentialActivity=skill!");

		HashMap<String, String> newPastActivityToSkill = new HashMap<String, String>();
		newPastActivityToSkill.put(potentialActivity, skillForPotentialActivity);// variables2_v2,
		double pCorrect = getOneSpecificPrediction(user, course, group, subgroup, skill, question, newPastActivityToSkill, -1);// probabilities = {predLabel, probClass1_n, posteriorProbState1_n, priorProbState1_n1};
		return pCorrect;
	}

	/**
	 * @param user
	 * @param course
	 * @param subgroup
	 * @param correctness
	 * @param skill
	 * @param question
	 * @param example
	 * @return
	 */
	public void updateKnowledge(String user, int course, String group, String subgroup, int correctness, String skill, String question, HashMap<String, String> pastActivityToSkill) {
		if (verbose)
			log("Updating knowledge for user=" + user + ", course=" + course + ", group=" + group + ", subgroup=" + subgroup + ", correctness=" + correctness + ", skill=" + skill + ", question=" + question);
		if (skill == null || skill.length() == 0)
			throw new RuntimeException("ERROR: skill == null || skill.length() == 0!");
		if (question == null || question.length() == 0)
			throw new RuntimeException("ERROR: question == null || question.length() == 0!");

		double updatedKnowldge = getOneSpecificPrediction(user, course, group, subgroup, skill, question, pastActivityToSkill, correctness);// probabilities = {predLabel, probClass1_n, posteriorProbState1_n, priorProbState1_n1};
		if (verbose)
			log("updated knowledge:\t" + updatedKnowldge);
		// insert into DB: String user, String course, String subgroup, String skill, updatedKnowldge
		HashMap<String, Double> skillToKnowledge = new HashMap<String, Double>();
		skillToKnowledge.put(skill, updatedKnowldge);
		if (fastDB != null)
			fastDB.saveUserModel(user, course, group, skillToKnowledge);
	}

	/**
	 * 
	 * @param fileName
	 * @param parameterizing
	 * @param parameterizedInit
	 * @param parameterizedTran
	 * @param parameterizedEmit
	 * @param nbHiddenStates
	 * @param nbObsStates
	 * @param knownState
	 * @param kcColumn
	 * @param initColumn
	 * @param learnColumn
	 * @param forgetColumn
	 * @param guessColumn
	 * @param slipColumn
	 * @param featureStartColumn
	 * @throws IOException
	 */
	public void getParametersFromFile(String fileName, int kcColumn, int knownStateColumn, int initColumn, int learnColumn, int forgetColumn, int guessColumn, int slipColumn, int featureStartColumn) throws IOException {
		if (nbHiddenStates != 2 || nbObsStates != 2)
			throw new RuntimeException("ERROR: currently we only support nbHiddenStates=2, nbObsStates=2!");

		featureHMMs = new HashMap<String, FeatureHMM>();

		// read one HMM
		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line = "";
		Bijection initFeatureNames = new Bijection(), tranFeatureNames = new Bijection(), emitFeatureNames = new Bijection();
		HashMap<String, Integer> allFeatureNameToColumn = new HashMap<String, Integer>();

		while ((line = reader.readLine()) != null) {
			// System.out.println(line);
			String[] splitResult = line.split(",");
			if (line.contains("KC")) {
				if (featureStartColumn != -1) {
					/*
					 * TODO: check whether there are repeated feature names, now we assume there aren't.
					 */
					for (int i = featureStartColumn; i < splitResult.length; i++) {
						String featureName = splitResult[i];
						if (splitResult[i].startsWith("init"))
							initFeatureNames.put(featureName);
						else if (splitResult[i].startsWith("learn"))// ||splitResult[i].startsWith("forget")
							tranFeatureNames.put(featureName);
						else if (splitResult[i].startsWith("guess"))
							emitFeatureNames.put(featureName);
						else if (splitResult[i].startsWith("slip")) {
							featureName = "1-" + splitResult[i];
							emitFeatureNames.put(featureName);
						}
						else
							throw new RuntimeException("ERROR: please specify the feature columns correclty!");
						allFeatureNameToColumn.put(featureName, i);
					}
					if (parameterizing && parameterizedInit && initFeatureNames.getSize() == 0)
						throw new RuntimeException("ERROR: please specify the features for initial probabilities correctly!");
					if (parameterizing && parameterizedTran && tranFeatureNames.getSize() == 0)
						throw new RuntimeException("ERROR: please specify the features for transition probabilities correctly!");
					if (parameterizing && parameterizedEmit && emitFeatureNames.getSize() == 0)
						throw new RuntimeException("ERROR: please specify the features for emission probabilities correctly!");
				}
			}
			else {
				if (Double.parseDouble(splitResult[knownStateColumn]) != 1.0 * knownState)
					throw new RuntimeException("ERROR: currently we only support knownState=" + knownState);
				String hmmName = splitResult[kcColumn];
				double[] probInit = null;
				double[][] probTran = null, probEmit = null;
				if (!splitResult[initColumn].equals("-1")) {
					probInit = new double[2];
					probInit[0] = Double.parseDouble(splitResult[initColumn]);
					probInit[1] = 1 - probInit[0];
				}
				if (!splitResult[learnColumn].equals("-1") && !splitResult[forgetColumn].equals("-1")) {
					probTran = new double[2][2];
					probTran[0][1] = Double.parseDouble(splitResult[learnColumn]);
					probTran[0][0] = 1 - probTran[0][1];
					probTran[1][0] = Double.parseDouble(splitResult[forgetColumn]);
					probTran[1][1] = 1 - probTran[1][0];
				}
				if (!splitResult[guessColumn].equals("-1") && !splitResult[slipColumn].equals("-1")) {
					probEmit = new double[2][2];
					probEmit[0][1] = Double.parseDouble(splitResult[guessColumn]);
					probEmit[0][0] = 1 - probEmit[0][1];
					probEmit[1][0] = Double.parseDouble(splitResult[slipColumn]);
					probEmit[1][1] = 1 - probEmit[1][0];
				}
				if (!parameterizedInit && probInit == null)
					throw new RuntimeException("ERROR: please specify the features for initial probabilities correctly!");
				if (!parameterizedTran && probTran == null)
					throw new RuntimeException("ERROR: please specify the features for transition probabilities correctly!");
				if (!parameterizedEmit && probEmit == null)
					throw new RuntimeException("ERROR: please specify the features for emission probabilities correctly!");

				double[] initFeatureWeights = null, tranFeatureWeights = null, emitFeatureWeights = null;
				if (initFeatureNames.getSize() > 0) {
					initFeatureWeights = new double[initFeatureNames.getSize()];
					for (int featureVectorIndex = 0; featureVectorIndex < initFeatureNames.getSize(); featureVectorIndex++) {
						String name = initFeatureNames.get(featureVectorIndex);
						int featureColumn = allFeatureNameToColumn.get(name);
						initFeatureWeights[featureVectorIndex] = Double.parseDouble(splitResult[featureColumn]);
					}
				}
				if (tranFeatureNames.getSize() > 0) {
					tranFeatureWeights = new double[tranFeatureNames.getSize()];
					for (int featureVectorIndex = 0; featureVectorIndex < tranFeatureNames.getSize(); featureVectorIndex++) {
						String name = tranFeatureNames.get(featureVectorIndex);
						int featureColumn = allFeatureNameToColumn.get(name);
						tranFeatureWeights[featureVectorIndex] = Double.parseDouble(splitResult[featureColumn]);
					}
				}
				if (emitFeatureNames.getSize() > 0) {
					emitFeatureWeights = new double[emitFeatureNames.getSize()];
					for (int featureVectorIndex = 0; featureVectorIndex < emitFeatureNames.getSize(); featureVectorIndex++) {
						String name = emitFeatureNames.get(featureVectorIndex);
						int featureColumn = allFeatureNameToColumn.get(name);
						Double featureWeight = Double.parseDouble(splitResult[featureColumn]);
						if (name.startsWith("1-slip"))
							featureWeight *= -1.0;
						emitFeatureWeights[featureVectorIndex] = featureWeight;
					}
				}

				ArrayList<PdfFeatureAwareLogisticRegression> initPdfs = new ArrayList<PdfFeatureAwareLogisticRegression>();
				ArrayList<PdfFeatureAwareLogisticRegression> tranPdfs = new ArrayList<PdfFeatureAwareLogisticRegression>();
				ArrayList<PdfFeatureAwareLogisticRegression> emitPdfs = new ArrayList<PdfFeatureAwareLogisticRegression>();

				for (int hiddenState = 0; hiddenState < nbHiddenStates; hiddenState++) {

					PdfFeatureAwareLogisticRegression initPdf = new PdfFeatureAwareLogisticRegression(parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit, nbHiddenStates, nbObsStates);
					if (parameterizing && parameterizedInit)
						initPdf.initialize(initFeatureWeights, initFeatureNames);
					else
						initPdf.initialize(probInit[hiddenState]);

					PdfFeatureAwareLogisticRegression tranPdf = new PdfFeatureAwareLogisticRegression(parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit, nbHiddenStates, nbObsStates);
					if (parameterizing && parameterizedTran)
						tranPdf.initialize(tranFeatureWeights, tranFeatureNames);
					else
						tranPdf.initialize(probTran[hiddenState]);

					PdfFeatureAwareLogisticRegression emitPdf = new PdfFeatureAwareLogisticRegression(parameterizing, parameterizedInit, parameterizedTran, parameterizedEmit, nbHiddenStates, nbObsStates);
					if (parameterizing && parameterizedEmit)
						emitPdf.initialize(emitFeatureWeights, emitFeatureNames);
					else
						emitPdf.initialize(probEmit[hiddenState]);

					initPdfs.add(initPdf);
					tranPdfs.add(tranPdf);
					emitPdfs.add(emitPdf);
				}
				FeatureHMM hmm = new FeatureHMM(initPdfs, tranPdfs, emitPdfs);
				featureHMMs.put(hmmName, hmm); // (opts.limit_fast_guess_slip?opts.FAST_GUESS_SLIP_LIMIT:null),
				// System.out.println(hmm);

				initFeatureNames = new Bijection();
				tranFeatureNames = new Bijection();
				emitFeatureNames = new Bijection();
				allFeatureNameToColumn = new HashMap<String, Integer>();
			}
		}
		reader.close();
	}

	public double getOneSpecificPrediction(String user, int course, String group, String subgroup, String skill, String question, HashMap<String, String> pastActivityToSkill, int correctness) {
		if (verbose)
			log("skill:" + skill);
		FeatureHMM hmm = getHMM(skill);

		HashMap<String, Double> features = getSpecificFeatures(skill, question, pastActivityToSkill, hmm);
		DataPoint dp = getFormattedDataPoint(features, correctness, hmm);

		/* TODO: Now assume no transferring from one skill to another. */
		Double priorKnowledge = null;
		if (fastDB != null)
			priorKnowledge = fastDB.getUserModel(user, course, group, skill);
		if (priorKnowledge == null)
			priorKnowledge = hmm.getInitialPdf(0).getProbability();
		if (verbose)
			log("priorKnowledge:\t" + priorKnowledge);

		double probability;
		if (correctness == -1) {
			double[] probabilities = Predictor.getOnePrediction(hmm, dp, priorKnowledge, knownState);
			probability = probabilities[2];
		}
		else {
			// probabilities = {predLabel, probClass1_n, posteriorProbState1_n, priorProbState1_n1};
			double[] probabilities = Predictor.getOnePredictionAndInference(hmm, dp, priorKnowledge, knownState, verbose);// verbose
			probability = probabilities[3];
		}
		if (verbose)
			log("specific probability:\t" + probability);
		return probability;
	}

	public HashMap<String, Double> getSpecificFeatures(String skill, String question, HashMap<String, String> pastActivityToSkill, FeatureHMM hmm) {
		/*
		 * TODO: Now assume that question or example has unique ids, so we don't differentiate by input argument pastActivityToTopic.
		 */
		if (skill == null || skill.length() == 0)
			throw new RuntimeException("skill == null || skill.length() == 0");
		if (question == null || question.length() == 0)
			throw new RuntimeException("question == null || question.length() == 0");

		HashMap<String, Double> features = new HashMap<String, Double>();
		features.put("guess_bias", 1.0);
		features.put("1-slip_bias", 1.0);

		Bijection emissionFeatures = hmm.getEmissionPdf(0).getFeatureNames();
		if (emissionFeatures == null || emissionFeatures.getSize() == 0)
			throw new RuntimeException("emissionFeatures == null || emissionFeatures.getSize() == 0");

		String QD = "features_QD_|" + question + "|";
		if (emissionFeatures.contains("guess_" + QD)) {
			features.put("guess_" + QD, 1.0);
			features.put("1-slip_" + QD, 1.0);
		}
		else{
			log("WARNING: your question (" + question + ") doesn't belong to current skill (" + skill + ")!");
		}
		if (pastActivityToSkill != null && pastActivityToSkill.size() > 0) {
			if (pastActivityToSkill.size() > 1)
				throw new RuntimeException("ERROR: currently I only consider one past activity!");

			String pastActivity = "";
			for (Map.Entry<String, String> entry : pastActivityToSkill.entrySet()) {
				pastActivity = entry.getKey();
				// String skillOfPastActivity = entry.getValue();
				// if (!skillOfPastActivity.equals(skill))
				// throw new RuntimeException("ERROR: currently I only consider past activity within the same skill!");
			}
			if (pastActivity != null && pastActivity.length() > 0) {
				/* TODO: Now requires that we only parameterize emission */

				String RBED = "features_RBED_|" + pastActivity + "|";
				String RBE2QD = "features_RBE2QD_|" + pastActivity + "|TO|" + question + "|";
				String RBQ2QD = "features_RBQ2QD_|" + pastActivity + "|TO|" + question + "|";
				if (emissionFeatures.contains("guess_" + RBED)) {
					features.put("guess_" + RBED, 1.0);
					features.put("1-slip_" + RBED, 1.0);
				}
				if (emissionFeatures.contains("guess_" + RBE2QD)) {
					features.put("guess_" + RBE2QD, 1.0);
					features.put("1-slip_" + RBE2QD, 1.0);
					if (emissionFeatures.contains("guess_" + RBQ2QD))
						throw new RuntimeException("ERRRO: the past activity (" + pastActivity + ") is both the name for a question and an example!");
				}
				if (emissionFeatures.contains("guess_" + RBQ2QD)) {
					features.put("guess_" + RBQ2QD, 1.0);
					features.put("1-slip_" + RBQ2QD, 1.0);
				}
			}
		}
		return features;
	}

	public DataPoint getFormattedDataPoint(HashMap<String, Double> features, int outcome, FeatureHMM hmm) {
		if (features == null || features.size() == 0)
			throw new RuntimeException("features == null || features.size() == 0");

		double[][][] aFeatures = new double[2][3][];
		int hiddenState = 0;
		Bijection emissionFeatures = hmm.getEmissionPdf(hiddenState).getFeatureNames();
		if (emissionFeatures == null || emissionFeatures.getSize() == 0)
			throw new RuntimeException("emissionFeatures == null || emissionFeatures.getSize() == 0");
		aFeatures[hiddenState][2] = new double[emissionFeatures.getSize()];

		for (Map.Entry<String, Double> entry : features.entrySet()) {
			String featureName = entry.getKey();
			Double featureValue = entry.getValue();
			if (featureName == null || featureName.length() == 0)
				throw new RuntimeException("featureName == null || featureName.length() == 0");
			if (featureValue == null)
				throw new RuntimeException("featureValue == null");
			if (featureName.startsWith("guess")) {
				if (!emissionFeatures.contains(featureName))
					throw new RuntimeException("!emissionFeatures.contains featureName:" + featureName);
				int featureIndex = emissionFeatures.get(featureName);
				aFeatures[hiddenState][2][featureIndex] = featureValue;
				if (verbose)
					log("featureName:" + featureName + ", value:" + featureValue + ", weight:" + hmm.getEmissionPdf(hiddenState).getFeatureWeights()[featureIndex]);
			}
		}

		hiddenState = 1;
		emissionFeatures = hmm.getEmissionPdf(hiddenState).getFeatureNames();
		aFeatures[hiddenState][2] = new double[emissionFeatures.getSize()];
		for (Map.Entry<String, Double> entry : features.entrySet()) {
			String featureName = entry.getKey();
			Double featureValue = entry.getValue();
			if (featureName.startsWith("1-slip")) {
				if (!emissionFeatures.contains(featureName))
					throw new RuntimeException("emissionFeatures doesn't contain featureName: " + featureName);
				int featureIndex = emissionFeatures.get(featureName);
				aFeatures[hiddenState][2][featureIndex] = featureValue;
				if (verbose)
					log("featureName:" + featureName + ", value:" + featureValue + ", weight:" + hmm.getEmissionPdf(hiddenState).getFeatureWeights()[featureIndex]);
			}
		}

		DataPoint dp = new DataPoint(-1, -1, -1, -1, -1, outcome, aFeatures);
		// public DataPoint(int aStudent, int aSkill, int aProb, int aStep, int
		// aFold, int aOutcome, double[][][] aFeatures) {
		return dp;
	}

	public FeatureHMM getHMM(String hmmName) {
		FeatureHMM hmm;
		if (featureHMMs.containsKey(hmmName)) {
			hmm = featureHMMs.get(hmmName);
			if (hmm == null || hmmName.equals("") || hmmName == null)
				throw new RuntimeException("ERROR: hmmName doesn't exist (" + hmmName + ")!");
		}
		else
			throw new RuntimeException("ERROR: hmmName doesn't exist (" + hmmName + ")!");
		return hmm;
	}
	
	public HashMap<String, Double> getPrevQueProbForAct(String user, String domain, int course, String group,
			String activity){
    	if (fastDB != null)
           return fastDB.getPrevQueProbForAct( user,  domain,  course,  group,
        			 activity);
    	
    	return new HashMap<String, Double>();
    }
    
    public void putNewQuestionProbabilityinDB(String user, String domain,
			int course, String group, String activity, String question,
			Double prob) {
    	if (fastDB != null){
            fastDB.putNewQuestionProbabilityinDB(user, domain,course, group, activity, question, prob);
    	}else{
            throw new RuntimeException("ERROR: DB connection is null!");
    	}
     			
	}

	public static void log(String str) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		str = dateFormat.format(date) + " " + str;
		System.out.println(str);
	}

	// for testing
	// public FASTDBInterface getFASTDBObj() {
	// return this.fastDB;
	// }

	public static void main(String[] args) throws IOException {
		/*
		 * Please use this configuration of initializing an API object. You can change verbose, and fastDB(e.g., set null)
		 * 
		 * Caution: remember to call fastDB.close() to close the DB connection at the end!
		 */
		boolean verbose = true;
		// FASTDBInterface fastDB = new FASTDBInterface("jdbc:mysql://localhost/fast", "root", "", verbose);
		// fastDB.openConnection();
		FASTDBInterface fastDB = null;

		API fastAPI = new API(true, false, false, true, 2, 2, 1, false, true, verbose, fastDB);

		// Please just change the file name for your purpose and keep the other arguments.
		fastAPI.getParametersFromFile("/Users/hy/Center/Study/Codes/CodingProjects/recommendation/data/Parameters_FAST_11131.csv", 0, 1, 7, 4, 8, 6, 10, 12);

		// Following are examples of how to use the two functions
		String user = "dguerra", group = "ADL";
		int course = 13;
		HashMap<String, String> pastActivityToSkill;
		String skill = "Variables", question = "jVariables2";

		// Example 1: updating knowledge or getProbQue without pastActivity
		pastActivityToSkill = null;
		fastAPI.updateKnowledge(user, course, group, null, 1, skill, question, pastActivityToSkill);

		// // Example 2: updating knowledge with a past question and current question
		// pastActivityToSkill = new HashMap<String, String>();
		// pastActivityToSkill.put("jVariables1", skill);
		// fastAPI.updateKnowledge(user, course, group, null, 1, skill, question, pastActivityToSkill);

		// // // // Example 3: updating knowledge with a past example and current question
		// pastActivityToSkill = new HashMap<String, String>();
		// pastActivityToSkill.put("variables2_v2", skill);//
		// fastAPI.updateKnowledge(user, course, group, null, 1, skill, question, pastActivityToSkill);

		// // Example 4: get Prob(Que) for a potential question
		// pastActivityToSkill = null;
		// fastAPI.getProbQue(user, course, group, null, skill, question, skill, "jVariables1", pastActivityToSkill);

		// // Example 5: get Prob(Que) for a potential example
		// pastActivityToSkill = null;
		// fastAPI.getProbQue(user, course, group, null, skill, question, skill, "variables2_v2", pastActivityToSkill);

		// // Example 6: another skill and question, updating knowledge with a past question and current question
		// String skill = "Primitive_Data_Types", question = "jDouble1";
		// pastActivityToSkill = new HashMap<String, String>();
		// pastActivityToSkill.put("jInteger1", skill);
		// fastAPI.updateKnowledge(user, course, group, null, 1, skill, question, pastActivityToSkill);
		//
		// // // Example 7: another skill and question, get Prob(Que) for a potential question
		// pastActivityToSkill = null;
		// fastAPI.getProbQue(user, course, group, null, skill, question, skill, "jInteger1", pastActivityToSkill);

		// // Example 8: another user, updating knowledge with a past question and current question, get Prob(Que) for a potential question
		// String skill = "Primitive_Data_Types", question = "jDouble1", user = "yuh43";
		// HashMap<String, String> pastActivityToSkill = new HashMap<String, String>();
		// pastActivityToSkill.put("jInteger1", skill);
		// fastAPI.updateKnowledge(user, course, group, null, 1, skill, question, pastActivityToSkill);

		// pastActivityToSkill = new HashMap<String, String>();
		// pastActivityToSkill.put("TypeCasting_v2", skill);//
		// fastAPI.updateKnowledge(user, course, group, null, 1, skill, question, pastActivityToSkill);

		// pastActivityToSkill = null;
		// fastAPI.getProbQue(user, course, group, null, skill, question, skill, "jInteger1", pastActivityToSkill);

		// Example 9: get probability of a potential question not within the topic
		// pastActivityToSkill = null;
		// String skillOfPotentialActivity = "";
		// String potentialActivity = "";// jDouble1
		// // question = null;
		// // skill = "";
		// fastAPI.getProbQue(user, course, group, null, skill, question, skillOfPotentialActivity, potentialActivity, pastActivityToSkill);

		if (fastDB != null)
			fastDB.closeConnection();
	}
}
