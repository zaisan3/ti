package com.zaisan.ti.web.pagecache.rulecenter;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.WebApplicationContext;

public interface RuleCenter{

    void init(FilterConfig filterConfig,WebApplicationContext webApplicationContext);

    PageCacheRule deleteRuleIfExist(String requestURLAndMethod);

    PageCacheRule saveOrUpdateRule(PageCacheRule pageCacheRule);

    /**
     * @param httpRequest
     * @return 若不需要 pageCache 快速返回 null
     */
    CacheStrategyCarrier calculateKey(HttpServletRequest httpRequest);

    void destroy();
    
    /**
     * 重新加载最新的规则配置
     */
    void reloadAllRule();

}
