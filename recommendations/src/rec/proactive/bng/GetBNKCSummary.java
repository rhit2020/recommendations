package rec.proactive.bng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetBNKCSummary {

	public static final String server = "http://adapt2.sis.pitt.edu";
	public static final String bnServiceURL = server + "/bn_general/StudentModelCache";

	public static HashMap<String, Double> getItemKCEstimates(String usr, String grp, String lastAct, String lastActRes,
			String[] contentList, String updatesm) {

		HashMap<String, Double> itemKCEstimates = new HashMap<String, Double>();

		try {
			//String str = "{\"item-kc-estimates\":[{\"p\":0.3907,\"name\":\"IfElseIfStatement\"},{\"p\":0.6578,\"name\":\"False\"},{\"p\":0.5518,\"name\":\"java.lang.String.equalsIgnoreCase\"},{\"p\":0.2819,\"name\":\"FormalMethodParameter\"},{\"p\":0.9285,\"name\":\"NotEqualExpression\"},{\"p\":0.8925,\"name\":\"IfStatement\"},{\"p\":0.9737,\"name\":\"AndExpression\"},{\"p\":0.4223,\"name\":\"java.lang.Math.round\"},{\"p\":0.8976,\"name\":\"ArrayVariable\"},{\"p\":0.8531,\"name\":\"ReturnStatement\"},{\"p\":0.5638,\"name\":\"java.util.ArrayList.size\"},{\"p\":0.2655,\"name\":\"java.lang.String.equals\"},{\"p\":0.3993,\"name\":\"java.lang.Double.parseDouble\"},{\"p\":0.9244,\"name\":\"AddAssignmentExpression\"},{\"p\":0.0766,\"name\":\"ExtendsSpecification\"},{\"p\":0.7171,\"name\":\"ModulusExpression\"},{\"p\":0.2375,\"name\":\"java.lang.System.out.println\"},{\"p\":0.1916,\"name\":\"GenericObjectCreationStatement\"},{\"p\":0.9477,\"name\":\"ArrayInitializationStatement\"},{\"p\":0.1918,\"name\":\"DivideExpression\"},{\"p\":0.485,\"name\":\"java.lang.String.length\"},{\"p\":0.381,\"name\":\"InstanceFieldInvocation\"},{\"p\":0.1478,\"name\":\"StringLiteral\"},{\"p\":0.0276,\"name\":\"AddExpression\"},{\"p\":0.1498,\"name\":\"ActualMethodParameter\"},{\"p\":0.366,\"name\":\"PrivateFieldSpecifier\"},{\"p\":0.6559,\"name\":\"EqualExpression\"},{\"p\":0.2056,\"name\":\"ConstructorDefinition\"},{\"p\":0.3584,\"name\":\"StringAddition\"},{\"p\":0.1062,\"name\":\"WhileStatement\"},{\"p\":0.1916,\"name\":\"java.util.ArrayList\"},{\"p\":0.6182,\"name\":\"SubtractExpression\"},{\"p\":0.3073,\"name\":\"java.lang.String.charAt\"},{\"p\":0.9532,\"name\":\"ForStatement\"},{\"p\":0.9868,\"name\":\"BooleanDataType\"},{\"p\":0.7164,\"name\":\"InstanceFieldInitializationStatement\"},{\"p\":0.8487,\"name\":\"PostIncrementExpression\"},{\"p\":0.3216,\"name\":\"PublicClassSpecifier\"},{\"p\":0.767,\"name\":\"MultiplyExpression\"},{\"p\":0.7027,\"name\":\"TryCatchStatement\"},{\"p\":0.8475,\"name\":\"StringVariable\"},{\"p\":0.381,\"name\":\"ThisReference\"},{\"p\":0.1858,\"name\":\"PublicConstructorSpecifier\"},{\"p\":0.9371,\"name\":\"java.lang.Math.sqrt\"},{\"p\":0.5518,\"name\":\"java.lang.String.replace\"},{\"p\":0.5042,\"name\":\"java.lang.String.substring\"},{\"p\":0.8757,\"name\":\"StringInitializationStatement\"},{\"p\":0.283,\"name\":\"OverridingToString\"},{\"p\":0.5815,\"name\":\"NestedForLoops\"},{\"p\":0.4442,\"name\":\"GreaterEqualExpression\"},{\"p\":0.3119,\"name\":\"True\"},{\"p\":0.9941,\"name\":\"ImportStatement\"},{\"p\":0.9812,\"name\":\"OrExpression\"},{\"p\":0.2104,\"name\":\"SuperReference\"},{\"p\":0.4197,\"name\":\"FinalFieldSpecifier\"},{\"p\":0.6777,\"name\":\"LessEqualExpression\"},{\"p\":0.1157,\"name\":\"StringDataType\"},{\"p\":0.4306,\"name\":\"InstanceField\"},{\"p\":0.7248,\"name\":\"ArrayElement\"},{\"p\":0.9264,\"name\":\"ForEachStatement\"},{\"p\":0.4816,\"name\":\"PreDecrementExpression\"},{\"p\":0.8931,\"name\":\"ArrayLength\"},{\"p\":0.651,\"name\":\"CharDataType\"},{\"p\":0.4156,\"name\":\"nullInitialization\"},{\"p\":0.952,\"name\":\"java.lang.Math.pow\"},{\"p\":0.5638,\"name\":\"ThrowsSpecification\"},{\"p\":0.775,\"name\":\"ExplicitTypeCasting\"},{\"p\":0.0766,\"name\":\"SuperclassConstructorCall\"},{\"p\":0.4156,\"name\":\"null\"},{\"p\":0.8471,\"name\":\"MinusAssignmentExpression\"},{\"p\":0.496,\"name\":\"IntDataType\"},{\"p\":0.4163,\"name\":\"LongDataType\"},{\"p\":0.931,\"name\":\"ObjectMethodInvocation\"},{\"p\":0.5123,\"name\":\"ArrayDataType\"},{\"p\":0.4811,\"name\":\"OnDemandImport\"},{\"p\":0.4816,\"name\":\"PreIncrementExpression\"},{\"p\":0.5304,\"name\":\"DoStatement\"},{\"p\":0.9067,\"name\":\"Constant\"},{\"p\":0.8883,\"name\":\"NotExpression\"},{\"p\":0.4801,\"name\":\"MultiDimensionalArrayDataType\"},{\"p\":0.9343,\"name\":\"ArrayCreationStatement\"},{\"p\":0.9836,\"name\":\"LessExpression\"},{\"p\":0.9067,\"name\":\"ConstantInitializationStatement\"},{\"p\":0.2104,\"name\":\"SuperclassMethodCall\"},{\"p\":0.0794,\"name\":\"MethodDefinition\"},{\"p\":0.9696,\"name\":\"GreaterExpression\"},{\"p\":0.6997,\"name\":\"XORExpression\"},{\"p\":0.6945,\"name\":\"PostDecrementExpression\"},{\"p\":0.951,\"name\":\"SimpleVariable\"},{\"p\":0.0794,\"name\":\"PublicMethodSpecifier\"},{\"p\":0.1916,\"name\":\"java.util.ArrayList.add\"},{\"p\":0.9266,\"name\":\"IfElseStatement\"},{\"p\":0.7072,\"name\":\"java.lang.Integer.parseInt\"},{\"p\":0.8134,\"name\":\"java.lang.System.out.print\"},{\"p\":0.36,\"name\":\"VoidDataType\"},{\"p\":0.8765,\"name\":\"DoubleDataType\"},{\"p\":0.9092,\"name\":\"ArrayInitializer\"},{\"p\":0.4933,\"name\":\"PythagoreanTheorem2\"},{\"p\":0.3712,\"name\":\"in_order\"},{\"p\":0.5392,\"name\":\"JAverageEvenNums\"},{\"p\":0.1497,\"name\":\"end_characters\"},{\"p\":0.2071,\"name\":\"first_half\"},{\"p\":0.3249,\"name\":\"Transactions2\"},{\"p\":0.6256,\"name\":\"JForThree2\"},{\"p\":0.4166,\"name\":\"JIfElseWages2\"},{\"p\":0.5812,\"name\":\"JNestedIfTemperature2\"},{\"p\":0.4302,\"name\":\"is_special\"},{\"p\":0.4365,\"name\":\"FahrenheitToCelsius\"},{\"p\":0.4972,\"name\":\"JThreeBoolean3\"},{\"p\":0.2229,\"name\":\"JThreeBoolean2\"},{\"p\":0.8084,\"name\":\"ifElseOddEven\"},{\"p\":0.8257,\"name\":\"JNestedIfMaxOfThree\"},{\"p\":0.0641,\"name\":\"love6\"},{\"p\":0.0086,\"name\":\"inheritance_2\"},{\"p\":0.2382,\"name\":\"JWinPercentageInput\"},{\"p\":0.5781,\"name\":\"JWriteSquaresRange\"},{\"p\":0.0647,\"name\":\"inheritance_1\"},{\"p\":0.7233,\"name\":\"JCharAt2\"},{\"p\":0.5441,\"name\":\"days_to_week_conversion\"},{\"p\":0.2197,\"name\":\"greenTicket\"},{\"p\":0.5054,\"name\":\"compute_average\"},{\"p\":0.3912,\"name\":\"StringAddition2\"},{\"p\":0.2084,\"name\":\"JWorkHours2\"},{\"p\":0.6418,\"name\":\"JArrayMin\"},{\"p\":0.1857,\"name\":\"for1_coding\"},{\"p\":0.4411,\"name\":\"JStringEqual2\"},{\"p\":0.6207,\"name\":\"JPhoneAge2\"},{\"p\":0.7699,\"name\":\"JArrayBasic2\"},{\"p\":0.473,\"name\":\"rectangle_perimeter\"},{\"p\":0.7539,\"name\":\"JArrayBasic3\"},{\"p\":0.106,\"name\":\"while4_coding\"},{\"p\":0.2252,\"name\":\"JSearchArrayCountsEach\"},{\"p\":0.1362,\"name\":\"while3_coding\"},{\"p\":0.3509,\"name\":\"for4_coding\"},{\"p\":0.4094,\"name\":\"for3_coding\"},{\"p\":0.299,\"name\":\"JEscapeChar2\"},{\"p\":0.057,\"name\":\"percentage_correctness\"},{\"p\":0.2461,\"name\":\"JArrayRotateRightTwice\"},{\"p\":0.1169,\"name\":\"JEscapeChar3\"},{\"p\":0.043,\"name\":\"in1To10\"},{\"p\":0.393,\"name\":\"squirrel_play\"},{\"p\":0.8408,\"name\":\"JBooleanDryHot3\"},{\"p\":0.9335,\"name\":\"JBooleanDryHot2\"},{\"p\":0.2595,\"name\":\"JReverseNumber\"},{\"p\":0.194,\"name\":\"JStars2\"},{\"p\":0.5683,\"name\":\"JBooleanDryHot4\"},{\"p\":0.5552,\"name\":\"JAverageArrayElements\"},{\"p\":0.1143,\"name\":\"PointTester2\"},{\"p\":0.1418,\"name\":\"nested_loops_1\"},{\"p\":0.5561,\"name\":\"VendingMachine2\"},{\"p\":0.2755,\"name\":\"JInputStat2\"},{\"p\":0.0912,\"name\":\"nested_loops_2\"},{\"p\":0.1351,\"name\":\"JCheckAge2\"},{\"p\":0.6983,\"name\":\"JIfElseIfGrade2\"},{\"p\":0.1601,\"name\":\"JPrintMedalsRowColumnTotal\"},{\"p\":0.3455,\"name\":\"JAdjacentConsecutives\"},{\"p\":0.7988,\"name\":\"JWriteSquaresOdd\"},{\"p\":0.6227,\"name\":\"JSearchArrayTotalCounts\"},{\"p\":0.2141,\"name\":\"JCheckProductCode2\"},{\"p\":0.0101,\"name\":\"without2\"},{\"p\":0.474,\"name\":\"object_classes_2\"},{\"p\":0.3632,\"name\":\"object_classes_1\"},{\"p\":0.2221,\"name\":\"object_classes_4\"},{\"p\":0.2407,\"name\":\"object_classes_3\"},{\"p\":0.3735,\"name\":\"InheritancePointTester2\"},{\"p\":0.2358,\"name\":\"arrays_1\"},{\"p\":0.0399,\"name\":\"arrays_2\"},{\"p\":0.0984,\"name\":\"arrays_3\"},{\"p\":0.3986,\"name\":\"JArrays2dBasic2\"},{\"p\":0.6328,\"name\":\"JArrays2dBasic3\"},{\"p\":0.2683,\"name\":\"AnimalTester2\"},{\"p\":0.5299,\"name\":\"JAdjacentGreater\"},{\"p\":0.0475,\"name\":\"first_last_swap\"},{\"p\":0.2919,\"name\":\"JDecInc2\"},{\"p\":0.2214,\"name\":\"JDecInc3\"},{\"p\":0.2428,\"name\":\"pcrs_2d_arrays_1\"},{\"p\":0.1697,\"name\":\"pcrs_2d_arrays_2\"},{\"p\":0.3773,\"name\":\"twoAsOne\"},{\"p\":0.6423,\"name\":\"JForOne2\"},{\"p\":0.4084,\"name\":\"JRepeatedSequence2\"},{\"p\":0.3083,\"name\":\"JSodaSurverySodaAvg\"},{\"p\":0.5309,\"name\":\"pcrs_2d_arrays_3\"},{\"p\":0.3598,\"name\":\"BmiCalculator2\"},{\"p\":0.6497,\"name\":\"JSumDigits\"},{\"p\":0.0965,\"name\":\"while5_coding\"},{\"p\":0.3437,\"name\":\"JLargestDivisor\"},{\"p\":0.4063,\"name\":\"make_out_word\"},{\"p\":0.7577,\"name\":\"JForTwo2\"},{\"p\":0.8165,\"name\":\"JFailCourse3\"},{\"p\":0.0319,\"name\":\"while1_coding\"},{\"p\":0.1018,\"name\":\"while2_coding\"},{\"p\":0.3245,\"name\":\"JArrayRotateLeftTwice\"},{\"p\":0.4142,\"name\":\"JAverageDouble\"},{\"p\":0.2765,\"name\":\"repeat_last_char\"},{\"p\":0.7296,\"name\":\"JRentCar2\"},{\"p\":0.2596,\"name\":\"JArrayRotateRight\"},{\"p\":0.8895,\"name\":\"JFailCourse2\"},{\"p\":0.5088,\"name\":\"JRentCar3\"},{\"p\":0.2494,\"name\":\"JTemperatureListDaysAboveThreshold\"},{\"p\":0.3335,\"name\":\"Initials2\"},{\"p\":0.5354,\"name\":\"JVocabulary2\"},{\"p\":0.2686,\"name\":\"sortaSum\"},{\"p\":0.7406,\"name\":\"Initials3\"},{\"p\":0.012,\"name\":\"array_lst_1\"},{\"p\":0.2813,\"name\":\"non_start\"},{\"p\":0.1718,\"name\":\"check_start_end_character\"},{\"p\":0.2669,\"name\":\"second_in_days\"},{\"p\":0.0801,\"name\":\"DisplayTime2\"},{\"p\":0.425,\"name\":\"JInput3\"},{\"p\":0.74,\"name\":\"JInput2\"},{\"p\":0.6165,\"name\":\"JArrayFillUserInput\"},{\"p\":0.3733,\"name\":\"JSodaSurverySodaRespondentAvg\"},{\"p\":0.1832,\"name\":\"JInput4\"},{\"p\":0.4438,\"name\":\"for2_coding\"},{\"p\":0.0965,\"name\":\"array_4\"},{\"p\":0.2163,\"name\":\"JArraySwapAdjacentElements\"},{\"p\":0.4484,\"name\":\"TVTester2\"},{\"p\":0.2048,\"name\":\"JWinPercentageWonEqual\"},{\"p\":0.4129,\"name\":\"LoanTester2\"}]}";
			//JSONObject json = new JSONObject(str);
			
			JSONObject json = callService(bnServiceURL, usr, grp, lastAct, lastActRes,
					                      contentList, updatesm);

			if (json.has("error")) {
				System.out.println("Error:[" + json.getString("errorMsg") + "]");
			} else {
				JSONArray activity = json.getJSONArray("item-kc-estimates");

				String name;
				double probability;
				for (int i = 0; i < activity.length(); i++) {
					JSONObject jsonobj = activity.getJSONObject(i);
					name = jsonobj.getString("name");
					probability = jsonobj.getDouble("p");
					itemKCEstimates.put(name, probability);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return itemKCEstimates;
	}

	private static JSONObject callService(String url, String usr, String grp, String lastAct, String lastActRes,
			String[] contentList, String updatesm) {
		InputStream in = null;
		JSONObject jsonResponse = null;
		// A JSON object is created to pass the required parameter to the
		// recommendation service implemented by GetRecommendations.java
		try {
			HttpClient client = new HttpClient();
			PostMethod method = new PostMethod(url);
			method.addParameter("usr", URLEncoder.encode(usr, "UTF-8"));
			method.addParameter("grp", URLEncoder.encode(grp, "UTF-8"));
			method.addParameter("lastContentId", URLEncoder.encode(lastAct, "UTF-8"));
			method.addParameter("lastContentResult", URLEncoder.encode(lastActRes, "UTF-8"));
			method.addParameter("contents", getContents(contentList));
			method.addParameter("updatesm", (updatesm == null ? "false" : updatesm));
			int statusCode = client.executeMethod(method);

			if (statusCode != -1) {

				in = method.getResponseBodyAsStream();
				jsonResponse = readJsonFromStream(in);
				in.close();
			} else {

			}
		} catch (Exception e) {
			System.out.println("GetBNKSummary: error in getting estimates from user model.");
			e.printStackTrace();
		}
		return jsonResponse;
	}

	private static String getContents(String[] contentList) {
		String contents = "";
		for (String c : contentList)
			contents += c + ",";
		if (contents.length() > 0)
			contents = contents.substring(0, contents.length() - 1); // this is
																		// for
																		// ignoring
																		// the
																		// last
																		// ,
		return contents;
	}

	public static JSONObject readJsonFromStream(InputStream is) throws Exception {
		JSONObject json = null;
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
//			System.out.println(jsonText);
			json = new JSONObject(jsonText);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return json;
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

}
