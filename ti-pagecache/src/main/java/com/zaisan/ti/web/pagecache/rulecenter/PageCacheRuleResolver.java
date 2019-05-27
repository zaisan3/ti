package com.zaisan.ti.web.pagecache.rulecenter;

import javax.servlet.http.HttpServletRequest;

public interface PageCacheRuleResolver{

    /**
     * 根据 当前配置和当前请求上下文 决定 
     *  1.是否需要cache支持  （若不需要 ，返回 null）
     *  2. 若需要,返回 CacheStrateCarrier,当cache中不存在缓存内容（或已过期），carrier中的
     *  属性指明 本次rebuild时，cache的相关参数设置
     * @param pageCacheRule
     * @param httpRequest
     * @return CacheStrategyCarrier 1.指明 当前请求的cacheKey, 2 本次请求需要rebuild cache时，指明 初始化cache的参数
     */
    CacheStrategyCarrier parse(PageCacheRule pageCacheRule,HttpServletRequest httpRequest,String curRequestMethodAndURL);
}
