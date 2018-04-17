package com.easynetcn.sunlands.app;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ReportApplication {
	private static final Charset charset = Charset.forName("utf-8");
	private static final String LOGIN_URL = "http://exercise.sunlands.com/exercise/login/stuLogIn";
	private static final String PAPER_URL = "http://exercise.sunlands.com/exercise/student/retrievePaperByPaperId";
	private static final String EXEERCISE_URL = "http://exercise.sunlands.com/exercise/student/retrievePaperUserRecordBySequence";
	private static final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().cookieJar(new CookieJar() {
		@Override
		public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
			cookieStore.put(httpUrl.host(), list);
		}

		@Override
		public List<Cookie> loadForRequest(HttpUrl httpUrl) {
			List<Cookie> cookies = cookieStore.get(httpUrl.host());

			return cookies != null ? cookies : new ArrayList<>();
		}
	}).build();

	public static void main(String[] args) throws Exception {
		String username = args[0];
		String password = args[1];
		File srcFile = new File(args[2]);
		File destFile = new File(args[3]);

		login(username, password);

		try (FileWriter fileWriter = new FileWriter(destFile)) {
			Files.readLines(srcFile, charset).stream().filter(line -> null != line && !line.isEmpty()).forEach(line -> {
				try {
					String[] strs = line.split(" ");
					String code = strs[0];
					String url = strs[1];
					int paperId = Integer.parseInt(url.substring(url.lastIndexOf("/") + 1));

					Map paperMap = objectMapper.readValue(getPaperResult(paperId), Map.class);
					Map dataMap = (Map) paperMap.get("data");
					int questionAmount = (int) dataMap.get("questionAmount");
					int recordId = (int) dataMap.get("recordId");

					fileWriter.write(code);
					fileWriter.write("\n\n");

					for (int i = 1; i <= questionAmount; i++) {
						String word = getUnknownWord(getExerciseResult(paperId, recordId, i));

						if (null != word && !word.isEmpty()) {
							fileWriter.write(word);
							fileWriter.write("\n");
						}
					}

					fileWriter.write("\n\n");
				} catch (Exception e) {
					System.out.println(line);
				}

			});
		}
	}

	private static void login(String phone, String password) throws IOException {
		RequestBody body = new FormBody.Builder().add("phone", phone).add("password", password).build();
		Request request = new Request.Builder().url(LOGIN_URL).post(body).build();
		Call call = okHttpClient.newCall(request);

		call.execute();
	}

	private static String getPaperResult(int paperId) throws IOException {
		RequestBody body = new FormBody.Builder().add("paperId", String.valueOf(paperId)).add("source", "H5").build();
		Request request = new Request.Builder().url(PAPER_URL).post(body).build();
		Call call = okHttpClient.newCall(request);

		return call.execute().body().string();
	}

	private static String getExerciseResult(int paperId, int recordId, int nextQuestionSequence) throws IOException {
		RequestBody body = new FormBody.Builder().add("paperId", String.valueOf(paperId))
				.add("recordId", String.valueOf(recordId))
				.add("nextQuestionSequence", String.valueOf(nextQuestionSequence)).add("source", "H5").build();
		Request request = new Request.Builder().url(EXEERCISE_URL).post(body).build();
		Call call = okHttpClient.newCall(request);

		return call.execute().body().string();
	}

	private static String getUnknownWord(String response) throws IOException {
		String word = null;

		Map map = objectMapper.readValue(response, Map.class);
		Map dataMap = (Map) map.get("data");
		String correctAnswer = (String) dataMap.get("correctAnswer");
		String answer = (String) dataMap.get("answer");

		if (null != correctAnswer && null != answer && !correctAnswer.equalsIgnoreCase(answer)) {
			word = (String) dataMap.get("content");
		}

		return word;
	}
}
