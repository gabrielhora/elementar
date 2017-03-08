package gsch;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskManager
{
	private static final Logger LOG = LoggerFactory.getLogger(TaskManager.class);
	private static final String JOB_QUEUE_KEY = "job-queue";
	private static final String JOB_LOCK_KEY = "job-queue-lock";

	private final RedissonClient client;
	private final RQueue<Job> jobQueue;
	private final ThreadPoolExecutor executor;

	public TaskManager(String redisHost, Integer redisPort)
	{
		this(redisHost, redisPort, 50);
	}

	public TaskManager(String redisHost, Integer redisPort, Integer threads)
	{
		this(redisHost, redisPort, (ThreadPoolExecutor) Executors.newFixedThreadPool(threads));
	}

	public TaskManager(String redisHost, Integer redisPort, ThreadPoolExecutor executor)
	{
		Config config = new Config();
		config.useSingleServer().setAddress(String.format("%s:%d", redisHost, redisPort));
		client = Redisson.create(config);
		this.executor = executor;
		jobQueue = client.getQueue(JOB_QUEUE_KEY);
	}

	/**
	 * Execute and return the next job on the queue if there is any, otherwise return null.
	 *
	 * @return the executed job or null if the queue is empty
	 */
	public Job executeNext()
	{
		if (!emptySlot())
		{
			LOG.info("All threads are currently in use");
			return null;
		}

		Job job = next();
		if (job != null)
		{
			LOG.info("Executing job " + job.getId());
			executor.execute(job);
			return job;
		}
		else
		{
			return null;
		}
	}

	/**
	 * Get the next {@link Job} in the queue. This is a synchronized action.
	 *
	 * @return the next job on the queue or null if the queue is empty
	 */
	public Job next()
	{
		RLock lock = client.getFairLock(JOB_LOCK_KEY);
		if (lock.tryLock())
		{
			try
			{
				return jobQueue.poll();
			}
			finally
			{
				lock.unlock();
			}
		}
		else
		{
			LOG.info("Could not acquire lock");
			return null;
		}
	}

	/**
	 * Add the {@link Job} to the end of the execution queue.
	 *
	 * @see java.util.Queue#add(Object)
	 * @param job the job instance to enqueue
	 * @return {@code true}
	 */
	public boolean enqueue(Job job)
	{
		return jobQueue.add(job);
	}

	/**
	 * Retrieve the number of jobs available in the queue.
	 *
	 * @return the number of jobs in the queue
	 */
	public int getCount()
	{
		return jobQueue.size();
	}

	/**
	 * Remove all jobs from the queue.
	 */
	public void clear()
	{
		jobQueue.clear();
	}

	/**
	 * Shutdown the {@link Executor} for this {@link TaskManager}.
	 */
	public void shutdown()
	{
		if (executor.isTerminating() || executor.isShutdown() || executor.isTerminated())
		{
			LOG.info("Executor is either already terminated or in the process of terminating");
			return;
		}

		LOG.info("Shuting down, awaiting current jobs...");
		executor.shutdown();
		try
		{
			executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			LOG.info("Shutdown complete");
		}
		catch (InterruptedException e)
		{
			LOG.info("Forcing shutdown");
		}
	}

	private Boolean emptySlot()
	{
		return (executor.getCorePoolSize() - executor.getActiveCount()) > 0;
	}
}
