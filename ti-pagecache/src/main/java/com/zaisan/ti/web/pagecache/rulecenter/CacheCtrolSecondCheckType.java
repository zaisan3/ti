package com.zaisan.ti.web.pagecache.rulecenter;

/**
 * cache-control 二次验证，表示精细控制。它的几个重要属性是： immutable：表示文档是不能更改的。
 * must-revalidate：表示客户端（浏览器）必须检查代理服务器上是否存在，即使它已经本地缓存了也要检查。
 * proxy-revalidata：表示共享缓存（CDN）必须要检查源是否存在，即使已经有缓存。
 * 
 * @author jumbo
 *
 */
public enum CacheCtrolSecondCheckType {

	immutable("immutable"),

	mustRevalidate("must-revalidate"),

	proxyRevalidate("proxy-revalidate");

	private String nameStr;

	CacheCtrolSecondCheckType(String nameStr) {
		this.nameStr = nameStr;
	}

	public String getNameStr() {
		return this.nameStr;
	}

}
