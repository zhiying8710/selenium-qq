package me.binge.selenium.qq.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import me.binge.selenium.qq.common.VerifyCode;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Cookie;

import com.alibaba.fastjson.JSONObject;

public class HttpUtils {

//    private static final Logger logger = Logger.getLogger(HttpUtils.class);

    private static Map<String, String> qqDomainCookieNames = new HashMap<String, String>();

    static {

        String domain = ".ptlogin2.qq.com";
        qqDomainCookieNames.put("chkuin", domain);
        qqDomainCookieNames.put("clientuin", domain);
        qqDomainCookieNames.put("confirmuin", domain);
        qqDomainCookieNames.put("dlock", domain);
        qqDomainCookieNames.put("ETK", domain);
        qqDomainCookieNames.put("pt_local_token", domain);
        qqDomainCookieNames.put("pt_login_sig", domain);
        qqDomainCookieNames.put("ptvfsession", domain);
        qqDomainCookieNames.put("qrsig", domain);
        qqDomainCookieNames.put("supertoken", domain);
        qqDomainCookieNames.put("uikey", domain);

        String domain2 = ".qq.com";
        qqDomainCookieNames.put("o_cookie", domain2);
        qqDomainCookieNames.put("pgv_pvid", domain2);
        qqDomainCookieNames.put("pt_clientip", domain2);
        qqDomainCookieNames.put("pt_serverip", domain2);
        qqDomainCookieNames.put("pt2gguin", domain2);
        qqDomainCookieNames.put("ptcz", domain2);
        qqDomainCookieNames.put("ptisp", domain2);
        qqDomainCookieNames.put("ptui_loginuin", domain2);
        qqDomainCookieNames.put("qz_gdt", domain2);
        qqDomainCookieNames.put("verifysession", domain2);
    }

    private static int size = 1 << 23;

    public static VerifyCode getQQVerifyCode(String url, Map<String, String> headers, Set<Cookie> cookies, String strCookies) {
        HttpClient httpClient = null;
        GetMethod get = null;
        try {
            httpClient = getHttpClient();

            HttpState initialState = new HttpState();
            List<org.apache.commons.httpclient.Cookie> realCookies = new ArrayList<org.apache.commons.httpclient.Cookie>();
            if (cookies != null) {
                if (cookies instanceof Collection) {
                    Collection<Cookie> orginCookies = (Collection<Cookie>) cookies;
                    for (Cookie cookie : orginCookies) {
                        realCookies.add(new org.apache.commons.httpclient.Cookie(qqDomainCookieNames.get(cookie.getName()), cookie.getName(), cookie.getValue(), "/", null, false));
                    }
                }
            }
            if (strCookies != null) {
                String[] cookieKvs = strCookies.split("; ");
                for (String cookieKv : cookieKvs) {
                    String[] cookieEntry = cookieKv.split("=");
                    if (cookieEntry.length == 1) {
                        continue;
                    }
                    String name = cookieEntry[0];
                    String value = cookieEntry[1];
                    realCookies.add(new org.apache.commons.httpclient.Cookie(qqDomainCookieNames.get(name), name, value, "/", null, false));
                }

            }
            initialState.addCookies(realCookies.toArray(new org.apache.commons.httpclient.Cookie[realCookies.size()]));
            httpClient.setState(initialState);
//            logger.info(StringUtils.join(initialState.getCookies(), "\r\n"));

            // and then added to your HTTP state instance
            get = new GetMethod(url);

            if (headers != null) {
                for (Entry<String, String> entry : headers.entrySet()) {
                    get.addRequestHeader(entry.getKey(), entry.getValue());
                }
            }
            initMethod(get);
            httpClient.executeMethod(get);
            VerifyCode verifyCode = new VerifyCode();
            verifyCode.setBytes(get.getResponseBody(size));

            String respCookie = get.getResponseHeader("Set-Cookie").getValue();
            List<Cookie> setCookies = new ArrayList<Cookie>();
            if (StringUtils.isNotBlank(respCookie)) {
                if (StringUtils.isNotBlank(respCookie)) {
                    String[] perCookies = respCookie.split(", ");
                    for (String perCookie : perCookies) {
                        String[] infos = perCookie.split("; ");
                        String name = "";
                        String value = "";
                        String path = "";
                        String domain = "";
                        for (String info : infos) {
                            String[] ss = info.split("=");
                            String s1 = ss[0];
                            String s2 = "";
                            if (ss.length > 1) {
                                s2 = ss[1];
                            }
                            if ("path".equals(s1.toLowerCase())) {
                                path = s2;
                            } else  if ("domain".equals(s1.toLowerCase())) {
//                                if (!s2.startsWith(".")) {
//                                    s2 = "." + s2;
//                                }
                                domain = s2;
                            } else {
                                name = s1;
                                value = s2;
                            }
                        }
                        setCookies.add(new Cookie(name, value, domain, path, null, false));
                    }
                }
            }
            verifyCode.setCookies(setCookies);
            return verifyCode;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
            if (httpClient != null) {
                httpClient.getHttpConnectionManager().closeIdleConnections(0);
            }
        }

    }

    private static void initMethod(HttpMethod method) {
        method.getParams().setVersion(HttpVersion.HTTP_1_1);
//        method.getParams().setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
        method.getParams().setSoTimeout(5000);// 5秒超时response
    }

    private static HttpClient getHttpClient() {
        HttpClient httpClient;
        httpClient = new HttpClient(new HttpClientParams(), new MultiThreadedHttpConnectionManager());// 连接在releaseConnection后总是被关闭
        httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        httpClient.getParams().setSoTimeout(30000);
        httpClient.getParams().setConnectionManagerTimeout(30000);
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(30000);
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
        return httpClient;
    }

    public static String postFile(String url, String filePath, Map<String, String> params) throws Exception {
        PostMethod post = null;
        HttpClient httpClient = null;
        try {
            httpClient = getHttpClient();
            post = new PostMethod(url);
            initMethod(post);
            File targetFile = new File(filePath);
            Part[] parts = new Part[params.size() + 1];
            parts[0] = new FilePart("upload", targetFile);
            int i = 1;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                parts[i] = new StringPart(entry.getKey(), entry.getValue());
                i ++;
            }
            post.setRequestEntity(new MultipartRequestEntity(parts,post.getParams()));
            httpClient.executeMethod(post);
            return post.getResponseBodyAsString(size);
        } finally {
            post.releaseConnection();
            httpClient.getHttpConnectionManager().closeIdleConnections(0);
        }
    }

    @SuppressWarnings("unchecked")
    public static String dama(String filePath, int retryCnt, int maxRetryCnt) {
        if (retryCnt > maxRetryCnt && maxRetryCnt != -1) {
            return null;
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put("user_name", "dickyzeng");
        params.put("user_pw", "3gfeidee");
        params.put("yzm_minlen", "4");
        params.put("yzm_maxlen", "4");
        params.put("yzmtype_mark", "0");
        params.put("zztool_token", "j8K17K05tGbg369k7uTLOO3T1t7x71kt01J8tgjk");
        String r;
        Map<String, Object> rj = null;
        try {
            r = postFile("http://bbb4.hyslt.com/api.php?mod=php&act=upload", filePath, params);
            rj = (Map<String, Object>) JSONObject.parse(r);
            Map<String, Object> data = (Map<String, Object>) rj.get("data");
            return data.get("val").toString();
        } catch (Exception e) {
            retryCnt += 1;
            return dama(filePath, retryCnt, maxRetryCnt);
        }

    }

    public static String getQQMailMainPage(String sid, String r, List<org.apache.commons.httpclient.Cookie> cookies) {

        HttpClient httpClient = null;
        GetMethod get = null;
        try {
            httpClient = getHttpClient();

            HttpState initialState = new HttpState();
            initialState.addCookies(cookies.toArray(new org.apache.commons.httpclient.Cookie[cookies.size()]));
            httpClient.setState(initialState);
            get = new GetMethod("https://mail.qq.com/cgi-bin/frame_html?sid=" + sid + "&r=" + r);
            initMethod(get);
            httpClient.executeMethod(get);
            return get.getResponseBodyAsString(size);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
            if (httpClient != null) {
                httpClient.getHttpConnectionManager().closeIdleConnections(0);
            }
        }



    }

}
