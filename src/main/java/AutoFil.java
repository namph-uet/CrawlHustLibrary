import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.formula.functions.Count;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AutoFil {
	public static String DOMAIN = "https://0-ebookcentral-proquest-com.libopac.hust.edu.vn/lib/hustvn-ebooks/";
	public static String SEARCH_URL = DOMAIN + "search.action?query=";
	public static String LOGIN_URL = "https://libopac.hust.edu.vn/iii/cas/login";
	public static String FILE_PATH = "./file.xlsx";
	private Map cookie;

	public void auto() {
		login();
		updateExcelFile();
	}

	private void login() {
		try {
			Connection.Response  loginPageResponse = Jsoup.connect(LOGIN_URL).referrer(SEARCH_URL).userAgent("Mozilla/5.0")
					.validateTLSCertificates(false)
					.timeout(10 * 1000)
					.followRedirects(true)
					.execute();
			Document loginDoc = loginPageResponse.parse();
			Elements ltInput = loginDoc.select("input[name=lt]");
			String lt = ltInput.val();

			//get the cookies from the response, which we will post to the action URL
			this.cookie = loginPageResponse.cookies();

			//lets make data map containing all the parameters and its values found in the form
			Map<String, String> mapParams = new HashMap<String, String>();
			mapParams.put("extpatid", Main.USER_NAME);
			mapParams.put("extpatpw", Main.PASSWORD);
			mapParams.put("code", "");
			mapParams.put("pin", "");
			mapParams.put("lt", lt);
			mapParams.put("_eventId", "submit");

			Connection.Response responsePostLogin = Jsoup.connect(LOGIN_URL)
					.validateTLSCertificates(false)
					.userAgent("Mozilla/5.0")
					.timeout(10 * 1000)
					.data(mapParams)
					.cookies(this.cookie)
					.followRedirects(true)
					.execute();

			this.cookie = responsePostLogin.cookies();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private void updateExcelFile() {
		FileInputStream inputStream = null;
		try {
			inputStream = new FileInputStream(new File(FILE_PATH));
			Workbook workbook = WorkbookFactory.create(inputStream);
			Sheet sheet = workbook.getSheetAt(0);
			int totalRow = sheet.getLastRowNum();
			int totalThread = totalRow / 50 + 1;
			CountDownLatch countDownLatch = new CountDownLatch(totalThread);
			System.out.println(totalRow + " - " + totalThread);
			boolean header = false;
			for(int i = 0; i <= totalRow; i += 50) {
				if(i == 0) header = true;
				else header = false;
				ProcessThread processThread = new ProcessThread(workbook, header, i, i + 50, cookie, countDownLatch);
				processThread.start();
			}

			countDownLatch.await();

			FileOutputStream fileOut = null;
			fileOut = new FileOutputStream("UpdateXLS.xlsx");
			workbook.write(fileOut);
			fileOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
