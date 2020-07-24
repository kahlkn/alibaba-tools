package alibaba.pay;

import artoria.data.AppType;
import artoria.exchange.FastJsonProvider;
import artoria.exchange.JsonUtils;
import artoria.identifier.IdentifierUtils;
import artoria.time.DateUtils;
import com.alibaba.fastjson.JSON;
import misaka.pay.*;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static artoria.common.Constants.ONE;
import static java.lang.Boolean.TRUE;

@Ignore
public class AliPayProviderTest {
    private static Logger log = LoggerFactory.getLogger(AliPayProviderTest.class);
    private static final String CUSTOM_APP_ID = "TEST01";
    private static final String PAY_WAY = "ALIPAY";

    static {
        List<AliPayConfig> configs = new ArrayList<AliPayConfig>();
        AliPayProperties aliPayProperties = new AliPayProperties();
        aliPayProperties.setConfigs(configs);

        AliPayConfig aliPayConfig = new AliPayConfig();
        aliPayConfig.setCustomAppId(CUSTOM_APP_ID);
        aliPayConfig.setCustomAppTypes("APP_ANDROID,APP_IOS");
        aliPayConfig.setPayWay(PAY_WAY);
        aliPayConfig.setServerUrl("https://openapi.alipay.com/gateway.do");
        aliPayConfig.setAppId("appId");
        aliPayConfig.setPrivateKey("classpath:app/privateKey.txt");
        aliPayConfig.setFormat("JSON");
        aliPayConfig.setCharset("utf-8");
        aliPayConfig.setSignType("RSA2");
        aliPayConfig.setAppCertPath("classpath:app/appCertPublicKey.crt");
        aliPayConfig.setPublicCertPath("classpath:app/alipayCertPublicKey.crt");
        aliPayConfig.setRootCertPath("classpath:app/alipayRootCert.crt");
        configs.add(aliPayConfig);

        new AliPayAutoConfiguration(aliPayProperties);
        JsonUtils.setJsonProvider(new FastJsonProvider());
    }

    @Test
    public void test1() {
        String outTradeId = IdentifierUtils.nextStringIdentifier();
        log.info("outTradeId: {}", outTradeId);
        OrderPayModel orderPayModel = new OrderPayModel();
        orderPayModel.setAppId(CUSTOM_APP_ID);
        orderPayModel.setPayWay(PAY_WAY);
        orderPayModel.setAppType(AppType.APP_ANDROID.name());
        orderPayModel.setTitle("支付测试");
        orderPayModel.setDescription("这是一个支付测试哈");
        orderPayModel.setOutTradeId(outTradeId);
        orderPayModel.setStartTime(new Date());
        orderPayModel.setExpirationTime(DateUtils.create().addHour(ONE).getDate());
        orderPayModel.setTotalAmount("0.01");
        orderPayModel.setNotifyUrl("http://127.0.0.1/notifyUrl");
        OrderPayResult payOrderResult = PayUtils.payOrder(orderPayModel);
        log.info(JSON.toJSONString(payOrderResult, TRUE));
    }

    @Test
    public void test2() {
        OrderQueryModel orderQueryModel = new OrderQueryModel();
        orderQueryModel.setAppId(CUSTOM_APP_ID);
        orderQueryModel.setPayWay(PAY_WAY);
        orderQueryModel.setAppType(AppType.APP_ANDROID.name());
//        orderQueryModel.setTradeId("TradeId");
        orderQueryModel.setOutTradeId("OutTradeId");
        OrderQueryResult orderQueryResult = PayUtils.queryOrder(orderQueryModel);
        log.info(JSON.toJSONString(orderQueryResult, TRUE));
    }

    @Test
    public void test3() {
        OrderCloseModel orderCloseModel = new OrderCloseModel();
        orderCloseModel.setAppId(CUSTOM_APP_ID);
        orderCloseModel.setPayWay(PAY_WAY);
        orderCloseModel.setAppType(AppType.APP_ANDROID.name());
//        orderCloseModel.setTradeId("TradeId");
        orderCloseModel.setOutTradeId("OutTradeId");
        OrderCloseResult orderCloseResult = PayUtils.closeOrder(orderCloseModel);
        log.info(JSON.toJSONString(orderCloseResult, TRUE));
    }

    @Test
    public void test4() {
        PayNotifyModel payNotifyModel = new PayNotifyModel();
        payNotifyModel.setAppId(CUSTOM_APP_ID);
        payNotifyModel.setPayWay(PAY_WAY);
        payNotifyModel.setAppType(AppType.APP_ANDROID.name());
        payNotifyModel.setNotify(JSON.parseObject("{\"gmt_create\":\"2020-05-14 15:29:23\",\"charset\":\"utf-8\",\"point_amount\":\"0.00\"}"));
        PayNotifyResult payNotifyResult = PayUtils.payNotify(payNotifyModel);
        log.info(JSON.toJSONString(payNotifyResult, TRUE));
    }

}
