package com.zaisan.ti.web.pagecache.rulecenter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PageCacheRule implements Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 4281181118698960370L;

	/**
	 * 该规则 适用的请求 类型（GET/POST）
	 */
	private String method;

	/**
	 * 该规则 适用的请求 路径（不带 查询参数）
	 */
	private String url;
	
	/**
	 * url中使用Ant语法
	 */
	private boolean urlWithAntStyle;

	/**
	 * 该规则 目前 的 状态 （开启1 ，关闭 -1）,默认开启
	 */
	private int status = 1;

	/**
	 * 该规则生效 开始时间 （不设置表示 规则一直有效）（默认null,不设置）
	 */
	private Date startDate;

	/**
	 * 该规则有效的 结束时间 （不设置表示 规则一直有效） （默认null,不设置）
	 */
	private Date endDate;

	/**
	 * 指定 参与缓存key生成的 查询参数列表 （若为空，默认将所有 查询参数拼装到key中）
	 */
	private List<String> paramKeyList;

	/**
	 * 配置 某个查询参数 必须在 指定值域中， 才能提供缓存 服务
	 */
	private Map<String, List<Object>> paramValueIn;

	/**
	 * 配置 某个查询参数 必须 不能再指定值域中， 才能提供缓存 服务
	 */
	private Map<String, List<Object>> paramValueNotIn;

	private Integer timeToLiveSeconds;

	/** cache-ctrol 区域 start */

	/**
	 * cache-ctrol 的缓冲能力 【请求经过CDN时 才需设置，不设置 那就为空】
	 */
	private CacheCtrolScope cacheCtrolScope;

	/**
	 * 单位 为 秒 不可大于 timeToLiveSeconds 【不设置时或超过时 默认为 timeToLiveSeconds】
	 */
	private Integer maxAgeOfCacheCtrol;

	/**
	 * 单位 为 秒 不可大于 timeToLiveSeconds 【请求经过CDN时 才需设置,不设置时或超过时 默认为
	 * timeToLiveSeconds】
	 */
	private Integer smaxAgeOfCacheCtrol;

	/**
	 * cache-ctrol的二次验证方式 【请求经过CDN时 才需设置，不设置时 默认为 must-revalidate】
	 */
	private CacheCtrolSecondCheckType cacheCtrolSecondCheckType;

	/** cache-ctrol 区域 end */

	private String finalCacheCtrolStr;

	public static final int STATUS_ENABLED = 1;

	public static final int STATUS_DISABLED = -1;

	public static final String CACHESUBKEY_EXPRESSION_QUERYSTRING = "{{#by}}httpRequest.getQueryString(){{/by}}";

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
		if(null != url && !url.isEmpty()){
			if(url.contains("*") || url.contains("?")){
				this.urlWithAntStyle=true;
			}
		}
	}


	
	public boolean isUrlWithAntStyle() {
		return urlWithAntStyle;
	}

	/**
	 * 请不要调用该接口（只是为了序列化）
	 * @param urlWithAntStyle
	 */
	public void setUrlWithAntStyle(boolean urlWithAntStyle) {
		this.urlWithAntStyle = urlWithAntStyle;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Integer getTimeToLiveSeconds() {
		return timeToLiveSeconds;
	}

	public void setTimeToLiveSeconds(Integer timeToLiveSeconds) {
		this.timeToLiveSeconds = timeToLiveSeconds;
	}

	public String getRequestURLAndMethod() {
		return method + url;
	}

	public boolean isEnabled() {
		return status == STATUS_ENABLED;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public List<String> getParamKeyList() {
		if (paramKeyList == null)
			return null;
		return Collections.unmodifiableList(paramKeyList);
	}

	public void addParamKey(String paramKey) {
		if (this.paramKeyList == null) {
			this.paramKeyList = new ArrayList<String>();
		}
		this.paramKeyList.add(paramKey);
	}

	public boolean isParamValueIn(String paramName, String testValue) {
		if (paramValueIn.containsKey(paramName)) {
			if (paramValueIn.get(paramName) != null) {
				return paramValueIn.get(paramName).contains(testValue);
			}
		}
		return false;
	}

	public void addParamValueIn(String paramName, List<Object> valueInList) {
		if (this.paramValueIn == null) {
			this.paramValueIn = new HashMap<String, List<Object>>();
		}
		this.paramValueIn.put(paramName, valueInList);
	}

	public boolean isParamValueNotIn(String paramName, String testValue) {
		if (paramValueNotIn.containsKey(paramName)) {
			if (paramValueNotIn.get(paramName) != null) {
				return paramValueNotIn.get(paramName).contains(testValue);
			}
		}
		return false;
	}

	public void addParamValueNotIn(String paramName, List<Object> valueInList) {
		if (this.paramValueNotIn == null) {
			this.paramValueNotIn = new HashMap<String, List<Object>>();
		}
		this.paramValueNotIn.put(paramName, valueInList);
	}

	public Set<String> getParamKeyOfParamValueIn() {
		if (paramValueIn == null)
			return null;
		return Collections.unmodifiableSet(this.paramValueIn.keySet());
	}

	public Set<String> getParamKeyOfParamValueNotIn() {
		if (paramValueNotIn == null)
			return null;
		return Collections.unmodifiableSet(this.paramValueNotIn.keySet());
	}
	/**
	 * cache-ctrol 的缓冲能力 【请求经过CDN时 才需设置，不设置 那就为空】
	 */
	public CacheCtrolScope getCacheCtrolScope() {
		return cacheCtrolScope;
	}

	public void setCacheCtrolScope(CacheCtrolScope cacheCtrolScope) {
		this.cacheCtrolScope = cacheCtrolScope;
	}

	/**
	 * 单位 为 秒 不可大于 timeToLiveSeconds 【不设置时或超过时 默认为 timeToLiveSeconds】
	 */
	public Integer getMaxAgeOfCacheCtrol() {
		return maxAgeOfCacheCtrol;
	}

	public void setMaxAgeOfCacheCtrol(Integer maxAgeOfCacheCtrol) {
		this.maxAgeOfCacheCtrol = maxAgeOfCacheCtrol;
	}

	/**
	 * 单位 为 秒 不可大于 timeToLiveSeconds 【请求经过CDN时 才需设置,不设置时或超过时 默认为
	 * timeToLiveSeconds】
	 */
	public Integer getSmaxAgeOfCacheCtrol() {
		return smaxAgeOfCacheCtrol;
	}

	public void setSmaxAgeOfCacheCtrol(Integer smaxAgeOfCacheCtrol) {
		this.smaxAgeOfCacheCtrol = smaxAgeOfCacheCtrol;
	}

	/**
	 * cache-ctrol的二次验证方式 【请求经过CDN时 才需设置，不设置时 默认为 must-revalidate】
	 */
	public CacheCtrolSecondCheckType getCacheCtrolSecondCheckType() {
		return cacheCtrolSecondCheckType;
	}

	public void setCacheCtrolSecondCheckType(
			CacheCtrolSecondCheckType cacheCtrolSecondCheckType) {
		this.cacheCtrolSecondCheckType = cacheCtrolSecondCheckType;
	}

	String getFinalCacheCtrolStr() {
		if (finalCacheCtrolStr == null) {
			synchronized (this) {
				if (finalCacheCtrolStr == null) {
					finalCacheCtrolStr = genernateCacheCtrolStr();
				}
			}
		}
		return finalCacheCtrolStr;
	}

	private String genernateCacheCtrolStr() {
		String douhao = ",";
		StringBuilder sb = new StringBuilder();
		if (this.getCacheCtrolScope() != null) {
			sb.append(this.getCacheCtrolScope().getNameStr()).append(douhao);
		}
		if (this.getMaxAgeOfCacheCtrol() != null) {
			sb.append("max-age").append("=")
					.append(this.getMaxAgeOfCacheCtrol()).append(douhao);
		} 

		if (this.getSmaxAgeOfCacheCtrol() != null) {
			sb.append("s-maxage").append("=")
					.append(this.getSmaxAgeOfCacheCtrol()).append(douhao);
		} 
		
		

		if (this.getCacheCtrolSecondCheckType() != null) {
			sb.append(this.getCacheCtrolSecondCheckType().getNameStr()).append(douhao);
		}
		if(sb.length()>0){
			return sb.substring(0, sb.lastIndexOf(douhao));
		}
		return sb.toString();
	}
}
