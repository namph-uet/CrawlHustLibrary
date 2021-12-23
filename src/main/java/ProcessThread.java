import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.functions.Count;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ProcessThread extends Thread {
	public static String DOMAIN = "https://0-ebookcentral-proquest-com.libopac.hust.edu.vn/lib/hustvn-ebooks/";
	public static String SEARCH_URL = DOMAIN + "search.action?query=";
	private final CountDownLatch latch;

	private Workbook workbook;
	private Map cookie;
	private boolean header;
	private int start;
	private int end;

	public ProcessThread(Workbook workbook, boolean header, int start, int end, Map cookie, CountDownLatch latch) {
		this.workbook = workbook;
		this.header = header;
		this.start = start;
		this.end = end;
		this.cookie = cookie;
		this.latch = latch;
	}

	@Override
	public void run() {
		int rowCount = -1;
		Sheet sheet = workbook.getSheetAt(0);

		for (Row row : sheet) {
			rowCount++;
			if(rowCount < start) {
				continue;
			}
			if(rowCount >= end ) break;

			if(header) {
				header = false;
				continue;
			}

			System.out.println(rowCount);
			String elsbn = null;
			String href = null;
			String dewey = null;

			Cell checkHaveElsbn = row.getCell(3);
			if(!checkHaveElsbn.getStringCellValue().isEmpty()) continue;

			elsbn = row.getCell(1).getStringCellValue();
			href = getDetailPage(elsbn);
			dewey = getDeweyLink(href);

			row.getCell(2).setCellValue(DOMAIN + href);
			row.getCell(3).setCellValue(dewey);
		}

		latch.countDown();

		// Write the output to a file
//		FileOutputStream fileOut = null;
//		try {
//			fileOut = new FileOutputStream("UpdateXLS_" + start +"_" + end + ".xlsx");
//			workbook.write(fileOut);
//			fileOut.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	private String getDetailPage(String elsbn) {
		String url = SEARCH_URL + elsbn;
		Connection.Response detailPage = null;
		Document document = null;
		try {
			detailPage = Jsoup.connect(url)
					.validateTLSCertificates(false)
					.userAgent("Mozilla/5.0")
					.timeout(20 * 1000)
					.cookies(this.cookie)
					.followRedirects(true)
					.execute();

			document = detailPage.parse();
		} catch (IOException e) {
			System.out.println("error find: " + elsbn);
			return getDetailPage(elsbn);
		}
		Elements detailATag = document.select("div.pub-list-item-description").select("a");
		return detailATag.attr("href");
	}

	private String getDeweyLink(String detailLink) {
		String url = DOMAIN + detailLink;
		Connection.Response detailPage = null;
		Document document = null;
		try {
			detailPage = Jsoup.connect(url)
					.validateTLSCertificates(false)
					.userAgent("Mozilla/5.0")
					.timeout(20 * 1000)
					.cookies(this.cookie)
					.followRedirects(true)
					.maxBodySize(0)
					.execute();

			document = detailPage.parse();

		} catch (IOException e) {
			System.out.println("error find deway of: " + detailLink);
			return getDeweyLink(detailLink);
		}

		Elements detailATag = document.select("a#deweyLink");
		return detailATag.text();
	}
}
