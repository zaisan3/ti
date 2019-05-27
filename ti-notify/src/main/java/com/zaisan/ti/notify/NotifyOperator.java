package com.zaisan.ti.notify;


import com.zaisan.ti.notify.result.NoticeResult;

/**
 * 通知操作类
 * @author jiajia.sang
 *
 */
public interface NotifyOperator {
	
	/** 
	 * 向broker中指定的频道投递消息 （消息内容默认为当前时间的字符串）
	 * @param channelName 频道名称 
	 * @return 各个 consumer的消费情况反馈
	 */
	Future<NoticeResult> noticeCluster(String channelName);
	
	/**
	 * 向broker中指定的频道投递指定内容的消息 
	 * @param channelName 频道名称 
	 * @param data 投递的消息内容
	 * @return 各个 consumer的消费情况反馈
	 */
	Future<NoticeResult> noticeCluster(String channelName,String data);
	
}
