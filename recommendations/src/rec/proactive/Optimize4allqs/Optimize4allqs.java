package rec.proactive.Optimize4allqs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;


public class Optimize4allqs {

	private static AggregateDB aggregate_db;
	private static UM2DB um2_db;

	// ******************* Start ******************* //
	//NOTE: the following parameters are constant//
	private static final int numberForFASTHistoryUpdate = 1;
	private static final boolean fastAcceptsAllSkills = false; //FAST might accept only activities with the same skill as last activity to update the knowledge model
	private static final String[] fastProviders = {"quizjet","webex","animatedexamples", "sqlnot"};
	private static final boolean queryAggregateDBInsteadofFile = false; // to make things faster, we can use a file to get topics instead of reading from aggregate DB
	private static final boolean fastInitializationMode = false; //For the first time that FAST runs, to calculate all probabilities to reduce the recommendation time for next steps
	private static final int noOfLastActivitiesNottoBeRecommended = 3; //this number defines if we want to exclude the last N activities from the recommendation list (what N should be)
	private static final boolean notRecommendCompleteProgress = true; //avoids recommending activities with 100% progress
	private static final boolean recommendBeforeLastSeenTopic = true; //this is to avoid recommending very advanced topics to users, so we only recommend up to one topic ahead of user's last seen topic
	private static final double exampleProgressConsideredAsComplete = 0.6; //for examples, avoids recommending them with exampleProgressConsideredAsComplete% progress instead of 100%
	// ******************* End ******************* //

	private static void openAggregateDBConnections(String agg_dbstring, String agg_dbuser, String agg_dbpass) {
		//		System.out.println("aggregate_cm.dbstring "+aggregate_cm.dbstring);
		aggregate_db = new AggregateDB(agg_dbstring, agg_dbuser, agg_dbpass);
		aggregate_db.openConnection();
	}

	private static void closeAggregateDBConnections() {
		if(aggregate_db!=null)
			aggregate_db.closeConnection();
		aggregate_db = null;
	}

	private static void openUM2DBConnections(String um2_dbstring, String um2_dbuser, String um2_dbpass) {
		um2_db = new UM2DB(um2_dbstring, um2_dbuser, um2_dbpass);
		um2_db.openConnection();
	}

	private static void closeUM2DBConnections() {
		if (um2_db!=null)
			um2_db.closeConnection();
		um2_db = null;
	}

	public static ArrayList<ArrayList<String>> calculateSequenceRank(String usr, String grp,
			String cid, String domain, String lastContentId,
			String lastContentResult, String lastContentProvider,
			String[] contentList, int proactive_max, String proactive_method,
			double proactive_threshold,
			ArrayList<ArrayList<String>> sequencingList,
			String agg_dbstring, String agg_dbuser, String agg_dbpass,
			String um2_dbstring, String um2_dbuser, String um2_dbpass,
			String fastdbstring, String paramFilePath,
			String topicContentAddr,String topicOrderAddr, String subApproach) throws IOException {
		int course = Integer.parseInt(cid);

		FASTDBInterface fastDB = new FASTDBInterface(fastdbstring, um2_dbuser, um2_dbpass);
		fastDB.openConnection();
		boolean verbose = false;
		API fastAPI = new API(true, false, false, true, 2, 2, 1, false, true, verbose, fastDB);
		fastAPI.getParametersFromFile(paramFilePath, 0, 1, 7, 4, 8, 6, 10, 12);
		if (fastInitializationMode){
			//initialize FAST probabilities for all users
			openUM2DBConnections(um2_dbstring, um2_dbuser, um2_dbpass);
			List<String> studentList = um2_db.getStudentList(grp);
			closeUM2DBConnections();
			for (String s:studentList){
				if (!fastDB.existUserProbability(s, course, grp)){
					//System.out.println("user does not exist "+s);
					initializeFASTProbabilities(s, domain, course, grp, contentList, proactive_method,
							proactive_threshold, proactive_max, fastAPI,
							agg_dbstring, agg_dbuser, agg_dbpass,
							topicContentAddr,topicOrderAddr);
				}
			}
		}else{
			if (lastContentProvider.toLowerCase().equals("quizjet")){//update FAST's knowlege level only if the student has worked on a question

				updateFastKnowledge(usr,  domain, course, grp, lastContentId, lastContentResult,
						lastContentProvider, numberForFASTHistoryUpdate, fastProviders, fastAPI,
						agg_dbstring, agg_dbuser, agg_dbpass,
						um2_dbstring, um2_dbuser, um2_dbpass,
						topicContentAddr,topicOrderAddr);
			}
			if (lastContentId==null || lastContentId.isEmpty() || lastContentId.equals("")){
				HashMap<String, String> lastNActivities = getLastNActivitieswithTopics(usr, 1, grp, domain, fastProviders,
						agg_dbstring, agg_dbuser, agg_dbpass,
						um2_dbstring, um2_dbuser, um2_dbpass,
						topicContentAddr, topicOrderAddr);
				if (lastNActivities!=null && !lastNActivities.isEmpty()){
					lastContentId = lastNActivities.keySet().iterator().next();
					lastContentProvider = fastProviders[0];
				}else{
					lastContentProvider = fastProviders[0];
				}
			}
			if (Arrays.asList(fastProviders).contains(lastContentProvider.toLowerCase())){
				if (subApproach.equals("optimize4allqs")){
					sequencingList = optimize4allqs( usr,  domain, course, grp, lastContentId, contentList, proactive_method,
							proactive_threshold, proactive_max, fastAPI,
							agg_dbstring, agg_dbuser, agg_dbpass,
							topicContentAddr,topicOrderAddr,
							um2_dbstring, um2_dbuser, um2_dbpass);
				} else if (subApproach.equals("optimize4allqswithprobabilities")) {
					sequencingList = optimize4allqsWithProbabilities( usr,  domain, course, grp, lastContentId, contentList, proactive_method,
							proactive_threshold, proactive_max, fastAPI,
							agg_dbstring, agg_dbuser, agg_dbpass,
							topicContentAddr,topicOrderAddr,
							um2_dbstring, um2_dbuser, um2_dbpass);
				}
			}
		}
		fastDB.closeConnection();
		return sequencingList;
	}

	/**
	 * 
	 * @param usr
	 * @param grp
	 * @param cid
	 * @param domain
	 * @param lastContentId
	 * @param lastContentResult
	 * @param lastContentProvider
	 * @param contentList
	 * @param proactive_max
	 * @param proactive_method
	 * @param proactive_threshold
	 * @param sequencingList
	 * @param agg_dbstring
	 * @param agg_dbuser
	 * @param agg_dbpass
	 * @param um2_dbstring
	 * @param um2_dbuser
	 * @param um2_dbpass
	 * @param fastdbstring
	 * @param paramFilePath
	 * @param topicContentAddr
	 * @param topicOrderAddr
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ArrayList<String>> calculateSequenceRankWithProbabilities(String usr, String grp,
			String cid, String domain, String lastContentId,
			String lastContentResult, String lastContentProvider,
			String[] contentList, int proactive_max, String proactive_method,
			double proactive_threshold,
			ArrayList<ArrayList<String>> sequencingList,
			String agg_dbstring, String agg_dbuser, String agg_dbpass,
			String um2_dbstring, String um2_dbuser, String um2_dbpass,
			String fastdbstring, String paramFilePath,
			String topicContentAddr,String topicOrderAddr) throws IOException {
		int course = Integer.parseInt(cid);

		FASTDBInterface fastDB = new FASTDBInterface(fastdbstring, um2_dbuser, um2_dbpass);
		fastDB.openConnection();
		boolean verbose = false;
		API fastAPI = new API(true, false, false, true, 2, 2, 1, false, true, verbose, fastDB);
		fastAPI.getParametersFromFile(paramFilePath, 0, 1, 7, 4, 8, 6, 10, 12);
		if (fastInitializationMode){
			//initialize FAST probabilities for all users
			openUM2DBConnections(um2_dbstring, um2_dbuser, um2_dbpass);
			List<String> studentList = um2_db.getStudentList(grp);
			closeUM2DBConnections();
			for (String s:studentList){
				if (!fastDB.existUserProbability(s, course, grp)){
					//System.out.println("user does not exist "+s);
					initializeFASTProbabilities(s, domain, course, grp, contentList, proactive_method,
							proactive_threshold, proactive_max, fastAPI,
							agg_dbstring, agg_dbuser, agg_dbpass,
							topicContentAddr,topicOrderAddr);
				}
			}
		}else{
			if (lastContentProvider.toLowerCase().equals("quizjet")){//update FAST's knowledge level only if the student has worked on a question

				updateFastKnowledge(usr,  domain, course, grp, lastContentId, lastContentResult,
						lastContentProvider, numberForFASTHistoryUpdate, fastProviders, fastAPI,
						agg_dbstring, agg_dbuser, agg_dbpass,
						um2_dbstring, um2_dbuser, um2_dbpass,
						topicContentAddr,topicOrderAddr);
			}
			if (lastContentId==null || lastContentId.isEmpty() || lastContentId.equals("")){
				HashMap<String, String> lastNActivities = getLastNActivitieswithTopics(usr, 1, grp, domain, fastProviders,
						agg_dbstring, agg_dbuser, agg_dbpass,
						um2_dbstring, um2_dbuser, um2_dbpass,
						topicContentAddr, topicOrderAddr);
				if (lastNActivities!=null && !lastNActivities.isEmpty()){
					lastContentId = lastNActivities.keySet().iterator().next();
					lastContentProvider = fastProviders[0];
				}else{
					lastContentProvider = fastProviders[0];
				}
			}
			if (Arrays.asList(fastProviders).contains(lastContentProvider.toLowerCase())){
				sequencingList = optimize4allqs( usr,  domain, course, grp, lastContentId, contentList, proactive_method,
						proactive_threshold, proactive_max, fastAPI,
						agg_dbstring, agg_dbuser, agg_dbpass,
						topicContentAddr,topicOrderAddr,
						um2_dbstring, um2_dbuser, um2_dbpass);
			}
		}
		fastDB.closeConnection();
		return sequencingList;
	}

	/**
	 * @author Sherry
	 * @param user
	 * @param domain
	 * @param group
	 * @param contentList
	 * @param approach
	 * @param proactive_threshold
	 * @param proactive_max
	 * @return a list of recommended activities for user
	 * This method uses FAST to recommend the activities that can maximize
	 *  the probability of the student answering all of the questions in the course at the moment
	 */
	private static ArrayList<ArrayList<String>> optimize4allqs(
			String user,  String domain,int course, String group, String lastContentID, String[] contentList,
			String approach, double proactive_threshold, 
			int proactive_max, API fastAPI,
			String agg_dbstring, String agg_dbuser, String agg_dbpass,
			String topicContentAddr,String topicOrderAddr,
			String um2_dbstring, String um2_dbuser, String um2_dbpass
	) {		

		long startTime = System.currentTimeMillis();

		HashMap<String, String> allQuestionsTopics = getAllQuestionswithTopics(group,agg_dbstring, agg_dbuser, agg_dbpass,topicContentAddr,topicOrderAddr); //all questions in the course with their topics
		HashMap<String, String> allActivitiesTopics = new HashMap<String, String>();
		ArrayList<ArrayList<String>> sortedActivities = new ArrayList<ArrayList<String>>();
		//TODO: define subgroup if it's needed
		String subgroup =null;
		Map<String, Double> activityProbability = new HashMap<String, Double>();
		AggregateFileReader afr = new AggregateFileReader(topicContentAddr,topicOrderAddr);
		openAggregateDBConnections(agg_dbstring, agg_dbuser, agg_dbpass);
		for (String activity :contentList){
			if (!allActivitiesTopics.containsKey(activity)){
				if (queryAggregateDBInsteadofFile){
					allActivitiesTopics.put(activity, aggregate_db.getActivityTopic(activity, group));
				}else{
					allActivitiesTopics.put(activity, afr.getActivityTopic(activity, group));
				}
			}
			String activityTopic = allActivitiesTopics.get(activity);
			//System.out.println("activity topic for " +activity + " is " + activityTopic);
			if (fastAcceptsAllSkills){
				for (String question : allQuestionsTopics.keySet()){
					if (activityProbability.containsKey(activity)){
						HashMap<String, String> pastActivityToSkill = null;
						Double prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
						activityProbability.put(activity, activityProbability.get(activity) + prob);
					}
					else{
						HashMap<String, String> pastActivityToSkill = null;
						Double prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
						activityProbability.put(activity,prob);
					}

				}
			}else{
				HashMap<String, String> pastActivityToSkill = null;
				String lastActivityTopic ="";
				if (lastContentID!= null && !lastContentID.isEmpty()){
					if (queryAggregateDBInsteadofFile){
						lastActivityTopic = aggregate_db.getActivityTopic(lastContentID, group);
					}else{
						lastActivityTopic = afr.getActivityTopic(lastContentID, group);
					}
				}
				HashMap<String, Double> previousProbabilities = fastAPI.getPrevQueProbForAct(user, domain,course, group, activity);
				for (String question : allQuestionsTopics.keySet()){
					Double prob = 0.0;
					String questionTopic = allQuestionsTopics.get(question);
					if (activityTopic.equals(questionTopic)){
						if (questionTopic.equals(lastActivityTopic)){
							prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
							fastAPI.putNewQuestionProbabilityinDB(user, domain,course, group, activity, question, prob);
						}else{
							if (previousProbabilities.containsKey(question)){
								prob = previousProbabilities.get(question);

							}else{
								prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
								fastAPI.putNewQuestionProbabilityinDB(user, domain,course, group, activity, question, prob);
							}
						}
					}else{
						if (previousProbabilities.containsKey(question)){
							prob = previousProbabilities.get(question);
						}else{
							prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
							fastAPI.putNewQuestionProbabilityinDB(user, domain,course, group, activity, question, prob);
						}
					}

					if (activityProbability.containsKey(activity)){
						activityProbability.put(activity, activityProbability.get(activity) + prob);
					}
					else{
						activityProbability.put(activity,prob);
					}

				} //for question
			} // for if fast accepts all skills

		} //for activity

		//System.out.println("activityProbability size "+activityProbability.size());
		long endTime   = System.currentTimeMillis();
		//System.out.println("loop over activities and questions time: "+(endTime-startTime));
		//startTime = System.currentTimeMillis();
		PriorityQueue<Entry<String, Double>> pq = new PriorityQueue<Map.Entry<String,Double>>(activityProbability.size(), new Comparator<Entry<String, Double>>() {
			@Override 
			public int compare(Entry<String, Double> arg0,
					Entry<String, Double> arg1) {
				return arg1.getValue().compareTo(arg0.getValue());
			}
		});

		pq.addAll(activityProbability.entrySet());

		if (notRecommendCompleteProgress){
			if (aggregate_db==null){
				openAggregateDBConnections(agg_dbstring, agg_dbuser, agg_dbpass);
			}
			HashMap<String,Double> activityProgress = aggregate_db.getActivityProgress(user, course);
			int lastRecommendableTopic = afr.getLastRecommendableTopic(activityProgress, group);
			int count = proactive_max;
			while (!pq.isEmpty()) {
				if (count == 0){
					break;
				}
				Entry<String, Double> e = pq.poll();
				String potentialActivity = e.getKey();
				if (activityProgress != null && !activityProgress.isEmpty()){
					if (!activityProgress.containsKey(potentialActivity)){
						ArrayList<String> list = new ArrayList<String>();
						list.add(potentialActivity);
						list.add(String.valueOf((double) e.getValue()/100.0));
						list.add(approach);
						sortedActivities.add(list);
						count--;
					}else{
						//TODO: should write the code for fetching from database
						if (!queryAggregateDBInsteadofFile){
							if (recommendBeforeLastSeenTopic){
								if (afr.getTopicOrder(afr.getActivityTopic(potentialActivity, group))<=lastRecommendableTopic){
									HashMap<String, String> allExamples = afr.getAllofOneContentTypewithTopics(group, "webex");
									if (allExamples.containsKey(potentialActivity)){
										//for examples, the bar is lower 	
										if (activityProgress.get(potentialActivity)<exampleProgressConsideredAsComplete){
											ArrayList<String> list = new ArrayList<String>();
											list.add(potentialActivity);
											list.add(String.valueOf((double) e.getValue()/100.0));
											list.add(approach);
											sortedActivities.add(list);
											count--;
										}
									}else{
										if (activityProgress.get(potentialActivity)<1.0){
											ArrayList<String> list = new ArrayList<String>();
											list.add(potentialActivity);
											list.add(String.valueOf((double) e.getValue()/100.0));
											list.add(approach);
											sortedActivities.add(list);
											count--;
										}
									}
								}
							}else{
								HashMap<String, String> allExamples = afr.getAllofOneContentTypewithTopics(group, "webex");
								if (allExamples.containsKey(potentialActivity)){
									//for examples, the bar is lower 	
									if (activityProgress.get(potentialActivity)<exampleProgressConsideredAsComplete){
										ArrayList<String> list = new ArrayList<String>();
										list.add(potentialActivity);
										list.add(String.valueOf((double) e.getValue()/100.0));
										list.add(approach);
										sortedActivities.add(list);
										count--;
									}
								}else{
									if (activityProgress.get(potentialActivity)<1.0){
										ArrayList<String> list = new ArrayList<String>();
										list.add(potentialActivity);
										list.add(String.valueOf((double) e.getValue()/100.0));
										list.add(approach);
										sortedActivities.add(list);
										count--;
									}
								}
							}
						}else{

							if (activityProgress.get(potentialActivity)<1.0){
								ArrayList<String> list = new ArrayList<String>();
								list.add(potentialActivity);
								list.add(String.valueOf((double) e.getValue()/100.0));
								list.add(approach);
								sortedActivities.add(list);
								count--;
							}
						}

					}
				}else{
					if (recommendBeforeLastSeenTopic){
						if (!queryAggregateDBInsteadofFile){
							if (afr.getTopicOrder(afr.getActivityTopic(potentialActivity, group))<=lastRecommendableTopic){
								ArrayList<String> list = new ArrayList<String>();
								list.add(potentialActivity);
								list.add(String.valueOf((double) e.getValue()/100.0));
								list.add(approach);
								sortedActivities.add(list);
								count--;
							}
						}else{
							//TODO: implement for the DB version
							System.out.println("Error: this part of the system has not been implemented for database ");
						}
					}else{
						ArrayList<String> list = new ArrayList<String>();
						list.add(potentialActivity);
						list.add(String.valueOf((double) e.getValue()/100.0));
						list.add(approach);
						sortedActivities.add(list);
						count--;
					}
				}
			}
		}else{
			if (recommendBeforeLastSeenTopic){
				HashMap<String,Double> activityProgress = aggregate_db.getActivityProgress(user, course);
				int lastRecommendableTopic = afr.getLastRecommendableTopic(activityProgress, group);
				HashMap<String, String> lastNActivities = getLastNActivitieswithTopics(user, noOfLastActivitiesNottoBeRecommended, group, domain, fastProviders,
						agg_dbstring, agg_dbuser, agg_dbpass,
						um2_dbstring, um2_dbuser, um2_dbpass,
						topicContentAddr,topicOrderAddr); 
				int count = proactive_max;
				while (!pq.isEmpty()) {
					if (count == 0){
						break;
					}
					Entry<String, Double> e = pq.poll();
					String potentialActivity = e.getKey();
					if (afr.getTopicOrder(afr.getActivityTopic(potentialActivity, group))<=lastRecommendableTopic){
						if (lastNActivities != null && !lastNActivities.isEmpty()){
							if (!lastNActivities.containsKey(potentialActivity)){
								ArrayList<String> list = new ArrayList<String>();
								list.add(potentialActivity);
								list.add(String.valueOf((double) e.getValue()/100.0));
								list.add(approach);
								sortedActivities.add(list);
								count--;
							}
						}else{
							ArrayList<String> list = new ArrayList<String>();
							list.add(potentialActivity);
							list.add(String.valueOf((double) e.getValue()/100.0));
							list.add(approach);
							sortedActivities.add(list);
							count--;
						}
					}
				}
			}else{
				HashMap<String, String> lastNActivities = getLastNActivitieswithTopics(user, noOfLastActivitiesNottoBeRecommended, group, domain, fastProviders,
						agg_dbstring, agg_dbuser, agg_dbpass,
						um2_dbstring, um2_dbuser, um2_dbpass,
						topicContentAddr,topicOrderAddr); 
				int count = proactive_max;
				while (!pq.isEmpty()) {
					if (count == 0){
						break;
					}
					Entry<String, Double> e = pq.poll();
					if (lastNActivities != null && !lastNActivities.isEmpty()){
						if (!lastNActivities.containsKey(e.getKey())){
							ArrayList<String> list = new ArrayList<String>();
							list.add(e.getKey());
							list.add(String.valueOf((double) e.getValue()/100.0));
							list.add(approach);
							sortedActivities.add(list);
							count--;
						}
					}else{
						ArrayList<String> list = new ArrayList<String>();
						list.add(e.getKey());
						list.add(String.valueOf((double) e.getValue()/100.0));
						list.add(approach);
						sortedActivities.add(list);
						count--;
					}
				}
			}
		}
		endTime   = System.currentTimeMillis();
		System.out.println("recommendation time: "+(endTime-startTime));
		closeAggregateDBConnections();
		afr = null;
		return sortedActivities;
	} 

	
	/**
	 * This is similar to optimizeall4qs function, except that it considers the ability of the student
	 * for consuming the potential activity, in addition to the probability that the student be able to solve
	 * all questions after taking that activity. For quizzes, the ability equals to the probability that the 
	 * student can solve this quiz with his/her current knowledge. For examples and animated examples activities, 
	 * the ability is an average of probability of solving all quizzes in the same topic as the potential activity.
	 *  
	 * @param user
	 * @param domain
	 * @param course
	 * @param group
	 * @param lastContentID
	 * @param contentList
	 * @param approach
	 * @param proactive_threshold
	 * @param proactive_max
	 * @param fastAPI
	 * @param agg_dbstring
	 * @param agg_dbuser
	 * @param agg_dbpass
	 * @param topicContentAddr
	 * @param topicOrderAddr
	 * @param um2_dbstring
	 * @param um2_dbuser
	 * @param um2_dbpass
	 * @return
	 */
	private static ArrayList<ArrayList<String>> optimize4allqsWithProbabilities(
			String user,  String domain,int course, String group, String lastContentID, String[] contentList,
			String approach, double proactive_threshold, 
			int proactive_max, API fastAPI,
			String agg_dbstring, String agg_dbuser, String agg_dbpass,
			String topicContentAddr,String topicOrderAddr,
			String um2_dbstring, String um2_dbuser, String um2_dbpass
	) {		

		long startTime = System.currentTimeMillis();

		HashMap<String, String> allQuestionsTopics = getAllQuestionswithTopics(group,agg_dbstring, agg_dbuser, agg_dbpass,topicContentAddr,topicOrderAddr); //all questions in the course with their topics
		HashMap<String, String> allActivitiesTopics = new HashMap<String, String>();
		ArrayList<ArrayList<String>> sortedActivities = new ArrayList<ArrayList<String>>();
		//TODO: define subgroup if it's needed
		String subgroup =null;
		Map<String, Double> activityProbability = new HashMap<String, Double>();
		Map<String, Double> questionSolvingProbabilityBeforeActivity = new HashMap<String, Double>();
		Map<String, Double> activityConsumptionProbability = new HashMap<String, Double>();
		Map<String, Double> finalActivityProbability = new HashMap<String, Double>();
		AggregateFileReader afr = new AggregateFileReader(topicContentAddr,topicOrderAddr);
		openAggregateDBConnections(agg_dbstring, agg_dbuser, agg_dbpass);
		
		for (String question : allQuestionsTopics.keySet()){
			HashMap<String, String> pastActivityToSkill = null;			
			questionSolvingProbabilityBeforeActivity.put(question, fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, null, null, pastActivityToSkill));
		}
		
		for (String activity :contentList){
			if (!allActivitiesTopics.containsKey(activity)){
				if (queryAggregateDBInsteadofFile){
					allActivitiesTopics.put(activity, aggregate_db.getActivityTopic(activity, group));
				}else{
					allActivitiesTopics.put(activity, afr.getActivityTopic(activity, group));
				}
			}
			String activityTopic = allActivitiesTopics.get(activity);
			//System.out.println("activity topic for " +activity + " is " + activityTopic);
			if (fastAcceptsAllSkills){
				for (String question : allQuestionsTopics.keySet()){
					if (activityProbability.containsKey(activity)){
						HashMap<String, String> pastActivityToSkill = null;
						Double prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
						activityProbability.put(activity, activityProbability.get(activity) + prob);
					}
					else{
						HashMap<String, String> pastActivityToSkill = null;
						Double prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
						activityProbability.put(activity,prob);
					}

				}
			}else{
				HashMap<String, String> pastActivityToSkill = null;
				String lastActivityTopic ="";
				if (lastContentID!= null && !lastContentID.isEmpty()){
					if (queryAggregateDBInsteadofFile){
						lastActivityTopic = aggregate_db.getActivityTopic(lastContentID, group);
					}else{
						lastActivityTopic = afr.getActivityTopic(lastContentID, group);
					}
				}
				HashMap<String, Double> previousProbabilities = fastAPI.getPrevQueProbForAct(user, domain,course, group, activity);
				for (String question : allQuestionsTopics.keySet()){
					Double prob = 0.0;
					String questionTopic = allQuestionsTopics.get(question);
					if (activityTopic.equals(questionTopic)){
						if (questionTopic.equals(lastActivityTopic)){
							prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
							fastAPI.putNewQuestionProbabilityinDB(user, domain,course, group, activity, question, prob);
						}else{
							if (previousProbabilities.containsKey(question)){
								prob = previousProbabilities.get(question);

							}else{
								prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
								fastAPI.putNewQuestionProbabilityinDB(user, domain,course, group, activity, question, prob);
							}
						}
					}else{
						if (previousProbabilities.containsKey(question)){
							prob = previousProbabilities.get(question);
						}else{
							prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
							fastAPI.putNewQuestionProbabilityinDB(user, domain,course, group, activity, question, prob);
						}
					}

					if (activityProbability.containsKey(activity)){
						activityProbability.put(activity, activityProbability.get(activity) + prob);
					}
					else{
						activityProbability.put(activity,prob);
					}

				} //for question
			} // for if fast accepts all skills
			
			// calculating the probability of this activity being consumed correctly by student
			Double prob = 0.0;
			if (allQuestionsTopics.keySet().contains(activity)){
				HashMap<String, String> pastActivityToSkill = null;
				prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(activity), activity, null, null, pastActivityToSkill); 
				activityConsumptionProbability.put(activity, prob);
			}else{
				int numberOfQuestionsInTopic = 0;
				for (String question : allQuestionsTopics.keySet()){
					String questionTopic = allQuestionsTopics.get(question);
					if (activityTopic.equals(questionTopic)){						
						prob =   prob + questionSolvingProbabilityBeforeActivity.get(question);
						numberOfQuestionsInTopic++;
					}
				}
				if (numberOfQuestionsInTopic>0){
					prob = prob / (double)numberOfQuestionsInTopic;
				}
				
				activityConsumptionProbability.put(activity, prob);
			}
			
			finalActivityProbability.put(activity, activityConsumptionProbability.get(activity)*activityProbability.get(activity));

		} //for activity
		
		

		//System.out.println("activityProbability size "+activityProbability.size());
		long endTime   = System.currentTimeMillis();
		//System.out.println("loop over activities and questions time: "+(endTime-startTime));
		//startTime = System.currentTimeMillis();
		PriorityQueue<Entry<String, Double>> pq = new PriorityQueue<Map.Entry<String,Double>>(finalActivityProbability.size(), new Comparator<Entry<String, Double>>() {
			@Override 
			public int compare(Entry<String, Double> arg0,
					Entry<String, Double> arg1) {
				return arg1.getValue().compareTo(arg0.getValue());
			}
		});

		pq.addAll(finalActivityProbability.entrySet());

		if (notRecommendCompleteProgress){
			if (aggregate_db==null){
				openAggregateDBConnections(agg_dbstring, agg_dbuser, agg_dbpass);
			}
			HashMap<String,Double> activityProgress = aggregate_db.getActivityProgress(user, course);
			int lastRecommendableTopic = afr.getLastRecommendableTopic(activityProgress, group);
			int count = proactive_max;
			while (!pq.isEmpty()) {
				if (count == 0){
					break;
				}
				Entry<String, Double> e = pq.poll();
				String potentialActivity = e.getKey();
				if (activityProgress != null && !activityProgress.isEmpty()){
					if (!activityProgress.containsKey(potentialActivity)){
						ArrayList<String> list = new ArrayList<String>();
						list.add(potentialActivity);
						list.add(String.valueOf((double) e.getValue()/100.0));
						list.add(approach);
						sortedActivities.add(list);
						count--;
					}else{
						//TODO: should write the code for fetching from database
						if (!queryAggregateDBInsteadofFile){
							if (recommendBeforeLastSeenTopic){
								if (afr.getTopicOrder(afr.getActivityTopic(potentialActivity, group))<=lastRecommendableTopic){
									HashMap<String, String> allExamples = afr.getAllofOneContentTypewithTopics(group, "webex");
									if (allExamples.containsKey(potentialActivity)){
										//for examples, the bar is lower 	
										if (activityProgress.get(potentialActivity)<exampleProgressConsideredAsComplete){
											ArrayList<String> list = new ArrayList<String>();
											list.add(potentialActivity);
											list.add(String.valueOf((double) e.getValue()/100.0));
											list.add(approach);
											sortedActivities.add(list);
											count--;
										}
									}else{
										if (activityProgress.get(potentialActivity)<1.0){
											ArrayList<String> list = new ArrayList<String>();
											list.add(potentialActivity);
											list.add(String.valueOf((double) e.getValue()/100.0));
											list.add(approach);
											sortedActivities.add(list);
											count--;
										}
									}
								}
							}else{
								HashMap<String, String> allExamples = afr.getAllofOneContentTypewithTopics(group, "webex");
								if (allExamples.containsKey(potentialActivity)){
									//for examples, the bar is lower 	
									if (activityProgress.get(potentialActivity)<exampleProgressConsideredAsComplete){
										ArrayList<String> list = new ArrayList<String>();
										list.add(potentialActivity);
										list.add(String.valueOf((double) e.getValue()/100.0));
										list.add(approach);
										sortedActivities.add(list);
										count--;
									}
								}else{
									if (activityProgress.get(potentialActivity)<1.0){
										ArrayList<String> list = new ArrayList<String>();
										list.add(potentialActivity);
										list.add(String.valueOf((double) e.getValue()/100.0));
										list.add(approach);
										sortedActivities.add(list);
										count--;
									}
								}
							}
						}else{

							if (activityProgress.get(potentialActivity)<1.0){
								ArrayList<String> list = new ArrayList<String>();
								list.add(potentialActivity);
								list.add(String.valueOf((double) e.getValue()/100.0));
								list.add(approach);
								sortedActivities.add(list);
								count--;
							}
						}

					}
				}else{
					if (recommendBeforeLastSeenTopic){
						if (!queryAggregateDBInsteadofFile){
							if (afr.getTopicOrder(afr.getActivityTopic(potentialActivity, group))<=lastRecommendableTopic){
								ArrayList<String> list = new ArrayList<String>();
								list.add(potentialActivity);
								list.add(String.valueOf((double) e.getValue()/100.0));
								list.add(approach);
								sortedActivities.add(list);
								count--;
							}
						}else{
							//TODO: implement for the DB version
							System.out.println("Error: this part of the system has not been implemented for database ");
						}
					}else{
						ArrayList<String> list = new ArrayList<String>();
						list.add(potentialActivity);
						list.add(String.valueOf((double) e.getValue()/100.0));
						list.add(approach);
						sortedActivities.add(list);
						count--;
					}
				}
			}
		}else{
			if (recommendBeforeLastSeenTopic){
				HashMap<String,Double> activityProgress = aggregate_db.getActivityProgress(user, course);
				int lastRecommendableTopic = afr.getLastRecommendableTopic(activityProgress, group);
				HashMap<String, String> lastNActivities = getLastNActivitieswithTopics(user, noOfLastActivitiesNottoBeRecommended, group, domain, fastProviders,
						agg_dbstring, agg_dbuser, agg_dbpass,
						um2_dbstring, um2_dbuser, um2_dbpass,
						topicContentAddr,topicOrderAddr); 
				int count = proactive_max;
				while (!pq.isEmpty()) {
					if (count == 0){
						break;
					}
					Entry<String, Double> e = pq.poll();
					String potentialActivity = e.getKey();
					if (afr.getTopicOrder(afr.getActivityTopic(potentialActivity, group))<=lastRecommendableTopic){
						if (lastNActivities != null && !lastNActivities.isEmpty()){
							if (!lastNActivities.containsKey(potentialActivity)){
								ArrayList<String> list = new ArrayList<String>();
								list.add(potentialActivity);
								list.add(String.valueOf((double) e.getValue()/100.0));
								list.add(approach);
								sortedActivities.add(list);
								count--;
							}
						}else{
							ArrayList<String> list = new ArrayList<String>();
							list.add(potentialActivity);
							list.add(String.valueOf((double) e.getValue()/100.0));
							list.add(approach);
							sortedActivities.add(list);
							count--;
						}
					}
				}
			}else{
				HashMap<String, String> lastNActivities = getLastNActivitieswithTopics(user, noOfLastActivitiesNottoBeRecommended, group, domain, fastProviders,
						agg_dbstring, agg_dbuser, agg_dbpass,
						um2_dbstring, um2_dbuser, um2_dbpass,
						topicContentAddr,topicOrderAddr); 
				int count = proactive_max;
				while (!pq.isEmpty()) {
					if (count == 0){
						break;
					}
					Entry<String, Double> e = pq.poll();
					if (lastNActivities != null && !lastNActivities.isEmpty()){
						if (!lastNActivities.containsKey(e.getKey())){
							ArrayList<String> list = new ArrayList<String>();
							list.add(e.getKey());
							list.add(String.valueOf((double) e.getValue()/100.0));
							list.add(approach);
							sortedActivities.add(list);
							count--;
						}
					}else{
						ArrayList<String> list = new ArrayList<String>();
						list.add(e.getKey());
						list.add(String.valueOf((double) e.getValue()/100.0));
						list.add(approach);
						sortedActivities.add(list);
						count--;
					}
				}
			}
		}
		endTime   = System.currentTimeMillis();
		System.out.println("recommendation time: "+(endTime-startTime));
		closeAggregateDBConnections();
		afr = null;
		return sortedActivities;
	} 

	
	private static void initializeFASTProbabilities(
			String user,  String domain,int course, String group, String[] contentList,
			String approach, double proactive_threshold, 
			int proactive_max, API fastAPI,
			String agg_dbstring, String agg_dbuser, String agg_dbpass,
			String topicContentAddr,String topicOrderAddr) {		

		//long startTime = System.currentTimeMillis();

		HashMap<String, String> allQuestionsTopics = getAllQuestionswithTopics(group,agg_dbstring, agg_dbuser, agg_dbpass,topicContentAddr, topicOrderAddr); //all questions in the course with their topics
		HashMap<String, String> allActivitiesTopics = new HashMap<String, String>();
		//TODO: define subgroup if it's needed
		String subgroup =null;
		AggregateFileReader afr = new AggregateFileReader(topicContentAddr,topicOrderAddr);
		openAggregateDBConnections(agg_dbstring, agg_dbuser, agg_dbpass);
		if (aggregate_db == null){
			System.out.println("aggregate_db is null");
		}
		for (String activity :contentList){
			if (!allActivitiesTopics.containsKey(activity)){
				if (queryAggregateDBInsteadofFile){
					allActivitiesTopics.put(activity, aggregate_db.getActivityTopic(activity, group));
				}else{
					allActivitiesTopics.put(activity, afr.getActivityTopic(activity, group));
				}
			}
			String activityTopic = allActivitiesTopics.get(activity);
			//System.out.println("activity topic for " +activity + " is " + activityTopic);
			for (String question : allQuestionsTopics.keySet()){

				HashMap<String, String> pastActivityToSkill = null;
				Double prob =   fastAPI.getProbQue(user, course, group, subgroup, allQuestionsTopics.get(question), question, activityTopic, activity, pastActivityToSkill);
				fastAPI.putNewQuestionProbabilityinDB(user, domain,course, group, activity, question, prob);
			}
		}
		closeAggregateDBConnections();
		afr = null;
		//long endTime   = System.currentTimeMillis();
		//System.out.println("loop over activities and questions time: "+(endTime-startTime));
	} 

	private static HashMap<String, String> getAllQuestionswithTopics(String group,String agg_dbstring, 
			String agg_dbuser, String agg_dbpass, String topicContentAddr,String topicOrderAddr) {
		HashMap<String, String> allQuestions = new HashMap<String, String>();
		String contentProvider = "quizjet";
		if (queryAggregateDBInsteadofFile){
			openAggregateDBConnections(agg_dbstring, agg_dbuser, agg_dbpass);
			allQuestions = aggregate_db.getAllofOneContentTypewithTopics(group, contentProvider);
			closeAggregateDBConnections();		
		}
		else{
			AggregateFileReader afr = new AggregateFileReader(topicContentAddr,topicOrderAddr);
			allQuestions = afr.getAllofOneContentTypewithTopics(group, contentProvider);
			afr = null;
		}
		return allQuestions;
	}

	/**
	 * @author Sherry
	 * @param user
	 * @param actNo number of last activities to be returned
	 * @param group
	 * @param domain
	 * @return  last activities of a user from specific providers and their topics calling both aggregate DB and UM2 DB
	 */
	private static HashMap<String, String> getLastNActivitieswithTopics(String user, int actNo, String group, String domain, String[] providers,
			String agg_dbstring, String agg_dbuser, String agg_dbpass,
			String um2_dbstring, String um2_dbuser, String um2_dbpass,
			String topicContentAddr,String topicOrderAddr) {
		HashMap<String, String> activitiesWithTopic = new HashMap<String, String>();
		openUM2DBConnections(um2_dbstring, um2_dbuser, um2_dbpass);
		String[] lastActivities = um2_db.getLastNActivities( user,  actNo,  group,  domain, providers);
		closeUM2DBConnections();
		if (lastActivities!=null && lastActivities.length>0){
			if (queryAggregateDBInsteadofFile){
				openAggregateDBConnections(agg_dbstring, agg_dbuser, agg_dbpass);
				for (String s:lastActivities){
					activitiesWithTopic.put(s, aggregate_db.getActivityTopic(s, group));
				}		
				closeAggregateDBConnections();
			}else{
				AggregateFileReader afr = new AggregateFileReader(topicContentAddr,topicOrderAddr);
				System.out.println("lastActivities size "+lastActivities.length);
				for (String s:lastActivities){	
					//System.out.println("activity "+ s);
					//System.out.println("group "+ group);
					//System.out.println("activity topic "+ afr.getActivityTopic(s, group));
					activitiesWithTopic.put(s, afr.getActivityTopic(s, group));
				}
				afr = null;
			}

		}else{
			System.out.println("No previous activities for user "+user +" in group "+ group);
		}
		return activitiesWithTopic;		
	}

	/**
	 * @author Sherry
	 * @param user
	 * @param domain
	 * @param group
	 * @param lastContentId
	 * @param lastContentResult
	 * @param lastContentProvider
	 * This function updates the knowledge states in FAST by sending the information of all learning activities of user to the model
	 * By learning activities, we mean quiz, examples, and animated examples (for now). It is hard-coded here, so it can be changed 
	 * if new learning resources get added.
	 * @param fastAPI 
	 */
	private static void updateFastKnowledge(String user, String domain,  int course, String group, String lastContentId,
			String lastContentResult, String lastContentProvider, int pastActivityNo,
			String[] providers, API fastAPI,
			String agg_dbstring, String agg_dbuser, String agg_dbpass,
			String um2_dbstring, String um2_dbuser, String um2_dbpass,
			String topicContentAddr,String topicOrderAddr) {
		//TODO: define subgroup if needed		
		String subgroup =null;
		if (lastContentProvider != null){ //Update only if it's a learning action
			if (lastContentProvider.toLowerCase().equals("quizjet") || 
					lastContentProvider.toLowerCase().equals("webex")  || 
					lastContentProvider.toLowerCase().equals("animatedexamples") ){
				int correctness = Integer.parseInt(lastContentResult);
				String activityTopic = "";
				if (queryAggregateDBInsteadofFile){
					openAggregateDBConnections(agg_dbstring, agg_dbuser, agg_dbpass);
					activityTopic = aggregate_db.getActivityTopic(lastContentId, group);
					closeAggregateDBConnections();
				}else{
					AggregateFileReader afr = new AggregateFileReader(topicContentAddr,topicOrderAddr);

					activityTopic = afr.getActivityTopic(lastContentId, group);
					afr = null;
				}
				if (fastAcceptsAllSkills){
					fastAPI.updateKnowledge(user, course, group, subgroup, correctness,
							activityTopic,lastContentId, 
							getLastNActivitieswithTopics(user,pastActivityNo,group,domain,providers,
									agg_dbstring, agg_dbuser, agg_dbpass,
									um2_dbstring, um2_dbuser, um2_dbpass,
									topicContentAddr,topicOrderAddr));
				}else{//find pastActivityNo of activities with the same skill as last activity within the last 100 activities
					HashMap<String, String> last100 = getLastNActivitieswithTopics( user,   100,  group,  domain, providers,
							agg_dbstring, agg_dbuser, agg_dbpass,
							um2_dbstring, um2_dbuser, um2_dbpass,
							topicContentAddr,topicOrderAddr);
					HashMap<String,String> lastNRelated = new HashMap<String, String>();
					int counter = 0;
					if (last100!= null && !last100.isEmpty()){
						Iterator<String> i = last100.keySet().iterator();
						while (i.hasNext()){
							String act = i.next();
							if (last100.get(act) != null && last100.get(act).equals(activityTopic)){
								lastNRelated.put(act, last100.get(act));
								counter++;
							}
							if (counter==pastActivityNo)
								break;																
						}
					}
					fastAPI.updateKnowledge(user, course, group, subgroup, correctness,activityTopic, lastContentId, lastNRelated);
				}
			}
		}
	}
}
