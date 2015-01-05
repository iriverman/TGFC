package com.linangran.tgfcapp.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import com.linangran.tgfcapp.data.ContentListItemData;
import com.linangran.tgfcapp.data.ContentListPageData;
import com.linangran.tgfcapp.data.ForumListItemData;
import com.linangran.tgfcapp.data.HttpResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParamBean;
import org.apache.http.params.HttpConnectionParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by linangran on 3/1/15.
 */
public class NetworkUtils
{
	private static Context applicationContext;
	private static String userAgent = null;
	private static BasicHttpParams httpParams = null;


	public static void init(Context context)
	{
		applicationContext = context;
		httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
		HttpConnectionParams.setSoTimeout(httpParams, 15000);
	}



	private static String getUserAgent()
	{
		if (userAgent == null)
		{
			String base = "Mozilla/5.0 (Linux; Android [AndroidVersion]; [DeviceName] Build/[BuildName]) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.93 Mobile Safari/537.36";
			try
			{
				PackageInfo packageInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
				userAgent = base.replace("[AndroidVersion]", packageInfo.versionName).replace("[BuildName]", Build.VERSION.RELEASE).replace("[DeviceName]", Build.MODEL);
			}
			catch (PackageManager.NameNotFoundException e)
			{
				e.printStackTrace();
			}
		}
		return userAgent;
	}

	private static HttpResponse httpRequest(String url, String referer, List<NameValuePair> getParams, List<NameValuePair> postParams)
	{
		try
		{
			HttpClient httpClient = new DefaultHttpClient(httpParams);
			String structuredURL = url;
			if (getParams != null && getParams.size() > 0)
			{
				StringBuilder urlBuilder = new StringBuilder(url);
				for (int i = 0; i < getParams.size(); i++)
				{
					NameValuePair pair = getParams.get(i);
					if (i == 0)
					{
						urlBuilder.append("?");
					}
					else
					{
						urlBuilder.append("&");
					}
					urlBuilder.append(pair.getName());
					urlBuilder.append("=");
					urlBuilder.append(pair.getValue());
				}
				structuredURL = urlBuilder.toString();
			}
			HttpPost httpPost = new HttpPost(structuredURL);
			if (postParams != null && postParams.size() > 0)
			{
				httpPost.setEntity(new UrlEncodedFormEntity(postParams));
			}
			if (referer != null)
			{
				httpPost.addHeader("Referer", referer);
			}
			HttpResponse response = httpClient.execute(httpPost);
			return response;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private static HttpResponse httpGet(String url, String referer, String... args)
	{
		List<NameValuePair> getParams = new ArrayList<NameValuePair>();
		if (args.length % 2 != 0)
		{
			return null;
		}
		else
		{
			for (int i = 0; i < args.length; i += 2)
			{
				NameValuePair pair = new BasicNameValuePair(args[i], args[i + 1]);
				getParams.add(pair);
			}
			return httpRequest(url, referer, getParams, null);
		}
	}

	private static HttpResult<String> httpGetString(String url, String referer, String... args)
	{
		HttpResponse response = httpGet(url, referer, args);
		HttpResult<String> result = new HttpResult<String>();
		if (response == null || response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			result.hasError = true;
			if (response != null)
			{
				result.errorInfo = response.getStatusLine().getReasonPhrase();
			}
			else
			{
				result.errorInfo = "网络连接错误";
			}
		}
		else
		{
			String encoding = "UTF-8";
			if (response.getEntity().getContentEncoding() != null && response.getEntity().getContentEncoding().getValue().length() != 0)
			{
				encoding = response.getEntity().getContentEncoding().getValue();
			}
			try
			{
				result.result = IOUtils.toString(response.getEntity().getContent(), encoding);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				result.setErrorInfo(e.toString());
			}
		}
		return result;
	}

	public static HttpResult<List<ForumListItemData>> getForumList(int fid, int page)
	{
		String url = APIURL.WAP_VIEW_FORUM_URL + fid + "&page=" + page;
		HttpResult<String> stringResult = httpGetString(url, APIURL.WAP_API_URL);
		HttpResult<List<ForumListItemData>> listResult = new HttpResult<List<ForumListItemData>>(stringResult);
		if (stringResult.hasError == false)
		{
			String html = StringEscapeUtils.unescapeHtml(stringResult.result);
			if (html.contains("<div>无权访问本版块</div>"))
			{
				listResult.setErrorInfo("无权访问本版块");
				return listResult;
			}
			else if (html.contains("<title>TGFC 手机版</title>"))
			{
				listResult.setErrorInfo("版面不存在");
				return listResult;
			}

			Pattern pattern = Pattern.compile("<span class=\"title\">(<b>)?<a href=\"([^\"]+)\">([^<]+)<\\/a>(<\\/b>)?<\\/span>(?:<span class=\"paging\">.*<\\/span>)?<br \\/>\\s*<span class=\"author\">\\[([^\\/]+)\\/(\\d+)\\/(\\d+)\\/([^\\]]+)]<\\/span>");
			Matcher matcher = pattern.matcher(html);
			List<ForumListItemData> dataList = new ArrayList<ForumListItemData>();
			while (matcher.find())
			{
				ForumListItemData line = new ForumListItemData();
				String topFlag = matcher.group(1);
				String itemURL = matcher.group(2);
				String title = matcher.group(3);
				String posterName = matcher.group(5);
				String replyCount = matcher.group(6);
				String readCount = matcher.group(7);
				String replierName = matcher.group(8);
				line.isTopped = topFlag == null;
				line.tid = getTidFromURL(itemURL);
				line.title = title;
				line.posterName = posterName;
				line.replierName = replierName;
				line.readCount = Integer.parseInt(readCount);
				line.replyCount = Integer.parseInt(replyCount);
				dataList.add(line);
			}
			listResult.result = dataList;
		}
		return listResult;
	}

	private static int getTidFromURL(String url)
	{
		Pattern pattern = Pattern.compile("index.php\\?action=thread&tid=(\\d+)");
		Matcher matcher = pattern.matcher(url);
		if (matcher.find())
		{
			return Integer.parseInt(matcher.group(1));
		}
		else
		{
			return -1;
		}
	}

	public static HttpResult<ContentListPageData> getContentList(int tid, int page)
	{
		String url = APIURL.WAP_VIEW_CONTENT_URL + tid + "&page=" + page;
		HttpResult<String> stringResult = httpGetString(url, APIURL.WAP_API_URL);
		HttpResult<ContentListPageData> contentResult = new HttpResult<ContentListPageData>(stringResult);
		ContentListPageData pageData = new ContentListPageData();
		if (stringResult.hasError == false)
		{
			String html = StringEscapeUtils.unescapeHtml(stringResult.result);
			if (html.contains("<div>指定主题不存在</div>"))
			{
				contentResult.setErrorInfo("指定的主题不存在");
				return contentResult;
			}
			else if (html.contains("<div>无权查看本主题</div>"))
			{
				contentResult.setErrorInfo("无权查看本主题");
				return contentResult;
			}
			else
			{
				generalContentParser(html, pageData, tid);
				contentListParser(html, pageData, tid);
				contentResult.setResult(pageData);
				return contentResult;
			}
		}
		else
		{
			return contentResult;
		}

	}

	public static void contentListParser(String html, ContentListPageData pageData, int tid)
	{
		List<ContentListItemData> dataList = pageData.dataList;
		Document htmlDoc = Jsoup.parse(html);
		Elements messageElements = htmlDoc.select(".message");
		Elements infobarElements = htmlDoc.select(".infobar");
		int messageStart = 0;
		Pattern mainPostPattern = Pattern.compile("标题:<b>(.+)<\\/b><br \\/>时间:(.+)<br \\/>作者:<a href=\".+uid=(\\d+).*\">(.+)<\\/a>");
		Matcher mainPostMatcher = mainPostPattern.matcher(html);
		if (mainPostMatcher.find())
		{
			messageStart++;
			ContentListItemData itemData = new ContentListItemData();
			itemData.floorNum = 1;
			itemData.posterTime = mainPostMatcher.group(2);
			itemData.posterUID = Integer.parseInt(mainPostMatcher.group(3));
			itemData.posterName = mainPostMatcher.group(4);
			Pattern ratingPattern = Pattern.compile("评分记录\\( <b>(\\d+)<\\/b>");
			Matcher ratingMatcher = ratingPattern.matcher(html);
			if (ratingMatcher.find())
			{
				itemData.ratings = Integer.parseInt(ratingMatcher.group(1));
			}
			Pattern pidPattern = Pattern.compile("收藏<\\/a> \\| <a href=\".*?pid=(\\d+).*?\">评分<\\/a>");
			Matcher pidMatcher = pidPattern.matcher(html);
			if (pidMatcher.find())
			{
				itemData.pid = Integer.parseInt(pidMatcher.group(1));
			}
			dataList.add(itemData);
		}
		for (int i = messageStart, j = 0; i < messageElements.size(); i++, j++)
		{
			ContentListItemData itemData = new ContentListItemData();
			Element msgElement = messageElements.get(i);
			Element barElement = infobarElements.get(j);
			String infoString = StringEscapeUtils.unescapeHtml(barElement.html());
			Pattern barPattern = Pattern.compile("<a href=\".*?pid=(\\d+).*?>#(\\d+)[\\s\\S]*?<a href=\".*?uid=(\\d+).*?>(.+?)<\\/a>[\\s\\S]*?骚\\((\\d+)\\)[\\s\\S]*?<span class=\"nf\"> (.*)<\\/span>");
			Matcher barMatcher = barPattern.matcher(infoString);
			if (barMatcher.find())
			{
				itemData.pid = Integer.parseInt(barMatcher.group(1));
				itemData.floorNum = Integer.parseInt(barMatcher.group(2));
				itemData.posterUID = Integer.parseInt(barMatcher.group(3));
				itemData.posterName = barMatcher.group(4);
				itemData.ratings = Integer.parseInt(barMatcher.group(5));
				itemData.posterTime = barMatcher.group(6);
			}
			Elements quotedElements = msgElement.select(".quote-bd");
			if (quotedElements.size() > 0)
			{
				Element quoteInfoElement = quotedElements.get(0);
				itemData.quotedInfo = quoteInfoElement.children().get(0).toString();
				itemData.quotedText = quoteInfoElement.children().get(2).html();
			}
			msgElement.select(".ui-topic-content fn-break").remove();
			itemData.mainText = msgElement.html();
			dataList.add(itemData);
		}
	}

	public static void generalContentParser(String html, ContentListPageData pageData, int tid)
	{
		pageData.tid = tid;
		Pattern titlePattern = Pattern.compile("<title>(.+)-TGFC 手机版<\\/title>");
		Matcher titleMatcher = titlePattern.matcher(html);
		if (titleMatcher.find())
		{
			pageData.title = titleMatcher.group(1);
		}
		Pattern pageInfoPattern = Pattern.compile("<\\/a>\\s*\\((\\d+)\\/(\\d+)页\\)<\\/span>");
		Matcher pageInfoMatcher = pageInfoPattern.matcher(html);
		if (pageInfoMatcher.find())
		{
			pageData.currentPage = Integer.parseInt(pageInfoMatcher.group(1));
			pageData.totalPageCount = Integer.parseInt(pageInfoMatcher.group(2));
		}
		if (html.contains("[此主题已关闭]"))
		{
			pageData.isClosed = true;
		}
		Pattern replyCountPattern = Pattern.compile("<b>回复列表 (\\d+)<\\/b>");
		Matcher replyCountMatcher = replyCountPattern.matcher(html);
		if (replyCountMatcher.find())
		{
			pageData.totalReplyCount = Integer.parseInt(replyCountMatcher.group(1));
		}
		else
		{
			pageData.totalReplyCount = 1;
		}
	}
}