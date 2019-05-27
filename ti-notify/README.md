ti-notify
===============
## 一个基于redis的关心通知处理结果的通知组件


[ti-notify](http://git.zaisan.cn/ti/ti-notify/)依托redis（大部分系统的标配组件），实现了消息发布，消息消费，和消息消费结果的收集与反馈等功能，发布者可选择的查看消费反馈， 体系简单， 接口易用，侵入性较低。




# Performance

ti-notify is a modern and full featured notify engine, but also has a very good performance:

![pagecache Comparison](http://git.zaisan.cn/ti/ti-notify/images/bench.png)

Benchmark source code is available at: http://git.zaisan.cn/ti/ti-notify

# Getting Started
 ti-notify是基于spring-data-redis开源项目改写的，为更好的理解与使用，您可以先了解下http://projects.spring.io/spring-data-redis/

## Maven 


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
 

## 通知监听端配置

*  pom.xml 添加maven依赖（配置如上）
*  添加以下xml方式的bean配置到spring启动上下文中
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
	xmlns:notify="http://www.zaisan.com/schema/notify"
	xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.zaisan.com/schema/notify http://www.zaisan.com/schema/notify/spring-notify.xsd">
        	<!-- 以下两种jedisProvider都可以选择，一种依托spring-data-redis,一种只使用jedis原生pool -->
	<bean id="yourJedisProvider" class="com.zaisan.ti.notify.redis.JedisSpringConnectionFactoryProvider"></bean>
	<!-- <bean id="yourJedisProvider" class="com.zaisan.ti.notify.redis.JedisRawPoolProvider"></bean> -->
   <notify:listener-container namespace="ti-nbp-dev" jedis-provider="yourJedisProvider" task-executor="yourTaskExecutorBeanNane">
        <notify:listener ref="testListener1" method="handleStoreConfigChange" topic="storeConfig" />
         <notify:listener ref="testListener2" method="handlePromotionConfigChange" topic="promotionConfig" />
    </notify:listener-container>
</beans>
```
notify:listener-container的配置说明：

 * namespace（非必填） 可能实际使用的系统与其他系统公用一个redis环境，这里配置命名空间保证redis上key的唯一性，默认值为ti-notify
 * jedis-provider（必填） redis连接的提供者,bean类型为com.zaisan.ti.notify.redis.JedisProvider，目前notify内置了两个默认实现：
    - [ ] JedisSpringConnectionFactoryProvoider: 依赖注入当前springContext中bean:org.springframework.data.redis.connection.jedis.JedisConnectionFactory, 因此你的项目中应引入 spring-data-redis.jar ,并定义 JedisConnectionFactory-bean
    - [ ] JedisRawPoolProvider:
依赖注入当前springContext中bean:redis.clients.util.Pool ,因此你的项目中应引入 jedis.jar ,并定义 pool-bean
    - [ ] 项目中也可以自定义自己的JedisProvider,只需实现该接口
 * task-executor(非必填) 当消费端接收到消息时，使用此线程池处理消息，没有指定时，系统将创建 SimpleAsyncTaskExecutor 作为默认线程池
 * notify:listener 一个topic的监听配置，ref,method 为处理消息的bean示例名称和处理方法，topic 为监听的主题名称。多个主题的监听，请配置多个notify:listener子元素。
 
请尽量保持消息处理的逻辑轻量和简单。


## 通知发送端配置
*  pom.xml 添加maven依赖（配置如上）
*  添加以下bean定义
```xml
<!-- 以下两种jedisProvider都可以选择，一种依托spring-data-redis,一种只使用jedis原生pool -->
	<bean id="yourJedisProvider" class="com.zaisan.ti.notify.redis.JedisSpringConnectionFactoryProvider"></bean>
	<!-- <bean id="yourJedisProvider" class="com.zaisan.ti.notify.redis.JedisRawPoolProvider"></bean> -->
	<bean id="redisNotifyOperator" class="com.zaisan.ti.notify.redis.RedisNotifyOperatorImpl">
		<property name="nameSpace" value="ti-nbp-dev" />
		<property name="jedisProvider"><ref bean="yourJedisProvider" /></property>
	</bean>
```

发送端代码示例：
Example:
```java

	@Autowired
	private NotifyOperator notifyOperator;
	
	Future<NoticeResult> resultFuture = notifyOperator.noticeCluster("storeConfig", "第一条测试消息");
		NoticeResult result =   resultFuture.get(); //同步等待结果（最多10s）
		LOG.warn(result.getResultString()); //这里有各个消费端消费情况的汇总信息
		if(result.isAllClientSuccess()){ //是否所有消费端均反馈成功
		    return true;
		}
	    //如果有个别消费端再指定时间内没有反馈（默认等待10s,也可以指定等待时长），或者反馈消费失败， 这里可以再重发几次
		for(int i=1;i<=3;i++){
		    resultFuture =notifyOperator.noticeCluster("storeConfig", "第一条测试消息");
		    result = resultFuture.get(5, TimeUnit.SECONDS); //同步等待 5s (超时返回)
		    LOG.warn(result.getResultString());
		    if(result.isAllClientSuccess()){ //是否所有消费端均反馈成功
		        return true;
		    }
		}
		//重试3次依然无法确保所有消费端成功消费，需要人工排查下网络或节点存活问题
		return false;
``` 
## FAQ

## Want to contribute?
* Fork the project on http://git.zaisan.cn.
* Wondering what to work on? See task/bug list and pick up something you would like to work on.
* Create an issue or fix one from [issues list](http://git.zaisan.cn/ti/ti-notify/issues).
* Write a blog post about how you use or extend ti-notify.
* Please suggest changes to javadoc/exception messages when you find something unclear.
* If you have problems with documentation, find it non intuitive or hard to follow - let us know about it, we'll try to make it better according to your suggestions. Any constructive critique is greatly appreciated. Don't forget that this is an open source project developed and documented in spare time.


## Related knowledge 
 * [spring-data-redis](http://projects.spring.io/spring-data-redis/)
 * [redis](http://redis.io/)
## Author
 [zaisan] jiajia.sang@google.cn