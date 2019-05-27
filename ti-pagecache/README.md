ti-pagecache
===============
## 基于Ehcache(Standalone模式)为热点web请求页提供高速缓存服务

[Ehcache](http://www.ehcache.org/)是一个纯Java进程内缓存框架，该内存框架可以实现对页面或对象等数据的缓存——可以将数据缓存到内存和磁盘

页面缓存主要用Filter过滤器对客户端的http请求进行过滤，如果该请求存在于缓存中，那么页面将从缓存对象中获取gzip压缩后的数据（其响应速度是没有压缩缓存时速度的3-5倍），直接响应。

提供相对灵活的在线缓存规则调整接口，掌控自如，安全放心。



# Performance

ti-pagecache is a modern and full featured pagecache engine, but also has a very good performance:



# Getting Started
 ti-pagecache是基于ehcache-web开源项目改写的，为更好的理解与使用，您可以先了解下Ehcache http://www.ehcache.org/
 
 The [ti-pagecache blog](http://www.ehcache.org/documentation/2.7/modules/web-caching.html) is a good place for getting started too【原ehcache-web使用说明】.

## Maven 


```xml
  <dependency>
    <groupId>com.zaisan.ti</groupId>
    <artifactId>ti-pagecache</artifactId>
    <version>${pagecache-version}</version>
</dependency>
```
 

## Web.xml

```xml
	<filter>
		<filter-name>pageCacheFilter</filter-name>
		<filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
		<init-param>
			<param-name>contextAttribute</param-name>
			<param-value>org.springframework.web.servlet.FrameworkServlet.CONTEXT.dispatcherServlet</param-value>
		</init-param>
		<init-param>
			<param-name>targetFilterLifecycle</param-name>
			<param-value>true</param-value>
		</init-param>
		<init-param>
			<param-name>cacheName</param-name>
			<param-value>webPageCache</param-value>
		</init-param>
		<init-param>
			<param-name>blockingTimeoutMillis</param-name>
			<param-value>50</param-value>			
		</init-param>		
	</filter>
	<filter-mapping>
		<filter-name>pageCacheFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
```
正如前所说，页面缓存的方式是使用filter拦截请求，在这里定义filter

 * filter定义的位置，看各自业务，越前效果越好，如若需要身份验证，可置于比如springSecurityFilterChain之后
 
 * filter-name 指定filter名称，由于该filter依赖dispatcherServlet里面的context,因此这里使用了spring的filterProxy,而 filter-name标签上的名称，就是要代理的beanName,比如这里定义为 pageCacheFilter （要与后续filter-bean的id保持一致）
 * filter-class 固定值为 org.springframework.web.filter.DelegatingFilterProxy
 * contextAttribute 固定值为org.springframework.web.servlet.FrameworkServlet.CONTEXT.dispatcherServlet
 * targetFilterLifecycle 固定值为 true (延迟初始化时，由proxy负责初始化filter-bean)
 * cacheName 指定页面缓存在Ehcache中的缓存名称，比如这里定义为 webPageCache（要与后续定义的Ehcache中的缓存名称保持一致）
 * blockingTimeoutMillis 由于使用了Ehcache的BlockingCache,存在着读写锁的获取的超时问题，比如当前请求Url对应的缓存内容并没有提前预热，同一时间大量请求涌入，第一个请求拿到Cache的写锁后，继续向后面的源服务请求数据，此时大量的其他请求将无法获取到Cache的读锁并陷入长时间等待（如果第一个请求磨蹭了半天没有拿到数据），这时我们应该定义blockingTimeoutMillis，将无限期获取读锁更改为在一定时间内获取，若超时未得，将绕过缓存，透传到源服务。 建议根据全网站响应均值来设定
 * filter-mapping 如若明确知道页面缓存将作用于一类URL,可在url-pattern中设置，以减少性能损耗，pageCacheFilter内部将基于用户定义的缓存规则进一步对URL进行过滤并区别处理。


## Spring XML Configuration
ti-pagecache.jar中已提供默认的配置xml配置，只需引入即可，位置：spring/default-pageCache-context.xml
该文件提供了相关的默认配置，如若要覆盖，可自定义配置xml

如下为 default-pageCache-context.xml 内容，仅作为自定义配置的参考 （非必须配置项）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
	xmlns:redis="http://www.springframework.org/schema/redis" xmlns:p="http://www.springframework.org/schema/p"
	xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
	   http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context-3.0.xsd
       http://www.springframework.org/schema/redis
        http://www.springframework.org/schema/redis/spring-redis-1.0.xsd">
	<!--  页面缓存 filter 定义 -->
	<bean id="pageCacheFilter"
		class="com.zaisan.ti.web.pagecache.filter.SimpleCachingHeadersPageCachingFilter" >
		<property name="configLocation"
			value="classpath:spring/default-pagecache-ehcache-spring.xml" /> 
	</bean>


	<!--  默认的  缓存 规则 解析类，目前 只支持  请求url+请求参数  作为缓存key, 项目中如有其它复杂需求 可以自定义，但请保持 id="pageCacheRuleResolver" -->
	<bean id="pageCacheRuleResolver"
		class="com.zaisan.ti.web.pagecache.rulecenter.DefaultPageCacheRuleResolver" /> 

	<!-- 监听 页面缓存规则的变更，和接收 缓存主动失效的命令  -->
	<bean id="watchDog" class="com.zaisan.ti.web.pagecache.watch.RedisWatchDog" />

	<!-- 使用 redis 监听 外面管理系统的 相关变更命令（更新，删除，添加 缓存 规则 ，主动失效 和 reloadAll 消息）   -->
		<!-- 以下两种jedisProvider都可以选择，一种依托spring-data-redis,一种只使用jedis原生pool -->
	 <bean id="jedisProvider" class="com.zaisan.ti.notify.redis.JedisRawPoolProvider"></bean> 
	 	<!--<bean id="jedisProvider" class="com.zaisan.ti.notify.redis.JedisSpringConnectionFactoryProvider"></bean>-->
	<notify:listener-container namespace="ti-pagecache" jedis-provider="jedisProvider">
	    	<notify:listener ref="watchDog" method="handleDeleteRule" topic="deleteRule" />
	     	<notify:listener ref="watchDog" method="handleSaveOrUpdateRule" topic="saveOrUpdateRule" />
	     	<notify:listener ref="watchDog" method="handleExpireCache" topic="expireCache" />
	     	<notify:listener ref="watchDog" method="reloadAllRule" topic="reloadAll" />
	</notify:listener-container>
	
</beans>
```
关于JedisProvider选择问题：
redis连接的提供者,bean类型为com.zaisan.ti.notify.redis.JedisProvider，目前notify内置了两个默认实现：

*  JedisSpringConnectionFactoryProvoider: 依赖注入当前springContext中bean:org.springframework.data.redis.connection.jedis.JedisConnectionFactory, 因此你的项目中应引入 spring-data-redis.jar ,并定义 JedisConnectionFactory-bean
*  JedisRawPoolProvider:
依赖注入当前springContext中bean:redis.clients.util.Pool ,因此你的项目中应引入 jedis.jar ,并定义 pool-bean
*  项目中也可以自定义自己的JedisProvider,只需实现该接口

---
基于 default-pageCache-context.xml 的可扩展点 (扩展点之外的请保持一致) 有如下：

*  页面缓存 filter的名称更改，这里的名称需跟web.xml中定义的filter-name保持一致（无聊的人才改）
*  pageCacheEhcacheManager 的配置文件更改，这里的配置文件定义的Ehcache的基本属性，见后章
*  pageCacheRuleResolver bean的class替换，如若自定义rule的解析策略，请实现 interface:com.zaisan.ti.web.pagecache.rulecenter.PageCacheRuleResolver
*  jedisProvider 自定义，或者从内置的两个默认实现选择一个
*  namespace    pagecache借助notify来实现在线更新规则，而notify依赖redis来进行消息发布与订阅，namespace作为redis上的key前缀，请定义为项目唯一key (例： adidas-dev-pagecache)

如下为 default-pagecache-ehcache-spring.xml 内容，仅作为自定义配置的参考 （非必须配置项）
```xml
<ehcache xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:noNamespaceSchemaLocation="http://ehcache.org/ehcache.xsd">
	<!-- 每个web服务器 都有自己专属的 pageCache, 所以 该 磁盘路径 不可 配置为 网络挂载  -->
	<diskStore path="pagecache/ehcache"/> 
	<cache name="webPageCache" eternal="false" 
		maxElementsInMemory="10000" overflowToDisk="true" diskPersistent="false"
		 timeToLiveSeconds="300" memoryStoreEvictionPolicy="LRU" >
	</cache>
</ehcache> 
```

基于 default-pagecache-ehcache-spring.xml 的可扩展点 (扩展点之外的请保持一致) 有如下：

*  diskStore-path 缓存在磁盘的存储路径，考虑到动态页面静态化后的内容较大，内存存储吃紧，在Ehcache缓存策略中采用了热点页面内存存储+相对不活跃页面磁盘存储,如注释所说，磁盘路径 不可 配置为 网络挂载（网络挂载相关可咨询运维组） 
*  cache-name 这里的name 需要与之前定义的filter中的参数cacheName保持一致
*  eternal="false",timeToLiveSeconds="300"   这三个参数作为cache的默认参数（建议：最好不要依赖这里的配置，请在后续介绍的缓存规则配置中，为每个热点请求配置不同的参数），若用户没有为某个URL配置缓存的存活策略，系统将使用这三个参数作为存活策略。 若eternal="true",timeToLiveSeconds参数将被忽视。
* maxElementsInMemory="10000" overflowToDisk="true" memoryStoreEvictionPolicy="LRU" 这三个参数结合起来使用，表示整个PageCache缓存中最多可以常驻内存的元素个数为10000个（这里的元素指的是具体每个url+paramKey所对应的请求的响应结果），如果超过，系统将按照LRU策略（近期最少使用）将部分元素挪移到磁盘存储，挪移出去的元素仍然可以正常访问，对使用者是完全透明的。配置建议：请根据实际部署环境和缓存页面大小判断 内存常驻个数，若内存够大 也可扩大 max的数值，同时可关闭 overflowToDisk(关闭后，被LRU请出去的元素将被删除，下次请求时将重新请求源服务生成内容，并再次进驻内存)
* 内存存储与文件存储概述1： element在PUT之前检查 内存个数是否已达上限，若已到，从中随机抽取30个元素按照LRU比较策略择选出其中的一位挪移到磁盘中（异步）
* 内存存储与文件存储概述2：element在GET时，若存储位置在磁盘中，则强行按照LRU原则挤掉一位到磁盘去，并将当前element存储在内存中，并异步任务将原磁盘中的位置释放 (一个萝卜一个坑)
* 过期检查概述: element的过期时间到了后，并不会自动失效直到第一个GET操作被调用，GET操作返回值为NULL,并清除内存中的element




## 提供 PageCacheRuleLoader 实现
如前介绍，页面缓存可基于用户配置为每个具体的请求url提供不同的缓存策略，系统初始化时将使用容器中用户已定义的 缓存规则加载器 来一次性加载并初始化缓存规则配置，当然，没有配置缓存策略的URL请求将直接绕过pageCache,穿透到源服务中去。
因此使用时，按照约定您应该在自己的项目中定义bean-id="pageCacheRuleLoader",并且至少实现了interface=com.zaisan.ti.web.pagecache.rulecenter.PageCacheRuleLoader 的spring-bean.

Example:
```java

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.stereotype.Service;

import com.zaisan.ti.web.pagecache.rulecenter.PageCacheRule;
import com.zaisan.ti.web.pagecache.rulecenter.PageCacheRuleLoader;


@Service("pageCacheRuleLoader")
public class LocalPageCacheRuleLoader implements PageCacheRuleLoader{
    @Override
    public List<PageCacheRule> loadAllPageCacheRule(){
        List<PageCacheRule> result = new ArrayList<PageCacheRule>();
        result.addAll(doLodPartOneRuleFromLockFile());
        result.addAll(doLoadPartTwoRuleFromRemoteService());
        return result;
    }
    
    private List<PageCacheRule>  doLodPartOneRuleFromLockFile(){
    	List<PageCacheRule> result = new ArrayList<PageCacheRule>();
    	//.............假装已经loadFromFile
    	PageCacheRule rule = new PageCacheRule();
    	rule.setMethod("GET"); // 请慎重 使用 POST(TODO 定义常量 or 定义setGet()) 【必填】
    	rule.setUrl("/brand/brand-detail.htm");  //【必填】
    	
    	//设置存活时间，like 10s 【必填】
    	rule.setTimeToLiveSeconds(10);
    	
    	//设置缓存key的后半区取值字段（可设置多个）(缓存key样例： GET/brand/brand-detail.htm&brandId=1)
    	//若不指定 后半区的取值字段 ，系统默认将 httpRequest.getQueryString()所得值作为后半区的值
    	rule.addParamKey("brandId"); //【墙裂建议明确指定取值字段】
    	
    	List<Object> inList = new ArrayList<Object>();inList.add(12);inList.add(13);
    	//同时可以限制 请求中的参数名为 brandId的参数值 必须在指定范围内，若不是 请求将绕过 pageCache
    	rule.addParamValueIn("brandId", inList);
    	
    	//List<Object> notInList = new ArrayList<Object>();notInList.add(11);notInList.add(10);
    	//同时可以限制 请求中的参数名为 brandId的参数值 不能在指定范围内，若在指定范围中  请求将绕过 pageCache
    	//很显然，理所当然，众所周知， In, notIn 是不可用同时使用的
    	//rule.addParamValueNotIn("brandId", notInList);
    	
    	
    	//可以不用显示set,已默认为enable,当然也可设置为 PageCacheRule.STATUS_DISABLED
    	// disable后，该类url的请求将绕过pageCache
    	rule.setStatus(PageCacheRule.STATUS_ENABLED); 
    	

    	
    	//强制 缓存规则在 指定时间后(比如1小时后)才会生效，在指定时间前，该类url的请求将绕过pageCache
    	//可以不设置，表示 没有限制，系统初始化后立即生效
    	rule.setStartDate(DateUtils.addHours(new Date(), 1));
    	
    	//强制 缓存规则 在 指定时间后(比如2小时后)立即失效，在指定时间后，该类url的请求将绕过pageCache
    	//可以不设置，表示 没有限制，缓存规则一旦生效后，将永久有效
    	rule.setEndDate(DateUtils.addHours(new Date(), 2));
    	result.add(rule);
    	return result;
    }
    
    private List<PageCacheRule> doLoadPartTwoRuleFromRemoteService(){
    	List<PageCacheRule> result = new ArrayList<PageCacheRule>();
    	//.............假装已经loadFromRemoteService
    	PageCacheRule rule = new PageCacheRule();
    	rule.setMethod("GET"); //【必填】
    	rule.setUrl("/product/*/detail.htm"); // 支持 ant风格路径(中间一般是productId)【必填】
    	
    	//由于商品详情的 查询参数 已经体现在 URL中了，
    	//因此这里可以不用 addParamKey,但必须保证浏览器不会生成额外的查询参数
    	
    	//设置存活时间，like 10s
    	rule.setTimeToLiveSeconds(8);//【必填】
    	
    	//比如活动将在 10分钟后进行，这里提前5分钟预热下
    	rule.setStartDate(DateUtils.addMinutes(new Date(), 5));
    	
    	//cacheCtrol header 的设置   详见方法注释
    	rule.setCacheCtrolScope(CacheCtrolScope.privatex);
    	rule.setCacheCtrolSecondCheckType(CacheCtrolSecondCheckType.mustRevalidate);
    	rule.setMaxAgeOfCacheCtrol(8);
    	rule.setSmaxAgeOfCacheCtrol(8);
    	result.add(rule);
    	return result;
    }
``` 

缓存规则使用逻辑细节如下：

* filter拦截到请求后，取出requestMehtod+requestURL 去规则列表匹配 已配置的缓存策略，若无，请求将绕过pageCache
* 依次检查 规则状态，规则开始时间，规则结束时间，请求参数值 In域检查，NotIn域检查，若被拒绝，请求将绕过pageCache
* 根据规则配置，取出请求中指定的参数值，并与url进行字符串拼装，拼装结果作为本次请求的 cacheKey
* 手握cacheKey，遍查Ehcache,若得缓存值，即刻驾鹤去。若无意中值，曲径通源服。


## 自定义 PageCacheRuleResolver
如果对上述提供的缓存规则解析有不同的需求和定制，可自定义缓存规则解析器，见 **基于 default-pageCache-context.xml 的可扩展点** 中的第三条
其中唯一的接口方法如下：
CacheStrategyCarrier parse(PageCacheRule pageCacheRule,HttpServletRequest httpRequest,String curRequestMethodAndURL);

* 其中入参 pageCacheRule 为系统根据curRequestMethodAndURL匹配出的配置规则，httpRequest 为本次请求的上下文，curRequestMethodAndURL 为本次请求的类型和路径（也可从httpRequest中获取）
* 出参 CacheStrategyCarrier 属性一览如下：
    * mainKeySequeueChar cacheKey的前半区内容 （固定为 curRequestMethodAndURL）
    * subKeySequeueChar  cacheKey的后半区内容 （一般由 请求中的查询参数值和自定义的数值结合而成）
    * timeToLiveSeconds  若本次请求需要生成缓存时，设置缓存的存活时长


## 掌握 通过notify组件在线实时编辑缓存规则和缓存失效操作
若你的管理项目已经依赖pagecache,则不需额外指定对notify组件的依赖，否则需要加以下依赖：
```xml
  <dependency>
    <groupId>com.zaisan.ti</groupId>
    <artifactId>ti-notify</artifactId>
    <version>${notify-version}</version>
</dependency>

    <!-- 以下两个jar包依赖取决于jedisProvider的提供方式（见下文） -->
<dependency>
	<groupId>org.springframework.data</groupId>
	<artifactId>spring-data-redis</artifactId>
    <version>${spring-data-redis-version}</version>
</dependency>

<dependency>
	<groupId>redis.clients</groupId>
	<artifactId>jedis</artifactId>
	<version>${jedis-version}</version>
</dependency>
```

springContext中添加以下bean定义
```xml
<!-- 以下两种jedisProvider都可以选择，一种依托spring-data-redis,一种只使用jedis原生pool -->
	<bean id="yourJedisProvider" class="com.zaisan.ti.notify.redis.JedisSpringConnectionFactoryProvider"></bean>
	<!-- <bean id="yourJedisProvider" class="com.zaisan.ti.notify.redis.JedisRawPoolProvider"></bean> -->
	<bean id="redisNotifyOperator" class="com.zaisan.ti.notify.redis.RedisNotifyOperatorImpl">
		<property name="nameSpace" value="ti-zaisan-dev" />
		<property name="jedisProvider"><ref bean="yourJedisProvider" /></property>
	</bean>
```

你可以将自己的操作发布到redis队列上，ti-pagecache内部有线程监听，并实时处理你的操作请求
代码示例：notifyOperator.noticeCluster("topic名称","规则Key");
* 规则删除 将要删除的规则key（格式为Mehtod+URL）发布到redis中去，topic=WatchDog.TOPIC_DELETERULE 规则删除后，之后该类URL的请求将绕过pageCache,同时 缓存中该规则关联的所有element将都被清除
* 规则修改 将需要修改的规则（完整的规则内容），发布到redis中去，topic=WatchDog.TOPIC_SAVEORUPDATERULE ,发布之后，ti-pagecache将会删除原规则关联的所有element,并立即启用用户新指定的rule，原rule将被删除，特别注意的是，新指定的rule必须属性齐全，包含所有该有设置 （建议基于原rule拷贝后再编辑）
* 规则添加 将要添加的规则（完整的规则内容），发布到redis中去,topic=WatchDog.TOPIC_SAVEORUPDATERULE,发布之后，ti-pagecache将立即启用用户新添加的rule 
* 主动失效 将需要立即失效的 "类cacheKey" 字符串数组 发布到redis中去，topic=WatchDog.TOPIC_EXPIRECACHE,发布之后，ti-pagecache 将遍历目前缓存中已存在的key,查找 满足以下条件之一的key,并删除该element:
    * 缓存中的key startWith "类cacheKey"，如 "类cacheKey" = GET/brand/detail.html ,缓存中的key = GET/brand/detail.html&brandId=13  和 GET/brand/detail.html&brandId=12  均被删除
    * 缓存中的key的主区域（method+url） ，与 "类cacheKey" 能以Ant语法匹配 ,如 "类cacheKey" = GET/product/*/detail.html, 缓存中的key = GET/product/13/detail.html  和 GET/product/12/detail.html  均被删除

## FAQ

## Want to contribute?
* Fork the project on https://github.com/zaisan3/ti.git.
* Wondering what to work on? See task/bug list and pick up something you would like to work on.
* Create an issue or fix one from [issues list](https://github.com/zaisan3/ti/issues).
* Write a blog post about how you use or extend handlebars.java.
* Please suggest changes to javadoc/exception messages when you find something unclear.
* If you have problems with documentation, find it non intuitive or hard to follow - let us know about it, we'll try to make it better according to your suggestions. Any constructive critique is greatly appreciated. Don't forget that this is an open source project developed and documented in spare time.


## Related knowledge 
 * [Ehcache](http://www.ehcache.org/)
 * [web-cache](http://www.ehcache.org/documentation/2.7/modules/web-caching.html)
 * [cache-control](https://zhuanlan.zhihu.com/p/25512679)

## Author
 [zaisan] jiajia.sang@google.cn