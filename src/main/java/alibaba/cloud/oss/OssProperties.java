package alibaba.cloud.oss;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Alibaba cloud oss properties.
 * @author Kahle
 */
@ConfigurationProperties("cloud.alibaba.oss")
public class OssProperties {
    private Boolean enabled;
    private String accessKeyId;
    private String accessKeySecret;
    private String endpoint;

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

    public String getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(String endpoint) {

        this.endpoint = endpoint;
    }

}
