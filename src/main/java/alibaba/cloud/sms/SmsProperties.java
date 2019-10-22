package alibaba.cloud.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Alibaba cloud sms properties.
 * @author Kahle
 */
@ConfigurationProperties("cloud.alibaba.sms")
public class SmsProperties {
    private Boolean enabled;
    private String accessKeyId;
    private String accessKeySecret;
    private String regionId;

    public Boolean getEnabled() {

        return enabled;
    }

    public void setEnabled(Boolean enabled) {

        this.enabled = enabled;
    }

    public String getAccessKeyId() {

        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {

        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {

        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {

        this.accessKeySecret = accessKeySecret;
    }

    public String getRegionId() {

        return regionId;
    }

    public void setRegionId(String regionId) {

        this.regionId = regionId;
    }

}
