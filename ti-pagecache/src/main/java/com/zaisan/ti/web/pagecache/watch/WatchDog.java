package com.zaisan.ti.web.pagecache.watch;

import net.sf.ehcache.constructs.blocking.BlockingCache;

import com.zaisan.ti.web.pagecache.rulecenter.PageCacheRule;
import com.zaisan.ti.web.pagecache.rulecenter.RuleCenter;

public interface WatchDog{

    void init(BlockingCache blockingCache,RuleCenter ruleCenter);

    /**
     * @param requestURLAndMethod
     */
    void handleDeleteRule(String requestURLAndMethod);
    
    void handleSaveOrUpdateRule(PageCacheRule pageCacheRule);
    
    /**
     * 支持正则，批量
     * @param requestURLAndMethodList
     */
    void handleExpireCache(String ... requestURLAndMethodList);
    
    /**
     * 
     * @param whatever
     */
    void reloadAllRule(String whatever);
    
    
    /**
     * topic 常量 - 重新加载所有规则
     */
    public static final String TOPIC_RELOADALL="reloadAll";
    
    /**
     * topic 常量 - 让指定页面 缓存失效
     */
    public static final String TOPIC_EXPIRECACHE="expireCache";
    
    /**
     * topic 常量 - 新增或修改指定规则
     */
    public static final String TOPIC_SAVEORUPDATERULE="saveOrUpdateRule";
    
    /**
     * topic 常量 - 删除指定规则
     */
    public static final String TOPIC_DELETERULE="deleteRule";
}
