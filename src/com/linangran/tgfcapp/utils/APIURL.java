package com.linangran.tgfcapp.utils;

/**
 * Created by linangran on 3/1/15.
 */
public class APIURL
{
	public static final String DOMAIN_NAME = "tgfcer.com";
	public static final String WAP_DOMAIN_NAME = "wap." + DOMAIN_NAME;
	public static final String WAP_BASE_URL = "http://" + WAP_DOMAIN_NAME;
	public static final String WAP_API_URL = WAP_BASE_URL + "/index.php";
	public static final String WAP_VIEW_FORUM_URL = WAP_API_URL + "?action=forum&fid=";
	public static final String WAP_VIEW_CONTENT_URL = WAP_API_URL + "?action=thread&sc=1&tid=";
	public static final String WAP_LOGIN_URL = WAP_API_URL + "?action=login";
	public static final String WAP_LOGOUT_URL = WAP_API_URL + "?action=login&logout=yes";
	public static final String WAP_MY_INFO = WAP_API_URL + "?action=my";
	public static final String WAP_POST_NEW = WAP_API_URL + "?action=post&do=newthread";
	public static final String WAP_POST_REPLY = WAP_API_URL + "?action=post&do=reply";
	public static final String WAP_POST_EDIT = WAP_API_URL + "?action=post&do=edit";


	public static final String ANDROID_CLIENT_SIGNATURE_DEFAULT = "\r\n________\r\n发送自TGFC Android测试版客户端";
	public static final String ANDROID_CLIENT_SIGNATURE_TGBXS = "\r\n________\r\n发送自TGBXS Android测试版客户端";
	public static final String ANDROID_CLIENT_SIGNATURE_TGYXW = "\r\n________\r\n发送自支持威武有希望了!客户端";
	public static final String ANDROID_CLIENT_SIGNATURE_REGEX = "<br(?:\\s*?\\/)?>\\s*_{8,}\\s*<br(?:\\s*?\\/)?>\\s*(发送自.+?客户端)";
	public static final String ANDROID_CLIENT_SIGNATURE_EDIT_REGEX = "\\s*_{8,}\\s*(发送自.+?客户端)\\s*";
}
