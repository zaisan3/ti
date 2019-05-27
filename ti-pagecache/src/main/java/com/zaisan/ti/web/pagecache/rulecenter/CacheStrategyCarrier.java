package com.zaisan.ti.web.pagecache.rulecenter;



/**      
 * web页面请求 缓存 key
* @Description:     
* @author jiajia.sang    
* @createTime 2017年3月7日 上午11:24:40    
*         
*/
public class CacheStrategyCarrier{

    
    private String mainKeySequeueChar;
    
    private String subKeySequeueChar;
    
    private Integer timeToLiveSeconds; 
    
    private String cacheCtrolStr;
    
    public static final String separator = "$";
    
    
    
    public CacheStrategyCarrier(){
        
    }
    
    public CacheStrategyCarrier(String mainKeySequeueChar, String subKeySequeueChar, Integer timeToLiveSeconds){
        super();
        this.mainKeySequeueChar = mainKeySequeueChar;
        this.subKeySequeueChar = subKeySequeueChar;
        this.timeToLiveSeconds = timeToLiveSeconds;
    }


	public String getMainKeySequeueChar() {
		return mainKeySequeueChar;
	}

	public void setMainKeySequeueChar(String mainKeySequeueChar) {
		this.mainKeySequeueChar = mainKeySequeueChar;
	}

	public String getSubKeySequeueChar(){
        return subKeySequeueChar;
    }



    
    public void setSubKeySequeueChar(String subKeySequeueChar){
        this.subKeySequeueChar = subKeySequeueChar;
    }



    public String getCacheKey(){
        return mainKeySequeueChar+separator+subKeySequeueChar;
    }


    
    public Integer getTimeToLiveSeconds(){
        return timeToLiveSeconds;
    }




    
    public void setTimeToLiveSeconds(Integer timeToLiveSeconds){
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

	public String getCacheCtrolStr() {
		return cacheCtrolStr;
	}

	public void setCacheCtrolStr(String cacheCtrolStr) {
		this.cacheCtrolStr = cacheCtrolStr;
	}
    
    
    
}
