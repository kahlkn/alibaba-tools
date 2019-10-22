package alibaba.cloud.sms;

import artoria.exchange.FastJsonProvider;
import artoria.exchange.JsonUtils;
import artoria.time.DateUtils;
import com.alibaba.fastjson.JSON;
import misaka.sms.SmsProvider;
import misaka.sms.SmsQuery;
import misaka.sms.SmsQueryResult;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Ignore
public class SmsProviderImplTest {
    private static Logger log = LoggerFactory.getLogger(SmsProviderImplTest.class);
    private static String accessKeySecret = "accessKeySecret";
    private static String accessKeyId = "accessKeyId";
    private static String regionId = "default";
    private static SmsProvider smsProvider = new SmsProviderImpl(accessKeyId, accessKeySecret, regionId);

    @Test
    public void findSelectiveTest() {
        JsonUtils.setJsonProvider(new FastJsonProvider());
        SmsQuery smsQuery = new SmsQuery();
        smsQuery.setPhoneNumber("13688886666");
        smsQuery.setSendTime(DateUtils.create().addDay(-4).getDate());
        // smsQuery.setBusinessId("11111111");
        smsQuery.setPageNum(1);
        smsQuery.setPageSize(100);
        List<SmsQueryResult> resultList = smsProvider.findSelective(smsQuery);
        log.info("{}", JSON.toJSONString(resultList, Boolean.TRUE));
    }

}
