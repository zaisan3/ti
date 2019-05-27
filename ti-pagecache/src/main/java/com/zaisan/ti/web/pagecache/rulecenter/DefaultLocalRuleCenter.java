package com.zaisan.ti.web.pagecache.rulecenter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.WebApplicationContext;

public class DefaultLocalRuleCenter implements RuleCenter{

    private volatile ConcurrentMap<String, PageCacheRule> ruleMap = new ConcurrentHashMap<String, PageCacheRule>(1024);

    private PageCacheRuleLoader pageCacheRuleLoader;

    private PageCacheRuleResolver pageCacheRuleResolver;

    private PageCacheRuleValidator pageCacheRuleValidator = new DefaultPageCacheRuleValidator();
    

	private PathMatcher pathMatcher = new AntPathMatcher();
	
	
    
    public void init(FilterConfig filterConfig,WebApplicationContext webApplicationContext){
    	pageCacheRuleResolver = (PageCacheRuleResolver) webApplicationContext.getBean("pageCacheRuleResolver");
        pageCacheRuleLoader = (PageCacheRuleLoader) webApplicationContext.getBean("pageCacheRuleLoader");
        loadAllRule();
        
        
    }

    public CacheStrategyCarrier calculateKey(HttpServletRequest httpRequest){
        String curRequestURL = httpRequest.getMethod() + httpRequest.getRequestURI();

        PageCacheRule pageCacheRule = findPageCacheRule(curRequestURL);

        if (pageCacheRule == null){ // get/post +url ===> baseKey 不存在，不应用pageCache 
            return null;
        }
        return pageCacheRuleResolver.parse(pageCacheRule, httpRequest,curRequestURL);
    }

    private PageCacheRule findPageCacheRule(String keyBasePart){
    	if(ruleMap.containsKey(keyBasePart)){ // 精确匹配
    		return ruleMap.get(keyBasePart);
    	}
    	for (String ruleKey:ruleMap.keySet()) { 
    		if(ruleMap.get(ruleKey).isUrlWithAntStyle() &&
    				this.pathMatcher.match(ruleKey, keyBasePart)){
    			return ruleMap.get(ruleKey);
    		}
		}
    	
    	return null;
    }
    
    public void destroy(){
        ruleMap.clear();
        ruleMap = null;
    }

    private void loadAllRule(){
        ruleMap.clear();
        List<PageCacheRule> ruleList = pageCacheRuleLoader.loadAllPageCacheRule();
        if (ruleList != null && ruleList.size() > 0){
            for (PageCacheRule pageCacheRule : ruleList){
                pageCacheRuleValidator.validate(pageCacheRule);
                String requestURLAndMethod = pageCacheRule.getMethod() + pageCacheRule.getUrl();
                ruleMap.put(requestURLAndMethod, pageCacheRule);
            }
        }
    }

    @Override
    public PageCacheRule deleteRuleIfExist(String requestURLAndMethod){
       return ruleMap.remove(requestURLAndMethod);
    }

    @Override
    public PageCacheRule saveOrUpdateRule(PageCacheRule pageCacheRule){
        pageCacheRuleValidator.validate(pageCacheRule);
        String requestURLAndMethod = pageCacheRule.getRequestURLAndMethod();
        return ruleMap.put(requestURLAndMethod, pageCacheRule);
    }

	@Override
	public void reloadAllRule() {
		ConcurrentMap<String, PageCacheRule> newMap = new ConcurrentHashMap<String, PageCacheRule>(1024);
        List<PageCacheRule> ruleList = pageCacheRuleLoader.loadAllPageCacheRule();
        if (ruleList != null && ruleList.size() > 0){
            for (PageCacheRule pageCacheRule : ruleList){
                pageCacheRuleValidator.validate(pageCacheRule);
                String requestURLAndMethod = pageCacheRule.getMethod() + pageCacheRule.getUrl();
                newMap.put(requestURLAndMethod, pageCacheRule);
            }
        }
        ruleMap = newMap; //最后把指针指回来
	}
    
  
}
