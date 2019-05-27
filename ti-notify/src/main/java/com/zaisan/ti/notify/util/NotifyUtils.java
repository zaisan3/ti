package com.zaisan.ti.notify.util;

import java.util.concurrent.atomic.AtomicInteger;

public class NotifyUtils {

	
	public static AtomicInteger count=new AtomicInteger(0);
	public static volatile long curTime= -1L;
	public static String getCurNodeIdStr(){
		return NetUtils.getLocalHost()+"-"+NetUtils.getProcessID();
	}
	
	public static synchronized String genernateMessageId(){
		StringBuilder sb  = new StringBuilder();
		sb.append(getCurNodeIdStr());
		long now = System.currentTimeMillis();
		if(now!=curTime){
			curTime=now;
			count.set(0);
		}
		sb.append(curTime);
		sb.append(count.incrementAndGet());
		return sb.toString();
	}
}
