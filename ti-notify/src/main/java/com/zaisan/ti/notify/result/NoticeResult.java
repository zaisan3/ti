package com.zaisan.ti.notify.result;

public interface NoticeResult {

	boolean isAllClientSuccess();
	
	String getResultString();
	
	public static final NoticeResult success = new SuccessNoticeResult();
	
	public class SuccessNoticeResult implements NoticeResult{

		@Override
		public boolean isAllClientSuccess() {
			return true;
		}

		@Override
		public String getResultString() {
			return "all-success";
		}
		
	}
}
