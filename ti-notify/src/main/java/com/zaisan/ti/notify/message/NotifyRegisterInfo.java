package com.zaisan.ti.notify.message;

import com.zaisan.ti.notify.constants.NotifyConstants;
import com.zaisan.ti.notify.maptable.MapTableOperator;

public class NotifyRegisterInfo {

	private long survivalTime;
	
	private String subcribeNodeId;

	private long subcribeTime;
	
	
	public long getSurvivalTime() {
		return survivalTime;
	}

	public void setSurvivalTime(long survivalTime) {
		this.survivalTime = survivalTime;
	}

	public String getSubcribeNodeId() {
		return subcribeNodeId;
	}

	public void setSubcribeNodeId(String subcribeNodeId) {
		this.subcribeNodeId = subcribeNodeId;
	}

	public long getSubcribeTime() {
		return subcribeTime;
	}

	public void setSubcribeTime(long subcribeTime) {
		this.subcribeTime = subcribeTime;
	}

	public boolean isAlive(){
		return this.survivalTime>System.currentTimeMillis();
	}
	
	public boolean isZombieNode(){
		return System.currentTimeMillis()- this.survivalTime>2*NotifyConstants.HEART_BEAT_PERIOD*1000;
	}
	
	public boolean registerBeforeMsgSend(long msgSendTime){
		return this.subcribeTime<msgSendTime;
	}
}
