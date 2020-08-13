package alibaba.talk;

import artoria.codec.Base64Utils;
import artoria.crypto.Hmac;
import artoria.crypto.KeyUtils;
import artoria.exception.ExceptionUtils;
import artoria.exchange.JsonUtils;
import artoria.net.*;
import artoria.util.CollectionUtils;
import artoria.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static artoria.common.Constants.*;

public class DingTalkRobot {
    private static Logger log = LoggerFactory.getLogger(DingTalkRobot.class);
    private HttpClient httpClient;
    private String webHook;
    private String secret;

    public DingTalkRobot(String webHook, String secret) {

        this(HttpUtils.getHttpClient(), webHook, secret);
    }

    public DingTalkRobot(HttpClient httpClient, String webHook, String secret) {
        this.httpClient = httpClient;
        this.webHook = webHook;
        this.secret = secret;
    }

    protected String sign(Long timestamp, String secret) {
        try {
            String strToSign = timestamp + "\n" + secret;
            byte[] bytesToSign = strToSign.getBytes(UTF_8);
            byte[] secretBytes = secret.getBytes(UTF_8);
            SecretKey secretKey =
                    KeyUtils.parseSecretKey(HMAC_SHA256, secretBytes);
            Hmac hmac = new Hmac(HMAC_SHA256);
            hmac.setSecretKey(secretKey);
            byte[] digest = hmac.digest(bytesToSign);
            String base64Str = Base64Utils.encodeToString(digest);
            return URLEncoder.encode(base64Str, UTF_8);
        }
        catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    protected String full(String webHook, String secret) {
        if (StringUtils.isBlank(secret)) { return webHook; }
        String fullUrl = webHook;
        Long timestamp = System.currentTimeMillis();
        String sign = sign(timestamp, secret);
        fullUrl += "&timestamp=" + timestamp;
        fullUrl += "&sign=" + sign;
        return fullUrl;
    }

    protected void at(Map<String, Object> data, boolean atAll, List<String> atList) {
        if (!atAll && CollectionUtils.isEmpty(atList)) { return; }
        Map<String, Object> atMap = new HashMap<String, Object>(FOUR);
        if (CollectionUtils.isNotEmpty(atList)) {
            atMap.put("atMobiles", atList);
        }
        atMap.put("isAtAll", atAll);
        data.put("at", atMap);
    }

    public Object send(Object object) {
        try {
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.setUrl(full(webHook, secret));
            httpRequest.setMethod(HttpMethod.POST);
            httpRequest.addHeader("Content-Type", "application/json");
            if (object instanceof String) {
                log.info("DingTalk robot send \"{}\". ", object);
                httpRequest.setBody(object);
            }
            else {
                log.info("DingTalk robot send \"{}\". ", JsonUtils.toJsonString(httpRequest));
                httpRequest.setBody(JsonUtils.toJsonString(object));
            }
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            String bodyAsString = httpResponse.getBodyAsString();
            log.info("DingTalk robot receive \"{}\". ", bodyAsString);
            return bodyAsString;
        }
        catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    public Object sendText(String content, boolean atAll, List<String> atList) {
        Map<String, Object> data = new HashMap<String, Object>(FOUR);
        data.put("msgtype", "text");
        Map<String, Object> textMap = new HashMap<String, Object>(TWO);
        textMap.put("content", content);
        data.put("text", textMap);
        at(data, atAll, atList);
        return send(JsonUtils.toJsonString(data));
    }

    public Object sendMarkdown(String title, String text, boolean atAll, List<String> atList) {
        Map<String, Object> data = new HashMap<String, Object>(FOUR);
        data.put("msgtype", "markdown");
        Map<String, Object> markdownMap = new HashMap<String, Object>(FOUR);
        markdownMap.put("title", title);
        markdownMap.put("text", text);
        data.put("markdown", markdownMap);
        at(data, atAll, atList);
        return send(JsonUtils.toJsonString(data));
    }

}
