package com.zaisan.ti.notify;

import org.springframework.util.Assert;

public class ChannelTopic implements Topic{

	private final String channelName;

	/**
	 * Constructs a new <code>ChannelTopic</code> instance.
	 * 
	 * @param name
	 */
	public ChannelTopic(String name) {
		Assert.notNull(name, "a valid topic is required");
		this.channelName = name;
	}

	/**
	 * Returns the topic name.
	 * 
	 * @return topic name
	 */
	public String getTopic() {
		return channelName;
	}

	@Override
	public int hashCode() {
		return channelName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ChannelTopic)) {
			return false;
		}
		ChannelTopic other = (ChannelTopic) obj;
		if (channelName == null) {
			if (other.channelName != null) {
				return false;
			}
		} else if (!channelName.equals(other.channelName)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return channelName;
	}
}
