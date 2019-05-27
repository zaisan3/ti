package com.zaisan.ti.notify.message;

public class NotifyMessage {

	private String messageId;
	private String channelName;
	private String content;
	
	public NotifyMessage(String messageId, String channelName, String content) {
		super();
		this.messageId = messageId;
		this.channelName = channelName;
		this.content = content;
	}
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getChannelName() {
		return channelName;
	}
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
}
