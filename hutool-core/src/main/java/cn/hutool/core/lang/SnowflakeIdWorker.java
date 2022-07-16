package cn.hutool.core.lang;

import java.security.SecureRandom;

/**
 * 雪花数，提供 Twitter 雪花算法的 Java 实现
 *
 * <p></>
 * 1、机器id生成
 */
public class SnowflakeIdWorker {

	/**
	 * 开始时间戳（使用自己业务系统上线的时间）
	 */
	private final long epoch = 1657951904808L;

	/**
	 * 机器在id所占的位数
	 */
	private final long workerIdBits = 10L;

	/**
	 * 序列在id所占的位数
	 */
	private final long sequenceBits = 12L;

	/**
	 * 支持的最大机器id
	 */
	public final long maxWorkerId = ~(-1L << workerIdBits);

	/**
	 * 生成序列的掩码
	 */
	public final long sequenceMask = ~(-1L << sequenceBits);

	/**
	 * 机器id左移位数
	 */
	private final long workerIdLeftShift = sequenceBits;

	/**
	 * 时间戳左移位数
	 */
	private final long timestampLeftShift = sequenceBits + workerIdBits;

	/**
	 * 工作机器id
	 */
	private long workerId;

	/**
	 * 毫秒内序列
	 */
	private long sequence = 0L;

	/**
	 * 上次生成id的时间戳
	 */
	private long lastTimestamp = -1;

	/**
	 * 构造函数
	 *
	 * @param workerId 工作机器id
	 */
	public SnowflakeIdWorker(long workerId) {
		if (workerId > maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(String.format("WorkerId can't be greater than %d or less than 0", maxWorkerId));
		}
		this.workerId = workerId;
	}

	public synchronized long nextId() {
		long timestamp = timeGen();

		// 如果当前时间小于上一次id生成的时间戳，说明系统时钟回退过这个时候应该抛出异常
		if (timestamp < lastTimestamp) {
			throw new RuntimeException(
					String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
		}

		// 如果是同一时间生成的，则进行毫秒内序列
		if (timestamp == lastTimestamp) {
			sequence = (sequence + 1) & sequenceMask;
			// 毫秒内序列溢出
			if (sequence == 0) {
				// 阻塞到下一个毫秒，获得新的时间戳
				timestamp = tillNextMillis(lastTimestamp);
			}
		}
		// 时间戳改变，毫秒内序列重置
		else {
			// 为了保证尾数随机性更大一些，最后再设置一个随机数
			sequence = new SecureRandom().nextInt(100);
		}

		lastTimestamp = timestamp;

		// 移位并通过或运算拼到一起组成64位的id
		return ((timestamp - epoch) << timestampLeftShift)
				| (workerId << workerIdLeftShift)
				| sequence;
	}

	/**
	 * 阻塞到下一个毫秒，直到获得新的时间戳
	 *
	 * @param lastTimestamp 上次生成id的时间戳
	 * @return 当前时间戳
	 */
	private long tillNextMillis(long lastTimestamp) {
		long timestamp = timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}
		return timestamp;
	}

	/**
	 * 返回以毫秒为单位的当前时间
	 *
	 * @return 当前时间（毫秒）
	 */
	private long timeGen() {
		return System.currentTimeMillis();
	}
}
