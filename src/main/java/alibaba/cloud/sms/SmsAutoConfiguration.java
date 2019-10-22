package alibaba.cloud.sms;

import alibaba.cloud.common.DefaultProperties;
import artoria.util.StringUtils;
import misaka.sms.SmsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Alibaba cloud sms auto configuration.
 * @author Kahle
 */
@Configuration
@ConditionalOnProperty(name = "cloud.alibaba.sms.enabled", havingValue = "true")
@EnableConfigurationProperties({DefaultProperties.class, SmsProperties.class})
public class SmsAutoConfiguration {
    private static final String DEFAULT_REGION_ID = "default";
    private static Logger log = LoggerFactory.getLogger(SmsAutoConfiguration.class);
    private final SmsProperties smsProperties;

    @Autowired
    public SmsAutoConfiguration(DefaultProperties defaultProperties, SmsProperties smsProperties) {
        if (StringUtils.isBlank(smsProperties.getAccessKeyId())) {
            smsProperties.setAccessKeyId(defaultProperties.getAccessKeyId());
        }
        if (StringUtils.isBlank(smsProperties.getAccessKeySecret())) {
            smsProperties.setAccessKeySecret(defaultProperties.getAccessKeySecret());
        }
        if (StringUtils.isBlank(smsProperties.getRegionId())) {
            smsProperties.setRegionId(DEFAULT_REGION_ID);
        }
        this.smsProperties = smsProperties;
    }

    @Bean
    public SmsProvider smsProvider() {
        String accessKeySecret = smsProperties.getAccessKeySecret();
        String accessKeyId = smsProperties.getAccessKeyId();
        String regionId = smsProperties.getRegionId();
        SmsProvider smsProvider = new SmsProviderImpl(accessKeyId, accessKeySecret, regionId);
        log.info("Alibaba cloud sms provider was initialized success. ");
        return smsProvider;
    }

}
