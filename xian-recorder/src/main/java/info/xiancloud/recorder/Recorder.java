package info.xiancloud.recorder;

import info.xiancloud.core.Unit;
import info.xiancloud.core.aop.IAllUnitsAop;
import info.xiancloud.core.init.IStartService;
import info.xiancloud.core.message.SingleRxXian;
import info.xiancloud.core.message.UnitRequest;
import info.xiancloud.core.message.UnitResponse;
import info.xiancloud.core.support.cache.api.CacheObjectUtil;
import info.xiancloud.core.util.EnvUtil;
import info.xiancloud.core.util.LOG;
import info.xiancloud.core.util.thread.MsgIdHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Recorder starter.
 * Warning, not fully tested.
 *
 * @author ads, happyyangyuan
 * @deprecated this won't work for asynchronous xian
 */
public class Recorder implements IStartService, IAllUnitsAop {

    @Override
    public boolean asyncAfter() {
        return true;
    }

    @Override
    public Object before(Unit unit, UnitRequest unitRequest) {
        return System.currentTimeMillis();
    }

    @Override
    public void after(Unit unit, UnitRequest unitRequest, UnitResponse unitResponse, Object beforeReturn) {
        //判断该动作来源的业务，是否被过滤，不被录制
        if (!RecorderFilter.filter(unit)) {
            handleRecode(unit, unitRequest, unitResponse, beforeReturn);
            handleRecordItem(unit, unitRequest, unitResponse, beforeReturn);
        }

    }

    /**
     * 处理动作录制
     */
    private void handleRecode(Unit unit, UnitRequest msg, UnitResponse out, Object beforeReturn) {
        String msgId = MsgIdHolder.get();
        String cacheKey = "RecordMessageId_" + msgId;
        CacheObjectUtil
                .exists(cacheKey)
                .subscribe(existed -> {
                    if (existed) {
                        CacheObjectUtil.set(cacheKey, msgId, 60 * 15 * 1000);
                        SourceBody source = getSource(unit, msg);
                        String sourceBody = getSourceBody(msg);
                        Map<String, Object> map = new HashMap<>();
                        map.put("source", source.getSource());
                        map.put("sourceReqBody", sourceBody);
                        map.put("sourceClassPath", source.getPath());
                        map.put("msgId", msgId);
                        map.put("reqTime", String.valueOf(beforeReturn.toString()));
                        SingleRxXian.call("recorder", "actionRecord", map);
                    }
                });
    }

    private void handleRecordItem(Unit unit, UnitRequest request, UnitResponse response, Object beforeReturn) {
        String msgId = MsgIdHolder.get();
        Map<String, Object> map = new HashMap<>();
        map.put("msgId", String.valueOf(msgId.hashCode()));
        map.put("group", unit.getGroup().getName());
        map.put("unit", unit.getName());
        map.put("requestMap", request.argJson().toJSONString());
        map.put("responseCode", response.getCode());
        map.put("responseData", response.getData() != null ? response.getData().toString() : "");
        map.put("requestTime", beforeReturn.toString());
        map.put("responseTime", String.valueOf(System.currentTimeMillis()));
        map.put("cost", String.valueOf(System.currentTimeMillis() - Long.parseLong(beforeReturn.toString())));
        SingleRxXian.call("recorder", "actionItemRecord", map)
                .subscribe(unitResponse -> LOG.info(unitResponse.toJSONString()));
    }

    @Override
    public boolean startup() {
        LOG.info("Recorder won't start by default.");
        return true;
    }

    private class SourceBody {
        private String path;
        private String source;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    /**
     * 获取请求来源
     */
    public SourceBody getSource(Unit unit, UnitRequest msg) {
        SourceBody body = new SourceBody();
        body.setSource(msg.getContext().getUri());
        if (body.getSource() == null || body.getSource().trim().length() <= 0) {
            body.setSource(EnvUtil.getApplication());
        }
        return body;
    }

    private String filterClassName(String className) {
        return className.contains("$") ? className.substring(0, className.indexOf("$")) : className;
    }

    /**
     * 获取请求内容
     */
    public String getSourceBody(UnitRequest msg) {
        String result;
        String source = msg.get("$url", String.class);
        if (source == null || source.trim().length() <= 0) {
            result = msg.argJson().toJSONString();
        } else {
            result = msg.get("$body", String.class);
        }
        return result;
    }
}
