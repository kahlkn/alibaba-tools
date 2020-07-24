package alibaba.pay;

import artoria.beans.BeanUtils;
import artoria.common.GenericResult;
import artoria.data.AppType;
import artoria.exception.ExceptionUtils;
import artoria.exchange.JsonUtils;
import artoria.time.DateUtils;
import artoria.util.Assert;
import artoria.util.ObjectUtils;
import artoria.util.StringUtils;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import misaka.pay.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

import static artoria.common.Constants.FAILURE;
import static artoria.common.Constants.SUCCESS;
import static artoria.data.AppType.APP_ANDROID;
import static artoria.data.AppType.APP_IOS;

public class AliPayProvider implements PayProvider {
    private static final String DEFAULT_SERVER_URL = "https://openapi.alipay.com/gateway.do";
    private static final String DEFAULT_SIGN_TYPE = "RSA2";
    private static final String DEFAULT_CHARSET = "utf-8";
    private static final String DEFAULT_FORMAT = "JSON";
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm";
    private static Logger log = LoggerFactory.getLogger(AliPayProvider.class);
    private AlipayClient payClient;
    private AliPayConfig payConfig;

    public AliPayProvider(AliPayConfig payConfig) {
        Assert.notNull(payConfig, "Parameter \"payConfig\" must not null. ");
        this.payConfig = payConfig;
        this.payClient = createPayClient(payConfig);
    }

    private AlipayClient createPayClient(AliPayConfig payConfig) {
        //
        String serverUrl = payConfig.getServerUrl();
        String appId = payConfig.getAppId();
        String privateKey = payConfig.getPrivateKey();
        String format = payConfig.getFormat();
        String charset = payConfig.getCharset();
        String signType = payConfig.getSignType();
        String appCertPath = payConfig.getAppCertPath();
        String publicCertPath = payConfig.getPublicCertPath();
        String rootCertPath = payConfig.getRootCertPath();
        String proxyHost = payConfig.getProxyHost();
        int proxyPort = payConfig.getProxyPort();
        //
        if (StringUtils.isBlank(serverUrl)) {
            payConfig.setServerUrl(serverUrl = DEFAULT_SERVER_URL);
        }
        if (StringUtils.isBlank(format)) {
            payConfig.setFormat(format = DEFAULT_FORMAT);
        }
        if (StringUtils.isBlank(charset)) {
            payConfig.setCharset(charset = DEFAULT_CHARSET);
        }
        if (StringUtils.isBlank(signType)) {
            payConfig.setSignType(signType = DEFAULT_SIGN_TYPE);
        }
        //
        AlipayClient alipayClient;
        if (StringUtils.isNotBlank(appCertPath)) {
            CertAlipayRequest certAlipayRequest =
                    BeanUtils.beanToBean(payConfig, CertAlipayRequest.class);
            certAlipayRequest.setCertPath(appCertPath);
            certAlipayRequest.setAlipayPublicCertPath(publicCertPath);
            certAlipayRequest.setRootCertPath(rootCertPath);
            try {
                alipayClient = new DefaultAlipayClient(certAlipayRequest);
            }
            catch (Exception e) { throw ExceptionUtils.wrap(e); }
        }
        else if (StringUtils.isNotBlank(publicCertPath)) {
            alipayClient = new DefaultAlipayClient(serverUrl, appId, privateKey
                    , format, charset, publicCertPath, signType, proxyHost, proxyPort);
        }
        else {
            alipayClient = new DefaultAlipayClient(serverUrl, appId, privateKey, format, charset);
        }
        return alipayClient;
    }

    private void handleError(String logMsg, Exception ex, GenericResult result) {
        String code;
        String message;
        if (ex instanceof AlipayApiException) {
            AlipayApiException aliEx = (AlipayApiException) ex;
            code = aliEx.getErrCode();
            message = aliEx.getErrMsg();
        }
        else {
            code = FAILURE;
            message = ex.getMessage();
        }
        result.setMessage(StringUtils.isNotBlank(message) ? message : FAILURE);
        result.setCode(StringUtils.isNotBlank(code) ? code : FAILURE);
        log.info(logMsg, ex);
    }

    @Override
    public OrderPayResult payOrder(OrderPayModel orderPayModel) {
        OrderPayResult orderPayResult = new OrderPayResult();
        try {
            Assert.notNull(orderPayModel, "Parameter \"orderPayModel\" must not null. ");
            String appId = orderPayModel.getAppId();
            String appType = orderPayModel.getAppType();
            String payWay = orderPayModel.getPayWay();

            orderPayResult.setAppId(appId);
            orderPayResult.setAppType(appType);
            orderPayResult.setPayWay(payWay);

            AlipayTradeAppPayRequest tradeAppPayRequest = new AlipayTradeAppPayRequest();
            AppType appTypeEnum = AppType.valueOf(appType);
            if (APP_ANDROID.equals(appTypeEnum) || APP_IOS.equals(appTypeEnum)) {
                AlipayTradeAppPayModel tradeAppPayModel = new AlipayTradeAppPayModel();
                tradeAppPayModel.setSubject(orderPayModel.getTitle());
                tradeAppPayModel.setBody(orderPayModel.getDescription());
                // currencyType
                tradeAppPayModel.setTotalAmount(orderPayModel.getTotalAmount());
                tradeAppPayModel.setOutTradeNo(orderPayModel.getOutTradeId());
                // startTime
                Date expirationTime = orderPayModel.getExpirationTime();
                if (expirationTime != null) {
                    String format = DateUtils.format(expirationTime, TIME_PATTERN);
                    tradeAppPayModel.setTimeExpire(format);
                }
                String passBack = orderPayModel.getPassBack();
                if (StringUtils.isNotBlank(passBack)) {
                    tradeAppPayModel.setPassbackParams(passBack);
                }
                tradeAppPayRequest.setBizModel(tradeAppPayModel);
                tradeAppPayRequest.setNotifyUrl(orderPayModel.getNotifyUrl());
            }
            else {
                throw new IllegalArgumentException("Unsupported application types. ");
            }
            log.info("Pay order internal input: {}", JsonUtils.toJsonString(tradeAppPayRequest));
            AlipayTradeAppPayResponse tradeAppPayResponse = payClient.sdkExecute(tradeAppPayRequest);
            log.info("Pay order internal output: {}", JsonUtils.toJsonString(tradeAppPayResponse));
            String body = tradeAppPayResponse.getBody();
            orderPayResult.rawData(tradeAppPayResponse);
            orderPayResult.setCode(SUCCESS);
            orderPayResult.setPayResult(body);
        }
        catch (Exception e) {
            handleError("Pay order failure. ", e, orderPayResult);
        }
        return orderPayResult;
    }

    @Override
    public OrderQueryResult queryOrder(OrderQueryModel orderQueryModel) {
        OrderQueryResult orderQueryResult = new OrderQueryResult();
        try {
            Assert.notNull(orderQueryModel, "Parameter \"orderQueryModel\" must not null. ");
            String appId = orderQueryModel.getAppId();
            String appType = orderQueryModel.getAppType();
            String payWay = orderQueryModel.getPayWay();
            String tradeId = orderQueryModel.getTradeId();
            String outTradeId = orderQueryModel.getOutTradeId();

            orderQueryResult.setAppId(appId);
            orderQueryResult.setAppType(appType);
            orderQueryResult.setPayWay(payWay);

            if (StringUtils.isBlank(tradeId)
                    && StringUtils.isBlank(outTradeId)) {
                throw new IllegalArgumentException(
                    "Parameter \"tradeId\" and \"outTradeId\" cannot be empty at the same time. "
                );
            }
            AlipayTradeQueryRequest tradeQueryRequest = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel tradeQueryModel = new AlipayTradeQueryModel();
            if (StringUtils.isNotBlank(tradeId)) {
                tradeQueryModel.setTradeNo(tradeId);
            }
            if (StringUtils.isNotBlank(outTradeId)) {
                tradeQueryModel.setOutTradeNo(outTradeId);
            }
            tradeQueryRequest.setBizModel(tradeQueryModel);

            log.info("Query order internal input: {}", JsonUtils.toJsonString(tradeQueryRequest));
            AlipayTradeQueryResponse tradeQueryResponse = payClient.certificateExecute(tradeQueryRequest);
            log.info("Query order internal output: {}", JsonUtils.toJsonString(tradeQueryResponse));

            orderQueryResult.rawData(tradeQueryResponse.getBody());
            orderQueryResult.putAll(BeanUtils.beanToMap(tradeQueryResponse));
            orderQueryResult.remove("class");
            orderQueryResult.remove("body");
            String code = (String) orderQueryResult.get("code");
            orderQueryResult.setCode(SUCCESS);
            if (!"10000".equals(code)) {
                String subCode = (String) orderQueryResult.get("subCode");
                String subMsg = (String) orderQueryResult.get("subMsg");
                orderQueryResult.setCode(subCode);
                orderQueryResult.setMessage(subMsg);
            }
            orderQueryResult.setTradeId(tradeQueryResponse.getTradeNo());
            orderQueryResult.setOutTradeId(tradeQueryResponse.getOutTradeNo());
            orderQueryResult.setCurrencyType(tradeQueryResponse.getPayCurrency());
            orderQueryResult.setTotalAmount(tradeQueryResponse.getTotalAmount());
            orderQueryResult.setTradeStatus(tradeQueryResponse.getTradeStatus());
//            orderQueryResult.setPassBack(tradeQueryResponse);
        }
        catch (Exception e) {
            handleError("Query order failure. ", e, orderQueryResult);
        }
        return orderQueryResult;
    }

    @Override
    public OrderCloseResult closeOrder(OrderCloseModel orderCloseModel) {
        OrderCloseResult orderCloseResult = new OrderCloseResult();
        try {
            Assert.notNull(orderCloseModel, "Parameter \"orderCloseModel\" must not null. ");
            String appId = orderCloseModel.getAppId();
            String appType = orderCloseModel.getAppType();
            String payWay = orderCloseModel.getPayWay();
            String tradeId = orderCloseModel.getTradeId();
            String outTradeId = orderCloseModel.getOutTradeId();

            orderCloseResult.setAppId(appId);
            orderCloseResult.setAppType(appType);
            orderCloseResult.setPayWay(payWay);

            if (StringUtils.isBlank(tradeId)
                    && StringUtils.isBlank(outTradeId)) {
                throw new IllegalArgumentException(
                    "Parameter \"tradeId\" and \"outTradeId\" cannot be empty at the same time. "
                );
            }
            AlipayTradeCloseRequest tradeCloseRequest = new AlipayTradeCloseRequest();
            AlipayTradeCloseModel tradeCloseModel = new AlipayTradeCloseModel();
            if (StringUtils.isNotBlank(tradeId)) {
                tradeCloseModel.setTradeNo(tradeId);
            }
            if (StringUtils.isNotBlank(outTradeId)) {
                tradeCloseModel.setOutTradeNo(outTradeId);
            }
            tradeCloseRequest.setBizModel(tradeCloseModel);

            log.info("Close order internal input: {}", JsonUtils.toJsonString(tradeCloseRequest));
            AlipayTradeCloseResponse tradeCloseResponse = payClient.certificateExecute(tradeCloseRequest);
            log.info("Close order internal output: {}", JsonUtils.toJsonString(tradeCloseResponse));

            orderCloseResult.rawData(tradeCloseResponse.getBody());
            orderCloseResult.putAll(BeanUtils.beanToMap(tradeCloseResponse));
            orderCloseResult.setCode(SUCCESS);
            orderCloseResult.setTradeId(tradeCloseResponse.getTradeNo());
            orderCloseResult.setOutTradeId(tradeCloseResponse.getOutTradeNo());
        }
        catch (Exception e) {
            handleError("Close order failure. ", e, orderCloseResult);
        }
        return orderCloseResult;
    }

    @Override
    public PayNotifyResult payNotify(PayNotifyModel payNotifyModel) {
        PayNotifyResult payNotifyResult = new PayNotifyResult();
        try {
            Assert.notNull(payNotifyModel, "Parameter \"payNotifyModel\" must not null. ");
            String appId = payNotifyModel.getAppId();
            String appType = payNotifyModel.getAppType();
            String payWay = payNotifyModel.getPayWay();
            Object notify = payNotifyModel.getNotify();
            Assert.notNull(notify, "Parameter \"notify\" must not null. ");

            payNotifyResult.setAppId(appId);
            payNotifyResult.setAppType(appType);
            payNotifyResult.setPayWay(payWay);

            String charset = payConfig.getCharset();
            String signType = payConfig.getSignType();
            String publicCertPath = payConfig.getPublicCertPath();

            Map<String, String> params = ObjectUtils.cast(BeanUtils.beanToMap(notify));
            log.info("Pay notify internal input: {}", JsonUtils.toJsonString(params));
            boolean validSignature = AlipaySignature.rsaCertCheckV1(params, publicCertPath, charset, signType);
            log.info("Pay notify internal output: validSignature = {}", validSignature);
            if (!validSignature) {
                throw new IllegalStateException("Signature verification failed. ");
            }
            //payNotifyResult.rawData(params);
            payNotifyResult.putAll(BeanUtils.beanToMap(params));
            payNotifyResult.setCode(SUCCESS);
            payNotifyResult.setMessage("Signature verification successful. ");
            payNotifyResult.setTradeId(params.get("trade_no"));
            payNotifyResult.setOutTradeId(params.get("out_trade_no"));
            //payNotifyResult.setCurrencyType(params.get(""));
            payNotifyResult.setTotalAmount(params.get("total_amount"));
            payNotifyResult.setPassBack(params.get("passback_params"));
        }
        catch (Exception e) {
            handleError("Pay notify failure. ", e, payNotifyResult);
        }
        return payNotifyResult;
    }

}
