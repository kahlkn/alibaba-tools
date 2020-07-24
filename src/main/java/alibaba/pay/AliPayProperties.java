package alibaba.pay;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "alibaba.pay")
public class AliPayProperties {
    private Boolean enabled;
    private List<AliPayConfig> configs;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<AliPayConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(List<AliPayConfig> configs) {
        this.configs = configs;
    }

}
