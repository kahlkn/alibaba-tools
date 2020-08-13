package alibaba.talk;

import artoria.exchange.FastJsonProvider;
import artoria.exchange.JsonUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

@Ignore
public class DingTalkRobotTest {

    @Test
    public void test1() {
        JsonUtils.setJsonProvider(new FastJsonProvider());
        String webHook = "https://oapi.dingtalk.com/robot/send?access_token=xxxxxxxx";
        String secret = "xxxxxxxx";
        DingTalkRobot dingTalkRobot = new DingTalkRobot(webHook, secret);
        List<String> atList = new ArrayList<String>();
        atList.add("16688886666");
        dingTalkRobot.sendMarkdown("测试的", "### 测试测试测试测试\n\n测试拉", false, atList);
    }

}
