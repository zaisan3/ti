package com.zaisan.ti.web.pagecache.watch;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.constructs.blocking.BlockingCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import com.zaisan.ti.web.pagecache.rulecenter.CacheStrategyCarrier;
import com.zaisan.ti.web.pagecache.rulecenter.PageCacheRule;
import com.zaisan.ti.web.pagecache.rulecenter.RuleCenter;

public class RedisWatchDog implements WatchDog {

	private static final Logger LOG = LoggerFactory
			.getLogger(RedisWatchDog.class);

	private volatile BlockingCache blockingCache;

	private volatile RuleCenter ruleCenter;

	private PathMatcher pathMatcher = new AntPathMatcher();
	
	@Override
	public void init(BlockingCache blockingCache, RuleCenter ruleCenter) {
		this.blockingCache = blockingCache;
		this.ruleCenter = ruleCenter;
	}

	@Override
	public void handleDeleteRule(String requestURLAndMethod) {
		ruleCenter.deleteRuleIfExist(requestURLAndMethod); // 去掉规则后 ，请求 不在
															// 查询缓存了，也不会 重新生成
															// 缓存内容 并 放入缓存中

		handleExpireCache(requestURLAndMethod); // 过期处理 （ 即 为删除）

	}

	@Override
	public void handleSaveOrUpdateRule(PageCacheRule pageCacheRule) {
		String requestURLAndMethod = pageCacheRule.getRequestURLAndMethod();
		PageCacheRule oldRule = ruleCenter
				.deleteRuleIfExist(requestURLAndMethod);
		if (oldRule != null) { // 修改规则时， 去掉 之前的缓存 内容，之后 会重新生成
			handleExpireCache(requestURLAndMethod);
		}
		ruleCenter.saveOrUpdateRule(pageCacheRule);
	}

	@Override
	public void handleExpireCache(String... requestURLAndMethodList) {
		if (requestURLAndMethodList == null
				|| requestURLAndMethodList.length <= 0) {
			return;
		}
		List<String> list = blockingCache.getKeysNoDuplicateCheck();
		if (list == null || list.size() <= 0) {
			return;
		}

		List<String> needRemoveKey = new ArrayList<String>();

		for (String requestURLAndMethod : requestURLAndMethodList) {
			for (String existCacheKey : list) {
				if (existCacheKey.startsWith(requestURLAndMethod)) {
					needRemoveKey.add(existCacheKey);
				} else {
					String urlInCacheKey = existCacheKey.subSequence(0,
							existCacheKey
									.indexOf(CacheStrategyCarrier.separator)).toString();
					if(pathMatcher.match(requestURLAndMethod, urlInCacheKey)){
						needRemoveKey.add(existCacheKey);
					}
				}
			}
		}
		doRemoveCache(needRemoveKey);
	}

	private void doRemoveCache(List<String> needRemoveKey) {
		if (needRemoveKey == null || needRemoveKey.size() <= 0) {
			return;
		}
		IllegalStateException failed = null;
		for (String key : needRemoveKey) {
			try {
				blockingCache.remove(key);
			} catch (IllegalStateException e) {
				LOG.warn("remove cache fail,cacheKey:" + key, e);
				// 继续下一个 remove
				failed = e;
			}
		}
		if (failed != null) {
			throw failed;
		}
	}

	@Override
	public void reloadAllRule(String whatever) {
		//可能 filter还没有初始化（filter是延时初始化的）
		if(ruleCenter==null)return;
		synchronized (this) {
			if(ruleCenter!=null){
				ruleCenter.reloadAllRule();		
			}
		}
	}

}
