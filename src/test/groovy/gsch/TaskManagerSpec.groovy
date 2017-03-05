package gsch

import spock.lang.Shared
import spock.lang.Specification

class TaskManagerSpec extends Specification
{
	@Shared TaskManager taskManager = new TaskManager("localhost", 6379, 1)

	def setup()
	{
		taskManager.clear()
	}

	def cleanupSpec()
	{
		taskManager.shutdown()
	}

	def "should enqueue new job"()
	{
		given:
		taskManager.getCount() == 0

		when:
		taskManager.enqueue(new TestJob())

		then:
		taskManager.getCount() == 1
	}

	def "queue should be a fifo"()
	{
		when:
		Job job1 = new TestJob()
		Job job2 = new TestJob()
		taskManager.enqueue(job1)
		taskManager.enqueue(job2)

		then:
		taskManager.next() == job1
		taskManager.next() == job2
	}

	def "should return null when no jobs in queue"()
	{
		when:
		taskManager.enqueue(new TestJob())

		then:
		taskManager.next() != null
		taskManager.next() == null

		when:
		taskManager.enqueue(new TestJob())

		then:
		taskManager.executeNext() != null
		taskManager.executeNext() == null
	}

	def "should not add jobs to executor queue"()
	{
		when:
		def wait = 1000
		Job job1 = new TestJob(waitTime: wait)
		Job job2 = new TestJob(waitTime: wait)
		taskManager.enqueue(job1)
		taskManager.enqueue(job2)

		then:
		taskManager.executeNext() == job1
		taskManager.executeNext() == null

		then:
		Thread.sleep(wait)
		taskManager.executeNext() == job2
	}

	def "should await jobs for shutdown"()
	{
		when:
		def wait = 1000L
		taskManager.enqueue(new TestJob(waitTime: wait))
		taskManager.executeNext()
		def start = System.currentTimeMillis()
		taskManager.shutdown()

		then:
		def end = System.currentTimeMillis() - start
		end >= wait - 100 && end < wait + 100
	}
}
