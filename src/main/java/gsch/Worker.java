package gsch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker
{
	private static final Logger LOG = LoggerFactory.getLogger(Worker.class);

	private final Integer interval;
	private final TaskManager taskManager;
	private boolean run = true;

	/**
	 * Create a new Worker class.
	 *
	 * @param redisHost dns or ip for the redis server
	 * @param redisPort port number for the redis server
	 * @param interval pooling interval in milliseconds
	 * @param threads number of threads for this worker
	 */
	public Worker(String redisHost, Integer redisPort, Integer interval, Integer threads)
	{
		this.interval = interval;
		this.taskManager = new TaskManager(redisHost, redisPort, threads);
	}

	/**
	 * Start the work loop.
	 */
	public void work()
	{
		while (run)
		{
			try
			{
				Job job = taskManager.executeNext();
				if (job == null)
				{
					Thread.sleep(interval);
				}
			}
			catch (Exception exception)
			{
				if (exception instanceof InterruptedException)
				{
					shutdown();
				}
				else
				{
					LOG.error("Exception while processing worker loop", exception);
				}
			}
		}
	}

	/**
	 * Shutdown the work loop.
	 */
	public void shutdown()
	{
		LOG.info("Shuting down worker");
		taskManager.shutdown();
		run = false;
	}

	public static void main(String[] args)
	{
		String redisHost = System.getProperty("redis_host", "localhost");
		Integer redisPort = Integer.valueOf(System.getProperty("redis_port", "6379"));
		Integer interval = Integer.valueOf(System.getProperty("interval", "1000"));
		Integer threads = Integer.valueOf(System.getProperty("threads", "50"));

		new Worker(redisHost, redisPort, interval, threads).work();
	}
}
