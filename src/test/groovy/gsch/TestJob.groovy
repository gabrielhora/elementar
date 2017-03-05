package gsch

class TestJob extends Job
{
	Integer waitTime = 0

	@Override
	void run()
	{
		println("This is a test job")
		Thread.sleep(waitTime)
	}
}
