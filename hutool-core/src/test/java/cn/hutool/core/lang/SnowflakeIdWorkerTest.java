package cn.hutool.core.lang;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.thread.ThreadUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class SnowflakeIdWorkerTest {

    /**
     * 测试snowflakeId是否存在重复问题
     */
    @Test
    public void snowflakeIdTest() {
        SnowflakeIdWorker idWorker = new SnowflakeIdWorker(666L);

        Set<Long> set = new ConcurrentHashSet<>(2000);
        ThreadUtil.concurrencyTest(2000, () -> set.add(idWorker.nextId()));
        Assert.assertEquals(2000, set.size());
    }
}
