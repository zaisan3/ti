package com.zaisan.ti.web.pagecache.rulecenter;

/**
 * cache-ctrol 的缓冲能力，它关注的是缓存到什么地方，和是否应该被缓存。他的几个重要的属性是：
 * 
 * private：表示它只应该存在本地缓存； 
 * public：表示它既可以存在共享缓存，也可以被存在本地缓存；
 * no-cache：表示不论是本地缓存还是共享缓存，在使用它以前必须用缓存里的值来重新验证； no-store：表示不允许被缓存。
 * 
 * @author jumbo
 *
 */
public enum CacheCtrolScope {

	privatex("private"),

	publicx("public"),

	nocache("no-cache"),

	nostore("no-store");

	private String nameStr;

	CacheCtrolScope(String nameStr) {
		this.nameStr = nameStr;
	}

	public String getNameStr() {
		return this.nameStr;
	}

}
