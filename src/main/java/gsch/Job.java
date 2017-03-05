package gsch;

import java.io.Serializable;
import java.util.UUID;

public abstract class Job implements Serializable, Runnable
{
	private final UUID id;

	public Job()
	{
		id = UUID.randomUUID();
	}

	public UUID getId()
	{
		return id;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		Job job = (Job) o;
		return id != null ? id.equals(job.id) : job.id == null;
	}

	@Override
	public int hashCode()
	{
		return id != null ? id.hashCode() : 0;
	}
}
