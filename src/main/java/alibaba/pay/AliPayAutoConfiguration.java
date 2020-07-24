package alibaba.pay;

import artoria.common.Constants;
import artoria.exception.ExceptionUtils;
import artoria.file.FileUtils;
import artoria.io.IOUtils;
import artoria.util.ClassLoaderUtils;
import artoria.util.CollectionUtils;
import artoria.util.StringUtils;
import misaka.pay.PayProvider;
import misaka.pay.PayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import static artoria.common.Constants.*;

@Configuration
@ConditionalOnProperty(name = "alibaba.pay.enabled", havingValue = "true")
@EnableConfigurationProperties({AliPayProperties.class})
public class AliPayAutoConfiguration {
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static Logger log = LoggerFactory.getLogger(AliPayAutoConfiguration.class);

    @Autowired
    public AliPayAutoConfiguration(AliPayProperties wxPayProperties) {
        List<AliPayConfig> configs = wxPayProperties.getConfigs();
        if (CollectionUtils.isNotEmpty(configs)) {
            for (AliPayConfig config : configs) {
                String customAppTypes = config.getCustomAppTypes();
                String customAppId = config.getCustomAppId();
                String payWay = config.getPayWay();
                String privateKey = config.getPrivateKey();
                if (StringUtils.isNotBlank(privateKey)) {
                    config.setPrivateKey(fileContent(privateKey));
                }
                String appCertPath = config.getAppCertPath();
                if (StringUtils.isNotBlank(appCertPath)) {
                    config.setAppCertPath(filePath(appCertPath));
                }
                String publicCertPath = config.getPublicCertPath();
                if (StringUtils.isNotBlank(publicCertPath)) {
                    config.setPublicCertPath(filePath(publicCertPath));
                }
                String rootCertPath = config.getRootCertPath();
                if (StringUtils.isNotBlank(rootCertPath)) {
                    config.setRootCertPath(filePath(rootCertPath));
                }
                PayProvider payProvider = new AliPayProvider(config);
                String[] typeArray = customAppTypes != null
                        ? customAppTypes.split(COMMA) : new String[ZERO];
                PayUtils.register(customAppId, typeArray, payWay, payProvider);
            }
        }
    }

    private String fileContent(String fileContent) {
        if (StringUtils.isBlank(fileContent)) {
            return fileContent;
        }
        try {
            if (fileContent.startsWith("path:")) {
                log.info("Reading the contents of file \"{}\". ", fileContent);
                String path = fileContent.substring(FIVE);
                return new String(FileUtils.read(new File(path)));
            }
            else if (fileContent.startsWith("classpath:")) {
                log.info("Reading the contents of file \"{}\". ", fileContent);
                String classpath = fileContent.substring(TEN);
                InputStream inputStream = ClassLoaderUtils
                        .getResourceAsStream(classpath, AliPayAutoConfiguration.class);
                return IOUtils.toString(inputStream);
            }
            else {
                return fileContent;
            }
        }
        catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private String filePath(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return filePath;
        }
        if (!filePath.startsWith("classpath:")) {
            return filePath;
        }
        String path = filePath.substring(TEN);
        InputStream inputStream = ClassLoaderUtils
                .getResourceAsStream(path, AliPayAutoConfiguration.class);
        File tmpFile = new File(TMP_DIR, "artoria");
        String name = new File(Constants.ROOT_PATH).getName();
        tmpFile = new File(tmpFile, name);
        File result = new File(tmpFile, path);
        try {
            FileUtils.write(inputStream, result);
        }
        catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
        log.info("Copy the file from \"{}\" to \"{}\" success. ", filePath, result);
        return result.toString();
    }

}
