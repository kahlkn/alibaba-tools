package alibaba.cloud.oss;

import artoria.exception.ExceptionUtils;
import artoria.exchange.JsonUtils;
import artoria.util.Assert;
import artoria.util.MapUtils;
import artoria.util.StringUtils;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import misaka.storage.object.*;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

import static artoria.common.Constants.DOT;

/**
 * Implementation of object storage provider based on alibaba cloud.
 * @author Kahle
 */
public class OssProviderImpl implements ObjectStorageProvider {
    private static final List<String> OSS_HEADER_LIST = new ArrayList<String>();
    private static final Long DEFAULT_EXPIRE_TIME = 30L;
    private String accessKeyId;
    private String endpoint;
    private OSS ossClient;

    public OssProviderImpl(OSS ossClient, String accessKeyId, String endpoint) {
        Assert.notBlank(accessKeyId, "Parameter \"accessKeyId\" must not blank. ");
        Assert.notBlank(endpoint, "Parameter \"endpoint\" must not blank. ");
        Assert.notNull(ossClient, "Parameter \"ossClient\" must not null. ");
        this.ossClient = ossClient;
        this.accessKeyId = accessKeyId;
        this.endpoint = endpoint;
        initializeHeaders();
    }

    public OssProviderImpl(String accessKeyId, String accessKeySecret, String endpoint) {
        Assert.notBlank(accessKeyId, "Parameter \"accessKeyId\" must not blank. ");
        Assert.notBlank(accessKeySecret, "Parameter \"accessKeySecret\" must not blank. ");
        Assert.notBlank(endpoint, "Parameter \"endpoint\" must not blank. ");
        OSSClientBuilder builder = new OSSClientBuilder();
        this.ossClient = builder.build(endpoint, accessKeyId, accessKeySecret);
        this.accessKeyId = accessKeyId;
        this.endpoint = endpoint;
        initializeHeaders();
    }

    private void initializeHeaders() {
        try {
            Field[] fields = OSSHeaders.class.getFields();
            for (Field field : fields) {
                Object obj = field.get(null);
                String value = String.valueOf(obj);
                value = value.trim().toLowerCase();
                OSS_HEADER_LIST.add(value);
            }
        }
        catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    private ObjectMetadata convert(Map<String, Object> metadata) {
        if (metadata == null) { return null; }
        ObjectMetadata result = new ObjectMetadata();
        if (MapUtils.isEmpty(metadata)) { return result; }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (StringUtils.isBlank(key)) { continue; }
            if (value == null) { continue; }
            String tmpKey = key.trim().toLowerCase();
            if (OSS_HEADER_LIST.contains(tmpKey)) {
                result.setHeader(key, value);
            }
            else {
                result.addUserMetadata(key, String.valueOf(value));
            }
        }
        return result;
    }

    @Override
    public AuthorizationResult authorization(AuthorizationRequest authorizationRequest) {
        Assert.notNull(authorizationRequest, "Parameter \"authorizationRequest\" must not null. ");
        String callbackAddress = authorizationRequest.getCallbackAddress();
        String bucketName = authorizationRequest.getBucketName();
        String directory = authorizationRequest.getDirectory();
        Long expireTime = authorizationRequest.getExpireTime();
        if (StringUtils.isBlank(bucketName)) {
            throw new ObjectStorageException("Parameter \"bucketName\" must not blank. ");
        }
        if (StringUtils.isBlank(directory)) {
            throw new ObjectStorageException("Parameter \"directory\" must not blank. ");
        }
        if (expireTime == null) { expireTime = DEFAULT_EXPIRE_TIME; }

        String host = "http://" + bucketName + DOT + endpoint;
        long expireTimestamp = System.currentTimeMillis() + expireTime * 1000;
        Date expiration = new Date(expireTimestamp);
        long min = 0; long max = 1048576000;
        PolicyConditions policyConditions = new PolicyConditions();
        policyConditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, min, max);
        policyConditions.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, directory);
        String postPolicy = ossClient.generatePostPolicy(expiration, policyConditions);
        byte[] binaryData = postPolicy.getBytes();
        String postPolicyBase64 = BinaryUtil.toBase64String(binaryData);
        String postSignature = ossClient.calculatePostSignature(postPolicy);

        AuthorizationResult result = new AuthorizationResult();
        result.setAccessId(accessKeyId);
        result.setHost(host);
        result.setDirectory(directory);
        result.setPolicy(postPolicyBase64);
        result.setSignature(postSignature);
        result.setExpireTime(expireTimestamp / 1000);

        if (StringUtils.isNotBlank(callbackAddress)) {
            String callbackBody = "filename=${object}&size=${size}&mimeType=${mimeType}";
            callbackBody += "&height=${imageInfo.height}&width=${imageInfo.width}";
            Map<String, Object> callbackMap = new LinkedHashMap<String, Object>();
            callbackMap.put("callbackUrl", callbackAddress);
            callbackMap.put("callbackBody", callbackBody);
            callbackMap.put("callbackBodyType", "application/x-www-form-urlencoded");
            String callbackJson = JsonUtils.toJsonString(callbackMap);
            String callbackBase64 = BinaryUtil.toBase64String(callbackJson.getBytes());
            result.setCallbackContent(callbackBase64);
        }

        return result;
    }

    @Override
    public StorageObject getObject(String bucketName, String objectKey) {
        Assert.notBlank(bucketName, "Parameter \"bucketName\" must not blank. ");
        Assert.notBlank(objectKey, "Parameter \"objectKey\" must not blank. ");
        OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
        StorageObject result = new StorageObject();
        result.setOriginal(ossObject);
        result.setObjectKey(ossObject.getKey());
        result.setBucketName(ossObject.getBucketName());
        result.setObjectContent(ossObject.getObjectContent());
        ObjectMetadata objectMetadata = ossObject.getObjectMetadata();
        result.setMetadata(objectMetadata.getRawMetadata());
        return result;
    }

    @Override
    public StorageResult putObject(String bucketName, String objectKey, File file) {

        return putObject(bucketName, objectKey, file, null);
    }

    @Override
    public StorageResult putObject(String bucketName, String objectKey, File file, Map<String, Object> metadata) {
        Assert.notNull(file, "Parameter \"file\" must not null. ");
        return putObject(bucketName, objectKey, null, file, metadata);
    }

    @Override
    public StorageResult putObject(String bucketName, String objectKey, InputStream inputStream) {

        return putObject(bucketName, objectKey, inputStream, null);
    }

    @Override
    public StorageResult putObject(String bucketName, String objectKey, InputStream inputStream, Map<String, Object> metadata) {
        Assert.notNull(inputStream, "Parameter \"inputStream\" must not null. ");
        return putObject(bucketName, objectKey, inputStream, null, metadata);
    }

    private StorageResult putObject(String bucketName, String objectKey, InputStream inputStream, File file, Map<String, Object> metadata) {
        Assert.notBlank(bucketName, "Parameter \"bucketName\" must not blank. ");
        Assert.notBlank(objectKey, "Parameter \"objectKey\" must not blank. ");
        PutObjectResult putObjectResult;
        if (inputStream != null) {
            putObjectResult = ossClient.putObject(bucketName, objectKey, inputStream, convert(metadata));
        }
        else if (file != null) {
            putObjectResult = ossClient.putObject(bucketName, objectKey, file, convert(metadata));
        }
        else {
            throw new IllegalArgumentException("All necessary parameters entered are null. ");
        }
        StorageResult result = new StorageResult();
        result.setBusinessId(putObjectResult.getRequestId());
        return result;
    }

    @Override
    public void destroy() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

}
