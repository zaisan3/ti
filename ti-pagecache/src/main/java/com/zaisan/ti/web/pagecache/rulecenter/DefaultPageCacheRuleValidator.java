package com.zaisan.ti.web.pagecache.rulecenter;

import org.springframework.util.Assert;


public class DefaultPageCacheRuleValidator implements PageCacheRuleValidator{

    @Override
    public void validate(PageCacheRule pageCacheRule){
        Assert.notNull(pageCacheRule);
        Assert.hasLength(pageCacheRule.getMethod());
        Assert.hasLength(pageCacheRule.getUrl());
    }

/*    private void checkCacheSubKeyExpressionIfHas(String cacheSubKeyExpression){
        // TODO Auto-generated method stub
        
    }

    private void checkCustomerAllowCacheExpressionIfHas(String customerAllowCacheExpression){
        // TODO Auto-generated method stub
        
    }*/

}
