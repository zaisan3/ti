package com.zaisan.ti.notify.message;

public class NotifyFeedbackPack {

	private String clientId;
	private boolean handlerSuccess;
	private String msg;
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public boolean isHandlerSuccess() {
		return handlerSuccess;
	}
	public void setHandlerSuccess(boolean handlerSuccess) {
		this.handlerSuccess = handlerSuccess;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	
}
