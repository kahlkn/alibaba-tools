package alibaba.cloud.sms;

import artoria.beans.BeanUtils;
import artoria.exception.ExceptionUtils;
import artoria.exchange.JsonUtils;
import artoria.time.DateUtils;
import artoria.util.*;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import misaka.sms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static artoria.common.Constants.ONE;
import static artoria.common.Constants.ZERO;

/**
 * Implementation of sms provider based on alibaba cloud.
 * @author Kahle
 */
public class SmsProviderImpl implements SmsProvider {
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_REGION_ID = "default";
    private static final String OK_CODE = "OK";
    private static Logger log = LoggerFactory.getLogger(SmsProviderImpl.class);
    private IAcsClient acsClient;

    public SmsProviderImpl(IClientProfile clientProfile) {
        Assert.notNull(clientProfile, "Parameter \"clientProfile\" must not null. ");
        this.acsClient = new DefaultAcsClient(clientProfile);
    }

    public SmsProviderImpl(String accessKeyId, String accessKeySecret, String regionId) {
        Assert.notBlank(accessKeySecret, "Parameter \"accessKeySecret\" must not blank. ");
        Assert.notBlank(accessKeyId, "Parameter \"accessKeyId\" must not blank. ");
        if (StringUtils.isBlank(regionId)) { regionId = DEFAULT_REGION_ID; }
        IClientProfile clientProfile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        this.acsClient = new DefaultAcsClient(clientProfile);
    }

    private CommonRequest newCommonRequest(String sysAction) {
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction(sysAction);
        return request;
    }

    @Override
    public SmsSendResult send(SmsMessage smsMessage) {
        Assert.notNull(smsMessage, "Parameter \"smsMessage\" must not null. ");
        try {
            CommonRequest request = newCommonRequest("SendSms");
            request.putQueryParameter("PhoneNumbers", smsMessage.getPhoneNumber());
            request.putQueryParameter("SignName", smsMessage.getSenderName());
            request.putQueryParameter("TemplateCode", smsMessage.getTemplateCode());
            request.putQueryParameter("TemplateParam", JsonUtils.toJsonString(smsMessage.getParameters()));
            log.info("Call alibaba cloud \"SendSms\" with a parameter of \"{}\". ", JsonUtils.toJsonString(smsMessage));
            CommonResponse response = acsClient.getCommonResponse(request);
            log.info("Call alibaba cloud \"SendSms\" and the result is \"{}\". ", response != null ? response.getData() : null);
            String responseData;
            if (response == null || (responseData = response.getData()) == null) {
                throw new SmsException("An error \"no response data\" occurred when calling \"SendSms\" of alibaba cloud. ");
            }
            ParameterizedType type = TypeUtils.parameterizedOf(Map.class, String.class, Object.class);
            Map<String, Object> data = JsonUtils.parseObject(responseData, type);
            SmsSendResult result = new SmsSendResult();
            result.putAll(data);
            String code = String.valueOf(data.get("Code"));
            if (!OK_CODE.equalsIgnoreCase(code)) {
                throw new SmsException("An error \"call failure\" occurred when calling \"SendSms\" of alibaba cloud. ");
            }
            result.setCode(code);
            result.setBusinessId(String.valueOf(data.get("BizId")));
            result.setDescription(String.valueOf(data.get("Message")));
            return result;
        }
        catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    @Override
    public List<SmsSendResult> sendBatch(List<SmsMessage> smsMessageList) {
        Assert.notEmpty(smsMessageList, "Parameter \"smsMessageList\" must not empty. ");
        try {
            List<String> phoneNumberList = new ArrayList<String>();
            List<String> senderNameList = new ArrayList<String>();
            List<Map<String, Object>> parameterList = new ArrayList<Map<String, Object>>();
            String templateCode = null;
            for (SmsMessage smsMessage : smsMessageList) {
                if (smsMessage == null) { continue; }
                phoneNumberList.add(smsMessage.getPhoneNumber());
                senderNameList.add(smsMessage.getSenderName());
                parameterList.add(smsMessage.getParameters());
                String nextCode = smsMessage.getTemplateCode();
                if (StringUtils.isNotBlank(nextCode)) {
                    templateCode = nextCode;
                }
            }
            CommonRequest request = newCommonRequest("SendBatchSms");
            request.putQueryParameter("PhoneNumberJson", JsonUtils.toJsonString(phoneNumberList));
            request.putQueryParameter("SignNameJson", JsonUtils.toJsonString(senderNameList));
            request.putQueryParameter("TemplateCode", templateCode);
            request.putQueryParameter("TemplateParamJson", JsonUtils.toJsonString(parameterList));
            log.info("Call alibaba cloud \"SendBatchSms\" with a parameter of \"{}\". ", JsonUtils.toJsonString(smsMessageList));
            CommonResponse response = acsClient.getCommonResponse(request);
            log.info("Call alibaba cloud \"SendBatchSms\" and the result is \"{}\". ", response != null ? response.getData() : null);
            String responseData;
            if (response == null || (responseData = response.getData()) == null) {
                throw new SmsException("An error \"no response data\" occurred when calling \"SendBatchSms\" of alibaba cloud. ");
            }
            ParameterizedType type = TypeUtils.parameterizedOf(Map.class, String.class, Object.class);
            Map<String, Object> data = JsonUtils.parseObject(responseData, type);
            List<SmsSendResult> resultList = new ArrayList<SmsSendResult>();
            SmsSendResult smsSendResult = new SmsSendResult();
            resultList.add(smsSendResult);
            smsSendResult.putAll(data);
            String code = String.valueOf(data.get("Code"));
            if (!OK_CODE.equalsIgnoreCase(code)) {
                throw new SmsException("An error \"call failure\" occurred when calling \"SendBatchSms\" of alibaba cloud. ");
            }
            smsSendResult.setCode(code);
            smsSendResult.setBusinessId(String.valueOf(data.get("BizId")));
            smsSendResult.setDescription(String.valueOf(data.get("Message")));
            return resultList;
        }
        catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

    @Override
    public SmsQueryResult findOne(SmsQuery smsQuery) {
        Assert.notNull(smsQuery, "Parameter \"smsQuery\" must not null. ");
        smsQuery.setPageSize(ONE);
        List<SmsQueryResult> list = findSelective(smsQuery);
        return CollectionUtils.isNotEmpty(list) ? list.get(ZERO) : null;
    }

    @Override
    public List<SmsQueryResult> findSelective(SmsQuery smsQuery) {
        Assert.notNull(smsQuery, "Parameter \"smsQuery\" must not null. ");
        try {
            CommonRequest request = newCommonRequest("QuerySendDetails");
            request.putQueryParameter("PhoneNumber", smsQuery.getPhoneNumber());
            request.putQueryParameter("SendDate", DateUtils.format(smsQuery.getSendTime(), "yyyyMMdd"));
            request.putQueryParameter("PageSize", String.valueOf(smsQuery.getPageSize()));
            request.putQueryParameter("CurrentPage", String.valueOf(smsQuery.getPageNum()));
            String businessId = smsQuery.getBusinessId();
            if (StringUtils.isNotBlank(businessId)) {
                request.putQueryParameter("BizId", businessId);
            }
            log.info("Call alibaba cloud \"QuerySendDetails\" with a parameter of \"{}\". ", JsonUtils.toJsonString(smsQuery));
            CommonResponse response = acsClient.getCommonResponse(request);
            log.info("Call alibaba cloud \"QuerySendDetails\" and the result is \"{}\". ", response != null ? response.getData() : null);
            String responseData;
            if (response == null || (responseData = response.getData()) == null) {
                throw new SmsException("An error \"no response data\" occurred when calling \"QuerySendDetails\" of alibaba cloud. ");
            }
            ParameterizedType type = TypeUtils.parameterizedOf(Map.class, String.class, Object.class);
            Map<String, Object> data = JsonUtils.parseObject(responseData, type);
            String code = (String) data.get("Code");
            if (!OK_CODE.equalsIgnoreCase(code)) {
                throw new SmsException("An error \"call failure\" occurred when calling \"QuerySendDetails\" of alibaba cloud. ");
            }
            Object smsSendDetailDTOs = data.get("SmsSendDetailDTOs");
            Map<String, Object> toMap = BeanUtils.beanToMap(smsSendDetailDTOs);
            Object smsSendDetailDTO = toMap.get("SmsSendDetailDTO");
            List<Object> list = ObjectUtils.cast(
                    smsSendDetailDTO instanceof List ? smsSendDetailDTO
                            : JsonUtils.parseObject(String.valueOf(smsSendDetailDTO), List.class)
            );
            List<SmsQueryResult> result = new ArrayList<SmsQueryResult>();
            if (CollectionUtils.isEmpty(list)) { return result; }
            for (Object obj : list) {
                if (obj == null) { continue; }
                Map<String, Object> objMap = BeanUtils.beanToMap(obj);
                SmsQueryResult smsQueryResult = new SmsQueryResult();
                smsQueryResult.putAll(objMap);
                smsQueryResult.setPhoneNumber(String.valueOf(objMap.get("PhoneNum")));
                smsQueryResult.setTemplateCode(String.valueOf(objMap.get("TemplateCode")));
                smsQueryResult.setTemplateContent(String.valueOf(objMap.get("Content")));
                smsQueryResult.setSendTime(DateUtils.parse(String.valueOf(objMap.get("SendDate")), DEFAULT_DATE_PATTERN));
                smsQueryResult.setReceiveTime(DateUtils.parse(String.valueOf(objMap.get("ReceiveDate")), DEFAULT_DATE_PATTERN));
                smsQueryResult.setCode(String.valueOf(objMap.get("ErrCode")));
                smsQueryResult.setStatus(String.valueOf(objMap.get("SendStatus")));
                result.add(smsQueryResult);
            }
            return result;
        }
        catch (Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

}
