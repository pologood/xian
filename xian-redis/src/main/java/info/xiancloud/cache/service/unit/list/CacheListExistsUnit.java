package info.xiancloud.cache.service.unit.list;

import info.xiancloud.cache.redis.Redis;
import info.xiancloud.cache.service.CacheGroup;
import info.xiancloud.core.Group;
import info.xiancloud.core.Input;
import info.xiancloud.core.Unit;
import info.xiancloud.core.UnitMeta;
import info.xiancloud.core.message.UnitRequest;
import info.xiancloud.core.message.UnitResponse;
import info.xiancloud.core.support.cache.CacheConfigBean;

import java.util.function.Consumer;

/**
 * List Exists
 *
 * @author John_zero, happyyangyuan
 */
public class CacheListExistsUnit implements Unit {

    @Override
    public String getName() {
        return "cacheListExists";
    }

    @Override
    public Group getGroup() {
        return CacheGroup.singleton;
    }

    @Override
    public UnitMeta getMeta() {
        return UnitMeta.create("List Exists").setPublic(false);
    }

    @Override
    public Input getInput() {
        return new Input().add("key", String.class, "缓存的关键字", REQUIRED)
                .add("cacheConfig", CacheConfigBean.class, "", NOT_REQUIRED);
    }

    @Override
    public void execute(UnitRequest msg, Consumer<UnitResponse> consumer) {
        String key = msg.getArgMap().get("key").toString();
        CacheConfigBean cacheConfigBean = msg.get("cacheConfig", CacheConfigBean.class);
        try {
            boolean result = Redis.call(cacheConfigBean, jedis -> jedis.exists(key));
            return UnitResponse.success(result);
        } catch (Exception e) {
            return UnitResponse.exception(e);
        }
    }

}
