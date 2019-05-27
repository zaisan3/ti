package com.zaisan.ti.web.pagecache.rulecenter;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

public class DefaultPageCacheRuleResolver implements PageCacheRuleResolver {

	@Override
	public CacheStrategyCarrier parse(PageCacheRule pageCacheRule,
			HttpServletRequest httpRequest, String curRequestMethodAndURL) {

		if (!allowCache(pageCacheRule, httpRequest)) {
			return null;
		}

		CacheStrategyCarrier cacheStrategyCarrier = new CacheStrategyCarrier();
		cacheStrategyCarrier.setMainKeySequeueChar(curRequestMethodAndURL);
		cacheStrategyCarrier.setSubKeySequeueChar(generateCacheSubKey(
				pageCacheRule, httpRequest));
		cacheStrategyCarrier.setTimeToLiveSeconds(pageCacheRule
				.getTimeToLiveSeconds());
		cacheStrategyCarrier
				.setCacheCtrolStr(pageCacheRule.getFinalCacheCtrolStr());
		return cacheStrategyCarrier;
	}


	private boolean allowCache(PageCacheRule pageCacheRule,
			HttpServletRequest httpRequest) {

		// 开始基本 通用的 语法 校验

		if (!pageCacheRule.isEnabled()) {
			return false;
		}
		if (pageCacheRule.getStartDate() != null) {
			if (new Date().before(pageCacheRule.getStartDate())) { // 还没到时间
				return false;
			}
		}
		if (pageCacheRule.getEndDate() != null) {
			if (new Date().after(pageCacheRule.getEndDate())) { // 到点 下班了
				return false;
			}
		}

		Set<String> inConditions = pageCacheRule.getParamKeyOfParamValueIn();
		if (inConditions != null && inConditions.size() > 0) {
			for (String mustInParamName : inConditions) {
				if (!pageCacheRule.isParamValueIn(mustInParamName,
						httpRequest.getParameter(mustInParamName))) {
					return false;
				}
			}
		}

		Set<String> notInConditions = pageCacheRule
				.getParamKeyOfParamValueNotIn();
		if (notInConditions != null && notInConditions.size() > 0) {
			for (String mustNotInParamName : notInConditions) {
				if (pageCacheRule.isParamValueNotIn(mustNotInParamName,
						httpRequest.getParameter(mustNotInParamName))) {
					return false;
				}
			}
		}
		return true;
	}

	/*
	 * private boolean callCustomerAllowCacheCommand(String
	 * customerAllowCacheExpression,HttpServletRequest httpRequest){
	 * 
	 * return true; }
	 */

	private String generateCacheSubKey(PageCacheRule pageCacheRule,
			HttpServletRequest httpRequest) {

		List<String> paramKeyList = pageCacheRule.getParamKeyList();

		if (paramKeyList == null || paramKeyList.size() <= 0) { // 默认全拼
			return httpRequest.getQueryString();
		}
		StringBuilder sb = new StringBuilder();
		for (String paramKey : paramKeyList) {
			sb.append(paramKey).append("=")
					.append(httpRequest.getParameter(paramKey));
		}
		return sb.toString();
	}
}
