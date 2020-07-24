package alibaba.pay;

public class AliPayConfig {
    private String customAppId;
    private String customAppTypes;
    private String payWay;
    private String serverUrl;
    private String appId;
    private String privateKey;
    private String format;
    private String charset;
    private String signType;
    private String appCertPath;
    private String publicCertPath;
    private String rootCertPath;
    private String proxyHost;
    private int    proxyPort;

    public String getCustomAppId() {
        return customAppId;
    }

    public void setCustomAppId(String customAppId) {
        this.customAppId = customAppId;
    }

    public String getCustomAppTypes() {
        return customAppTypes;
    }

    public void setCustomAppTypes(String customAppTypes) {
        this.customAppTypes = customAppTypes;
    }

    public String getPayWay() {
        return payWay;
    }

    public void setPayWay(String payWay) {
        this.payWay = payWay;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getAppCertPath() {
        return appCertPath;
    }

    public void setAppCertPath(String appCertPath) {
        this.appCertPath = appCertPath;
    }

    public String getPublicCertPath() {
        return publicCertPath;
    }

    public void setPublicCertPath(String publicCertPath) {
        this.publicCertPath = publicCertPath;
    }

    public String getRootCertPath() {
        return rootCertPath;
    }

    public void setRootCertPath(String rootCertPath) {
        this.rootCertPath = rootCertPath;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

}
