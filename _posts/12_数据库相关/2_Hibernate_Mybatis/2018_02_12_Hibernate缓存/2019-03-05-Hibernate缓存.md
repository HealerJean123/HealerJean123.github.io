---
title: Hibernate缓存
date: 2019-03-05 03:33:00
tags: 
- Database
category: 
- Database
description: Hibernate缓存
---
**前言**     

 Github：[https://github.com/HealerJean](https://github.com/HealerJean)         

 博客：[http://blog.healerjean.com](http://HealerJean.github.io)            




## 1、hibernate get/load

### 1.1、get方法：

　　首先在session缓存中查找，然后在二级缓存中查找，还没有就查询数据库，数据库中没有就返回null


### 1.2、load方法：

根据是否懒加载分情况讨论


#### 1.2.1、若为true

1、先在Session缓存中查找，看看该id对应的对象是否存在       

2、不存在则使用延迟加载，返回实体的代理类对象(该代理类为实体类的子类，由CGLIB动态生成)。      

3、等到具体使用该对象(除获取OID以外)的时候，再查询二级缓存和数据库，若仍没发现符合条件的记录，则会抛出一个ObjectNotFoundException。

#### 1.2.2、若为false

和get方法查找顺序一样，首先在session缓存中查找，然后在二级缓存中查找，还没有就查询数据库，数据库中没有，则会抛出一个ObjectNotFoundException。


## 2、缓存分类

### 1.事务范围（单Session即一级缓存） 

   事务范围的缓存只能被当前事务访问,每个事务都有各自的缓存,缓存内的数据通常采用相互关联的对象形式.缓存的生命周期依赖于事务的生命周期,只有当事务结束时,缓存的生命周期才会结束.事务范围的缓存使用内存作为存储介质,一级缓存就属于事务范围. 

### 2.应用范围（单SessionFactory即二级缓存） 

   应用程序的缓存可以被应用范围内的所有事务共享访问.缓存的生命周期依赖于应用的生命周期,只有当应用结束时,缓存的生命周期才会结束.应用范围的缓存可以使用内存或硬盘作为存储介质,二级缓存就属于应用范围. 

### 3.集群范围（多SessionFactory） 

   在集群环境中,缓存被一个机器或多个机器的进程共享,缓存中的数据被复制到集群环境中的每个进程节点,进程间通过远程通信来保证缓存中的数据的一致,缓存中的数据通常采用对象的松散数据形式.比如redis

## 3、一级缓存(session的缓存,默认开启)


### 1.1、简单介绍

hibernate的一级缓存是session级别的，所以如果session关闭后，缓存就没了，此时就会再次发sql去查数据库。（`session关闭还能访问，解决该问题----配置二级缓存）`    

由于Session对象的生命周期通常对应一个数据库事务或者一个应用事务，因此它的缓存是事务范围的缓存,

### 1.2、测试：真实项目


#### 1.2.1、Jpa方法


```java
public interface DemoEntityRepository extends CrudRepository<DemoEntity,Long> {

   
    /**
     * 为了验证该sql不会放入缓存
     * @param id
     * @return
     */
    @Query(" FROM DemoEntity d where d.id = :id")
    DemoEntity testCachefindById(@Param("id") Long id) ;
}

```

#### 1.2.2、测试成功

```java

/**
     * hibernate 一级缓存
     * hibernate的一级缓存是session级别的，所以如果session关闭后，缓存就没了，此时就会再次发sql去查数据库。
     * 作用范围较小！ 缓存的事件短。
     * 缓存效果不明显
     *
     * @param id
     */
    public void oneCache(Long id){

        DemoEntity demoEntity = demoEntityRepository.findOne(id);
        System.out.println(demoEntity);
        //Hibernate: select demoentity0_.id as id1_0_0_, demoentity0_.age as age2_0_0_, demoentity0_.cdate as cdate3_0_0_, demoentity0_.name as name4_0_0_, demoentity0_.udate as udate5_0_0_ from demo_entity demoentity0_ where demoentity0_.id=?

        //用到了缓存,没有执行sql语句
        DemoEntity demoEntity12 = demoEntityRepository.findOne(id);
        System.out.println(demoEntity12);

        //因为是sql的形式，所以没有用到缓存
        DemoEntity sqlEntity13 = demoEntityRepository.testCachefindById(id);
        System.out.println(sqlEntity13);
        //Hibernate: select demoentity0_.id as id1_0_, demoentity0_.age as age2_0_, demoentity0_.cdate as cdate3_0_, demoentity0_.name as name4_0_, demoentity0_.udate as udate5_0_ from demo_entity demoentity0_ where demoentity0_.id=?
    }



Hibernate: select demoentity0_.id as id1_0_0_, demoentity0_.age as age2_0_0_, demoentity0_.cdate as cdate3_0_0_, demoentity0_.name as name4_0_0_, demoentity0_.udate as udate5_0_0_ from demo_entity demoentity0_ where demoentity0_.id=?
DemoEntity(id=1, name=healerjean, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 16:22:58.0)
DemoEntity(id=1, name=healerjean, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 16:22:58.0)

//下面这个是sql查询语句，不会放到缓存中去
Hibernate: select demoentity0_.id as id1_0_, demoentity0_.age as age2_0_, demoentity0_.cdate as cdate3_0_, demoentity0_.name as name4_0_, demoentity0_.udate as udate5_0_ from demo_entity demoentity0_ where demoentity0_.id=?
DemoEntity(id=1, name=healerjean, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 16:22:58.0)

```

### 1.3、N+1（Hibernate原生）

#### 1.3.1、list查询

如果通过list()方法来获得对象，，hibernate会发出一条sql语句，将所有的对象查询出来 ,查询出来的这些对象会根据主键Id放入session一集缓存中去（1条语句）     


```sql
控制台
Hibernate: select student0_.id as id2_, student0_.name as name2_, student0_.rid as rid2_, student0_.sex as sex2_ from t_student student0_ limit ?

```

```java

　/**
   * 此时会发出一条sql，将30个学生全部查询出来，并放到session的一级缓存当中（按照主键Id的key缓存放入）
   * 
  * 当再次查询某个Id的学生信息时，会首先去缓存中看是否存在，如果不存在，再去数据库中查询
   * 这就是hibernate的一级缓存(session缓存)
   */
   
  List<Student> ls = (List<Student>)session.createQuery("from Student")
                      .setFirstResult(0).setMaxResults(30).list();
                      
                  
  Iterator<Student> stus = ls.iterator();
  for(;stus.hasNext();)
  {
      Student stu = (Student)stus.next();
      System.out.println(stu.getName());
  }
  
  //从缓存中拿
 Student stu = (Student)session.load(Student.class, 1);

```

#### 1.3.2、iterator()获取对象


1、如果使用iterator方法返回列表，对于hibernate而言，它仅仅只是发出取id列表的sql （1条语句）    

2、在查询相应的具体的某个学生信息时，会发出相应的SQL去取学生信息（同事每个都放入session缓存中）,
这就是典型的N+1问题 （N条语句）   


```sql
ibernate: select student0_.id as col_0_0_ from t_student student0_ limit ?
Hibernate: select student0_.id as id2_0_, student0_.name as name2_0_, student0_.rid as rid2_0_, student0_.sex as sex2_0_ from t_student student0_ where student0_.id=?
沈凡
Hibernate: select student0_.id as id2_0_, student0_.name as name2_0_, student0_.rid as rid2_0_, student0_.sex as sex2_0_ from t_student student0_ where student0_.id=?
王志名
Hibernate: select student0_.id as id2_0_, student0_.name as name2_0_, student0_.rid as rid2_0_, student0_.sex as sex2_0_ from t_student student0_ where student0_.id=?
叶敦
.........
```




```java

/**
   * 如果使用iterator方法返回列表，对于hibernate而言，它仅仅只是发出取id列表的sql
   * 在查询相应的具体的某个学生信息时，会发出相应的SQL去取学生信息
   * 这就是典型的N+1问题
   * 
   * 存在iterator的原因是，有可能会在一个session中查询两次数据，如果使用list每一次都会把所有的对象查询上来（根据Id主键放入session缓存）， iterator仅仅只会查询id，这样关于id的所有的对象已经存储在一级缓存(session的缓存)中，可以直接获取
   */
  Iterator<Student> stus = (Iterator<Student>)session.createQuery("from Student")
                      .setFirstResult(0).setMaxResults(30).iterate();
                      

  for(;stus.hasNext();)
  {
      Student stu = (Student)stus.next();
      System.out.println(stu.getName());
  }


```

### 1.3、 为什么存在iterator    

list() 方法每次返回集合对象，而iterator()方法先返回对象id ,需要查询具体对象信息才会发出相应的sql 语句去查询 ，节省了内存花费；    

例如：再一个session中要两次查询多个对象时，如果两条都用list() ，一定会发出两条sql(list集合查询语句不使用缓存存放，因为是根据对象的主键Id存放的缓存，list会将集合取出来，再根据Id放到session一级缓存中)，而且两条语句一样     

但是如果你第一条使用list() 方法（查出了所有对象），第二条使用iterator() 方法（查询出对象 id） ，也是两条sql（在执行iterator的第二条sql的是，开始使用lsit查询出来的每个对象的缓存进行查询） 但是明显第二条只是根据id 对应第一条查出来的对象而已，可以节省内存；     

### 1.4、解决 N+1 ,使用二级缓存

### 1.5、哪些方法会放数据

#### 1.5.1、get()、load()均会向缓存中存放以及读取数据，

#### 1.5.2、query.list()和query.uniqueResult()会向缓存存放数据但不会从缓存中读取数据

#### 1.5.3、save()和saveOrUpdate()进行添加时，均会向缓存中存放数据。











## 4、二级缓存（sessionFactory的缓存):默认不会开启（需要进行配置）


### 4.1、简单介绍 

二级缓存的使用策略一般有这几种：read-only、nonstrict-read-write、read-write、transactional。    

注意：我们通常使用二级缓存都是将其配置成 read-only ，即我们应当在那些不需要进行修改的实体类上使用二级缓存，否则如果对缓存进行读写的话，性能会变差，这样设置缓存就失去了意义。



```

事务型（transactional）：隔离级别最高，对于经常被读但很少被改的数据，可以采用此策略。因为它可以防止脏读与不可重复读的并发问题。发生异常的时候，缓存也能够回滚（系统开销大）。

读写型（read-write）：对于经常被读但很少被改的数据，可以采用此策略。因为它可以防止脏读的并发问题。更新缓存的时候会锁定缓存中的数据。

非严格读写型（nonstrict-read-write）：不保证缓存与数据库中数据的一致性。对于极少被改，并且允许偶尔脏读的数据，可采用此策略。不锁定缓存中的数据。

只读型（read-only）：对于从来不会被修改的数据，可使用此策略。（只能放入一次）

```




#### 1、二级缓存是sessionFactory级别的缓存    

#### 2、二级缓存缓存的仅仅是对象，如果查询出来的是对象的一些属性（name,age），则不会被加到缓存中去


### 4.2、解决N+1问题


只是说明这个迭代器不会再N+1，其实执行的sql还是 list（n） +1 



```java
@Test
    public void testCache3()
    {
        Session session = null;
        try
        {
            session = HibernateUtil.openSession();
            /**
             * 将查询出来的Student对象缓存到二级缓存中去
             */
            List<Student> stus = (List<Student>) session.createQuery(
                    "select stu from Student stu").list();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            HibernateUtil.close(session);
        }
        try
        {
            /**
             * 由于学生的对象已经缓存在二级缓存中了，此时再使用iterate来获取对象的时候，首先会通过一条
             * 取id的语句，然后在获取对象时去二级缓存中，如果发现就不会再发SQL，这样也就解决了N+1问题 
             * 而且内存占用也不多             */
            session = HibernateUtil.openSession();
            Iterator<Student> iterator = session.createQuery("from Student")
                    .iterate();
            for (; iterator.hasNext();)
            {
                Student stu = (Student) iterator.next();
                System.out.println(stu.getName());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

```



### 4.3、二级缓存会缓存 hql 语句吗(除非配置查询缓存)


#### 4.3。1、连续使用两个list查询，测试的时候，这里讲session关闭了，再查的

```java

@Test
    public void testCache4()
    {
        Session session = null;
        try
        {
        
        //  list查询
            session = HibernateUtil.openSession();
            List<Student> ls = session.createQuery("from Student")
                    .setFirstResult(0).setMaxResults(50).list();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            HibernateUtil.close(session);
        }
        try
        {
            /**
             * 使用List会发出两条一模一样的sql，此时如果希望不发sql就需要使用查询缓存
             */
            session = HibernateUtil.openSession();
            List<Student> ls = session.createQuery("from Student")
                    .setFirstResult(0).setMaxResults(50).list();
            Iterator<Student> stu = ls.iterator();
            for(;stu.hasNext();)
            {
                Student student = stu.next();
                System.out.println(student.getName());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            HibernateUtil.close(session);
        }
    }
       
```


```sql
Hibernate: select student0_.id as id2_, student0_.name as name2_, student0_.sex as sex2_, student0_.rid as rid2_ from t_student student0_ limit ?
Hibernate: select student0_.id as id2_, student0_.name as name2_, student0_.sex as sex2_, student0_.rid as rid2_ from t_student student0_ limit ?

```

#### 4.3.2、配置查询缓存，解决上面的问题

当我们如果通过 list() 去查询两次对象时，二级缓存虽然会缓存查询出来的对象，但是我们看到发出了两条相同的查询语句，这是因为二级缓存不会缓存我们的hql查询语句，要想解决这个问题，我们就要配置我们的查询缓存了。


**只有当 HQL 查询语句完全相同时，连参数设置都要相同，此时查询缓存才有效，查询缓存缓存的也仅仅是对象的id**


```
spring.jpa.properties.hibernate.cache.use_query_cache=true

```


```java

需要缓存的类加上：
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region ="cacheSerializer.hibernate.twocache")//注解指定缓存策略，以及存放到哪个缓存区域。

```


### 4.4、使用方案

#### 4.4.1、适合二级缓存的数据存储:

1 很少被修改的数据    
2 不是很重要的数据，允许出现偶尔并发的数据      
3 不会被并发访问的数据

#### 4.4.2、不适合二级缓存的数据存储:

1 经常被修改的数据     
2 .绝对不允许出现并发访问的数据，如财务数据，绝对不允许出现并发     
3 与其他应用共享的数据


​    
## 5、二级缓存开发

### 5.1、ehcache.xml


```xml

<!--
  ~ Hibernate, Relational Persistence for Idiomatic Java
  ~
  ~ Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
  ~ indicated by the @author tags or express copyright attribution
  ~ statements applied by the authors. All third-party contributions are
  ~ distributed under license by Red Hat Middleware LLC.
  ~
  ~ This copyrighted material is made available to anyone wishing to use, modify,
  ~ copy, or redistribute it subject to the terms and conditions of the GNU
  ~ Lesser General Public License, as published by the Free Software Foundation.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  ~ or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
  ~ for more details.
  ~
  ~ You should have received a copy of the GNU Lesser General Public License
  ~ along with this distribution; if not, write to:
  ~ Free Software Foundation, Inc.
  ~ 51 Franklin Street, Fifth Floor
  ~ Boston, MA  02110-1301  USA
  -->
<ehcache xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:noNamespaceSchemaLocation="http://www.ehcache.org/ehcache.xsd"
         updateCheck="true" monitoring="autodetect"
         dynamicConfig="true">

    <!--diskStore元素是可选的，非必须的。如果不使用磁盘存储,只需要将diskStore注释掉即可；
    如果使用，需要在ehcache.xml文件中的ehcahce元素下的定义一个diskStore元素并指定其path属性。-->
    <diskStore path="/Users/healerjean/Desktop/twocache"/>

    <transactionManagerLookup class="net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup"
                              properties="jndiName=java:/TransactionManager" propertySeparator=";"/>
    <!--初始化缓存监听-->
    <cacheManagerEventListenerFactory class="com.hlj.moudle.cache.config.cachelistener.CustomerCacheManagerEventListenerFactory" properties=""/>


    <!-- Sets the path to the directory where cacheSerializer .data files are created.

         If the path is a Java System Property it is replaced by
         its value in the running VM.

         The following properties are translated:
         user.home - User's home directory
         user.dir - User's current working directory
         java.io.tmpdir - Default temp file path -->
    <!--<diskStore path="c:\dev\cacheSerializer"/>-->

    <!--
       name：Cache的唯一标识
       maxElementsInMemory：内存中最大缓存对象数
       maxElementsOnDisk：磁盘中最大缓存对象数，若是0表示无穷大
       eternal：Element是否永久有效，一但设置了，timeout将不起作用
       overflowToDisk：配置此属性，当内存中Element数量达到maxElementsInMemory时，Ehcache将会Element写到磁盘中
       timeToIdleSeconds：设置Element在失效前的允许闲置时间。仅当element不是永久有效时使用，可选属性，默认值是0，也就是可闲置时间无穷大
       timeToLiveSeconds：设置Element在失效前允许存活时间。最大时间介于创建时间和失效时间之间。仅当element不是永久有效时使用，默认是0.，也就是element存活时间无穷大
       diskPersistent：是否缓存虚拟机重启期数据
       diskExpiryThreadIntervalSeconds：磁盘失效线程运行时间间隔，默认是120秒
       diskSpoolBufferSizeMB：这个参数设置DiskStore（磁盘缓存）的缓存区大小。默认是30MB。每个Cache都应该有自己的一个缓冲区
       memoryStoreEvictionPolicy：当达到maxElementsInMemory限制时，Ehcache将会根据指定的策略去清理内存。默认策略是LRU（最近最少使用）。你可以设置为FIFO（先进先出）或是LFU（较少使用）
       -->

    <defaultCache
            maxElementsInMemory="10000"
            eternal="false"
            timeToIdleSeconds="600"
            timeToLiveSeconds="600"
            overflowToDisk="true"
            maxElementsOnDisk="10000000"
            diskPersistent="false"
            diskExpiryThreadIntervalSeconds="120"
            diskSpoolBufferSizeMB="64"
            memoryStoreEvictionPolicy="LRU"
    >
    </defaultCache>

    <cache name="cacheSerializer.hibernate.twocache"
           maxEntriesLocalHeap="5000"
           eternal="true"
           timeToIdleSeconds="600"
           timeToLiveSeconds="600">
        <cacheEventListenerFactory class="com.hlj.moudle.cache.config.cachelistener.CustomerCacheEventListenerFactory" />
    </cache>

</ehcache>

```

### 5.2、 spring配置文件


```java
spring.profiles.active=demo

#aop
spring.aop.auto=true
spring.aop.proxy-target-class=true

#spring data jpa properties
spring.jpa.database-platform=org.hibernate.dialect.MySQL5InnoDBDialect
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database=MYSQL
spring.jpa.hibernate.naming.implicit-strategy=org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.data.jpa.repositories.enabled=true


# 打开hibernate统计信息
spring.jpa.properties.hibernate.generate_statistics=true
# 打开二级缓存
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
# 打开查询缓存
spring.jpa.properties.hibernate.cache.use_query_cache=true
# 指定缓存provider
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory
# 配置shared-cache-mode
spring.jpa.properties.javax.persistence.sharedCache.mode=ENABLE_SELECTIVE

#log level
logging.level.root=info
logging.level.org.springframework=ERROR
logging.level.org.mybatis=ERROR
logging.level.org.mybatis.example.BlogMapper=debug



#logging.level.com.duodian.youhui.dao.mybatis.coupon=debug

#static
spring.resources.chain.strategy.content.enabled=true
spring.resources.chain.strategy.content.paths=/**
#
#
##出现错误时, 直接抛出异常 路径访问错误也抛出异常
#spring.mvc.throw-exception-if-no-handler-found=true
#spring.resources.add-mappings=false


```






```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:cache="http://www.springframework.org/schema/cache"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
 http://www.springframework.org/schema/cache http://www.springframework.org/schema/cache/spring-cache.xsd " >


    <description>spring configuration</description>




    <!-- 自定义Ehcache缓存，根据java类加入更多的key 支持注解缓存   -->
    <cache:annotation-driven/>

    <bean id="cacheManager" class="com.hlj.moudle.cache.config.CustomEhCacheManager">
        <property name="cacheManager" ref="ehcacheManager"/>
    </bean>

    <bean id="ehcacheManager" class="org.springframework.cache.ehcache.EhCacheManagerFactoryBean">
        <property name="configLocation" value="classpath:ehcache.xml"/>
        <property name="shared" value="true"/>
    </bean>


</beans>

```

### 5.3、配置缓存名称类(没用到)


```java
package com.hlj.moudle.cache.config;

/**
 * 类名称：CacheConstants
 * 类描述：缓存常量类
 * 创建人：HealerJean
 * 需要初始化的缓存定义名称需要以CACHE_为前缀。如：CACHE_XXX
 * 如果需要增加自定义过期时间，则增加过期时间变量定义EXPIRE_为前缀的缓存过期时间.如：EXPIRE_CACHE_XXX
 * 如不设置自定义过期时间即默认spring cache中设置过期时间
 *
 * @version 1.0.0
 */
public class CacheConstants {

    //公共缓存，

    public static final String CACHE_PUBLIC = "cacheSerializer.public";
    public static final Long EXPIRE_CACHE_PUBLIC = 60L;

    public static final String CACHE_PUBLIC_TEN_MINUTE = "cacheSerializer.public.ten.minute";
    public static final Long EXPIRE_CACHE_PUBLIC_TEN_MINUTE = 10 * 60L;

}



```

### 5.4、缓存管理器


```java
package com.hlj.moudle.cache.config;

import com.hlj.moudle.cache.config.cachelistener.CustomerCacheEventListenerFactory;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.ehcache.EhCacheCacheManager;

import java.lang.reflect.Field;
import java.util.*;

/**
 *  类描述：继承 EhCacheCacheManager
 * @Description 初始化缓存。加载@CacheConstants中缓存定义及缓存自定义过期时间
 * @Date   2018/3/21 下午2:46.
 */
public class CustomEhCacheManager extends EhCacheCacheManager {

    private static String CACHE_PREFIX = "CACHE_";
    private static String EXPIRE_PREFIX = "EXPIRE_";
    private static Logger logger = LoggerFactory.getLogger(CustomEhCacheManager.class);

    private static List<String> cacheNames = new ArrayList<>();
    private static Map<String,Long> expires = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        try {
            Class clazz = CacheConstants.class;
            Field[] fields = clazz.getDeclaredFields();
            for(Field field : fields){
                if (field.getName().startsWith(CACHE_PREFIX)){
                    cacheNames.add(field.get(clazz).toString());
                } else if (field.getName().startsWith(EXPIRE_PREFIX)){
                    expires.put(
                            clazz.getField(field.getName().replace(EXPIRE_PREFIX,"")).get(clazz).toString(),
                            Long.parseLong(field.get(clazz).toString())
                    );
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            throw new RuntimeException("init cacheSerializer failure!",e);
        }
        super.afterPropertiesSet();
    }

    @Override
    protected Collection<Cache> loadCaches() {
        LinkedHashSet<Cache> caches = new LinkedHashSet<Cache>(cacheNames.size());
        for(String name:cacheNames){
            Ehcache exist = this.getCacheManager().addCacheIfAbsent(name);
            if(exist != null){
                Cache cache = new EhCacheCache(exist);
                Ehcache ehcache = (Ehcache) cache.getNativeCache();
                CacheConfiguration configuration = ehcache.getCacheConfiguration();
                Long time = expires.get(name);
                configuration.setTimeToIdleSeconds(time);
                configuration.setTimeToLiveSeconds(time);
                //添加缓存添加时候的监听
                configuration.cacheEventListenerFactory(new CacheConfiguration.CacheEventListenerFactoryConfiguration().className(CustomerCacheEventListenerFactory.class.getName()));

                caches.add(cache);
            }
        }
        return caches;
    }
}


```

### 5.5、缓存操作类(没用到)


```java

package com.hlj.moudle.cache.config;

/**
 * @Desc:
 * @Author HealerJean
 * @Date 2018/8/17  上午11:34.
 */
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 类名称：EhCacheObjectData
 * 类描述：ehcache 对象数据存储
 *
 */
@Component
public class EhCacheObjectData {

    @Resource
    private CustomEhCacheManager customEhCacheManager;

    private CacheManager cacheManager;

    @PostConstruct
    public void init() {
        cacheManager = customEhCacheManager.getCacheManager();
    }

    public Object getData(String cacheName,String key){
        Cache cache = cacheManager.getCache(cacheName);
        if(cache == null ) return null;
        Element element = cache.get(key);
        if(element != null) return element.getObjectValue();
        return null;
    }

    public void setData(String cacheName,String key,Object data){
        Cache cache = cacheManager.getCache(cacheName);
        if(cache != null ) cache.put(new Element(key,data));
    }

    /**
     * 删除缓存下的key
     * @param cacheName
     * @param key
     * @return
     */
    public boolean delete(String cacheName,String key){
        Cache cache = cacheManager.getCache(cacheName);
        if(cache != null ) {
            return cache.remove(key);
        }
        return false;
    }
}

```



### 5.6、缓存管理器事件监听


```java
public class CustomerCacheManagerEventListenerFactory extends CacheManagerEventListenerFactory {
    @Override
    public CacheManagerEventListener createCacheManagerEventListener(CacheManager cacheManager, Properties properties) {
        return new CustomerCacheManagerEventListener(cacheManager);
    }
}
```

#### 5.6.1、监听实例


启动web的时候，进行添加缓存名称的时候执行，也就是说对应的缓存策略


```java

package com.hlj.moudle.cache.config.cachelistener;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 作者 ：HealerJean
 * 日期 ：2019/3/4  下午5:06.
 * 类描述：
 */
public class CustomerCacheManagerEventListener implements CacheManagerEventListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final CacheManager cacheManager;

    public CustomerCacheManagerEventListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void init() throws CacheException {
        logger.info("init ehcache...");
    }

    @Override
    public Status getStatus() {
        return null;
    }

    @Override
    public void dispose() throws CacheException {
        logger.info("ehcache dispose...");
    }

    @Override
    public void notifyCacheAdded(String s) {
        logger.info("cacheAdded... {}", s);
        logger.info(cacheManager.getCache(s).toString());
    }

    @Override
    public void notifyCacheRemoved(String s) {

    }
}
```

### 5.7、缓存添加监听


```java
public class CustomerCacheEventListenerFactory extends CacheEventListenerFactory {

    @Override
    public CacheEventListener createCacheEventListener(final Properties properties) {
        return new CustomerCacheEventListener();
    }
}

```

#### 5.7.1、监听实例

添加添加key，value缓存的时候执行


```java
package com.hlj.moudle.cache.config.cachelistener;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 作者 ：HealerJean
 * 日期 ：2019/3/4  下午5:07.
 * 类描述：
 */
public class CustomerCacheEventListener implements CacheEventListener {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException {
        logger.info("cache removed. key = {}, value = {}", element.getObjectKey(), element.getObjectValue());
    }

    /**
     * 放入缓存的时候调用该放阿飞
     * @param ehcache
     * @param element
     * cache put. key = com.hlj.entity.db.demo.DemoEntity#1, value = org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Item@e99a9dd
     * @throws CacheException
     */
    @Override
    public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException {
        logger.info("cache put. key = {}, value = {}", element.getObjectKey(), element.getObjectValue());
    }

    @Override
    public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException {
        logger.info("cache updated. key = {}, value = {}", element.getObjectKey(), element.getObjectValue());
    }

    @Override
    public void notifyElementExpired(Ehcache ehcache, Element element) {
        logger.info("cache expired. key = {}, value = {}", element.getObjectKey(), element.getObjectValue());
    }

    @Override
    public void notifyElementEvicted(Ehcache ehcache, Element element) {
        logger.info("cache evicted. key = {}, value = {}", element.getObjectKey(), element.getObjectValue());
    }

    @Override
    public void notifyRemoveAll(Ehcache ehcache) {
        logger.info("all elements removed. cache name = {}", ehcache.getName());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public void dispose() {
        logger.info("cache dispose.");
    }
}
```

### 5.8、缓存的实体bean(只读测试)


```java
@Cacheable //注解标记该entity开启 二级缓存，
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region ="cacheSerializer.hibernate.twocache")//注解指定缓存策略，以及存放到哪个缓存区域。
```


```java
package com.hlj.entity.db.demo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * 测试实体类;
 */
@Entity
@Table(name = "demo_entity")
@Data
@Accessors(chain = true)
@ApiModel(value = "demo实体类")
@Cacheable //注解标记该entity开启 二级缓存，
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region ="cacheSerializer.hibernate.twocache")//注解指定缓存策略，以及存放到哪个缓存区域。
public class DemoEntity{

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@ApiModelProperty(value = "demo 主键")
	private Long id;

	@ApiModelProperty(value = "姓名")
	private String name;

	@ApiModelProperty(value = "年龄")
	private Long age ;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP",insertable = true,updatable = false)
	private Date cdate;

	@UpdateTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private Date udate;


}

/**

 drop table demo_entity;

 CREATE TABLE `demo_entity` (
 `id` bigint(20) NOT NULL AUTO_INCREMENT,
 `name` varchar(255)   DEFAULT NULL,
 `age` bigint(20) DEFAULT '0',
 `cdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 `udate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (`id`)
 );
 */

```

### 5.9、@EnableCaching启动类开始支持缓存


```java

@SpringBootApplication
@EnableTransactionManagement
@EnableAsync
@ImportResource(value = "classpath:applicationContext.xml")
@EnableJpaRepositories(basePackages = {"com.hlj.dao.db"})
@EntityScan(basePackages = "com.hlj.entity.db")
@EnableCaching
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}

```

### 5.10、测试

#### 5.10.1、service



```java

    /**
     * hibernate 一级缓存
     * hibernate的一级缓存是session级别的，所以如果session关闭后，缓存就没了，此时就会再次发sql去查数据库。
     * 作用范围较小！ 缓存的事件短。
     * 缓存效果不明显
     *
     * @param id
     */
    public void oneCache(Long id){

        DemoEntity demoEntity = demoEntityRepository.findOne(id);
        System.out.println(demoEntity);
        //Hibernate: select demoentity0_.id as id1_0_0_, demoentity0_.age as age2_0_0_, demoentity0_.cdate as cdate3_0_0_, demoentity0_.name as name4_0_0_, demoentity0_.udate as udate5_0_0_ from demo_entity demoentity0_ where demoentity0_.id=?

        //用到了缓存,没有执行sql语句
        DemoEntity demoEntity12 = demoEntityRepository.findOne(id);
        System.out.println(demoEntity12);

        //因为是sql的形式，所以没有用到缓存
        DemoEntity sqlEntity13 = demoEntityRepository.testCachefindById(id);
        System.out.println(sqlEntity13);
        //Hibernate: select demoentity0_.id as id1_0_, demoentity0_.age as age2_0_, demoentity0_.cdate as cdate3_0_, demoentity0_.name as name4_0_, demoentity0_.udate as udate5_0_ from demo_entity demoentity0_ where demoentity0_.id=?
    }




    /**
     * 二级缓存
     * Hibernate提供了基于应用程序级别的缓存， 可以跨多个session，即不同的session都可以访问缓存数据。 这个换存也叫二级缓存。
     *
     * ibernate提供的二级缓存有默认的实现，且是一种可插配的缓存框架！
     */
    public void twoCache(Long id){

        DemoEntity demoEntity = demoEntityRepository.findOne(id);
        System.out.println(demoEntity);

        DemoEntity demoEntity12 = demoEntityRepository.findOne(id);
        System.out.println(demoEntity12);

        DemoEntity sqlEntity13 = demoEntityRepository.testCachefindById(id);
        System.out.println(sqlEntity13);
    }



```

#### 5.10.2、controller

```java

    @ApiOperation(value = "一级缓存",
                  notes = "一级缓存",
                  consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                  produces = MediaType.APPLICATION_JSON_VALUE,
                  response = DemoEntity.class)
    @GetMapping(value = "one_cache",produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseBean insert(Long  id){
        try {
            hibernateCacheService.oneCache(id) ;
            return  ResponseBean.buildSuccess();
        }catch (AppException e){
            ExceptionLogUtils.log(e,this.getClass() );
            return  ResponseBean.buildFailure(e.getCode(),e.getMessage());
        }catch (Exception e){
            ExceptionLogUtils.log(e,this.getClass() );
            return  ResponseBean.buildFailure(e.getMessage());
        }
    }



    @ApiOperation(value = "二级缓存",
            notes = "二级缓存",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            response = DemoEntity.class)
    @GetMapping(value = "two_cache",produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseBean two_cache(Long  id){
        try {
            hibernateCacheService.twoCache(id) ;
            return  ResponseBean.buildSuccess();
        }catch (AppException e){
            ExceptionLogUtils.log(e,this.getClass() );
            return  ResponseBean.buildFailure(e.getCode(),e.getMessage());
        }catch (Exception e){
            ExceptionLogUtils.log(e,this.getClass() );
            return  ResponseBean.buildFailure(e.getMessage());
        }
    }


```

#### 5.10.3、启动服务器

分别访问 [http://localhost:8080/hibernate/cache/one_cache?id=1
](http://localhost:8080/hibernate/cache/one_cache?id=1
)


```sql
Hibernate: select demoentity0_.id as id1_0_0_, demoentity0_.age as age2_0_0_, demoentity0_.cdate as cdate3_0_0_, demoentity0_.name as name4_0_0_, demoentity0_.udate as udate5_0_0_ from demo_entity demoentity0_ where demoentity0_.id=?
2019-03-04 20:09:12.476 [http-nio-8080-exec-1] INFO  c.h.m.c.c.c.CustomerCacheEventListener - cache put. key = com.hlj.entity.db.demo.DemoEntity#1, value = CacheEntry(com.hlj.entity.db.demo.DemoEntity)[25,2019-03-04 16:22:58.0,更新Mon Mar 04 19:52:35 CST 2019,2019-03-04 19:53:08.0]
DemoEntity(id=1, name=更新Mon Mar 04 19:52:35 CST 2019, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 19:53:08.0)


DemoEntity(id=1, name=更新Mon Mar 04 19:52:35 CST 2019, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 19:53:08.0)


Hibernate: select demoentity0_.id as id1_0_, demoentity0_.age as age2_0_, demoentity0_.cdate as cdate3_0_, demoentity0_.name as name4_0_, demoentity0_.udate as udate5_0_ from demo_entity demoentity0_ where demoentity0_.id=?
DemoEntity(id=1, name=更新Mon Mar 04 19:52:35 CST 2019, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 19:53:08.0)
```


访问[http://localhost:8080/hibernate/cache/two_cache?id=1
](http://localhost:8080/hibernate/cache/two_cache?id=1
) 


**说明使用的是二级缓存中的数据，因为没有使用sql语句**


```java

DemoEntity(id=1, name=更新Mon Mar 04 19:52:35 CST 2019, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 19:53:08.0)

DemoEntity(id=1, name=更新Mon Mar 04 19:52:35 CST 2019, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 19:53:08.0)

//Hql语句和上面的Hql配置查询缓存不冲突，这里虽然配置了查询缓存，但是却没有生效，因为这里的缓存是我自己写的，没有用原生的类方法进行查询
Hibernate: select demoentity0_.id as id1_0_, demoentity0_.age as age2_0_, demoentity0_.cdate as cdate3_0_, demoentity0_.name as name4_0_, demoentity0_.udate as udate5_0_ from demo_entity demoentity0_ where demoentity0_.id=?
DemoEntity(id=1, name=更新Mon Mar 04 19:52:35 CST 2019, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 19:53:08.0)

```



### 5.11、缓存修改



```java

   /**
     * 更新缓存
     * @param id
     */
    public void updateOneCache(Long id){

        DemoEntity demoEntity = demoEntityRepository.findOne(id);

        demoEntity.setName("更新"+new Date().toString());
        demoEntityRepository.save(demoEntity);

        //用到了缓存,没有执行sql语句
        DemoEntity demoEntity12 = demoEntityRepository.findOne(id);
        System.out.println(demoEntity12);

        System.out.println("---------");

    }

```

#### 5.10.1、只读修改(再访问了5.10的基础上)


```
CacheConcurrencyStrategy.READ_ONLY

```

访问[http://localhost:8080/hibernate/cache/updateOneCache?id=1
](http://localhost:8080/hibernate/cache/updateOneCache?id=1
)



```
---------
Hibernate: update demo_entity set age=?, name=?, udate=? where id=?

缓存添加监听里面执行了notifyElementRemoved ，最后事物报错，因为我们设置的是只读属性不可以修改的

2019-03-04 20:18:18.030 [http-nio-8080-exec-5] INFO  c.h.m.c.c.c.CustomerCacheEventListener - cache removed. key = com.hlj.entity.db.demo.DemoEntity#1, value = CacheEntry(com.hlj.entity.db.demo.DemoEntity)[25,2019-03-04 16:22:58.0,更新Mon Mar 04 19:52:35 CST 2019,2019-03-04 19:53:08.0]
2019-03-04 20:18:26.056 [http-nio-8080-exec-5] ERROR c.h.m.cache.HibernateCacheController - 报错的文件是：JpaTransactionManager.java报错方法是：doCommit报错的行是：526报错的信息是：Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction

```

#### 5.10.2、CacheConcurrencyStrategy.READ_WRITE 读写修改

方法不变，重新启动服务，直接访问

[http://localhost:8080/hibernate/cache/updateOneCache?id=1
](http://localhost:8080/hibernate/cache/updateOneCache?id=1
)



```
 public void updateOneCache(Long id){

        DemoEntity demoEntity = demoEntityRepository.findOne(id);
        //Hibernate: select demoentity0_.id as id1_0_0_, demoentity0_.age as age2_0_0_, demoentity0_.cdate as cdate3_0_0_, demoentity0_.name as name4_0_0_, demoentity0_.udate as udate5_0_0_ from demo_entity demoentity0_ where demoentity0_.id=?
    //放入一级和二级缓存（二级缓存打印日志） 
    //2019-03-04 20:21:34.330 [http-nio-8080-exec-1] INFO  c.h.m.c.c.c.CustomerCacheEventListener - cache put. key = com.hlj.entity.db.demo.DemoEntity#1, value = org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Item@74f6e49d

        
        demoEntity.setName("更新"+new Date().toString());
        demoEntityRepository.save(demoEntity);
        //此时还不执行，等方法执行完成之后才会执行，先放到session一级缓存中去

        //用到了缓存,没有执行上面的sql语句，但是我们这里可以取到正常的值（在session一级缓存中取到的数据）
        DemoEntity demoEntity12 = demoEntityRepository.findOne(id);
        System.out.println(demoEntity12);

        System.out.println("---------");

    }


//上面方法走完之后
更新二级缓存，先将缓存设置一个值锁住，value = Lock Source-UUID:5d3c1948-9eeb-46e9-8f66-93eeb72901de
2019-03-04 20:22:50.932 [http-nio-8080-exec-1] INFO  c.h.m.c.c.c.CustomerCacheEventListener - cache updated. key = com.hlj.entity.db.demo.DemoEntity#1, value = Lock Source-UUID:5d3c1948-9eeb-46e9-8f66-93eeb72901de Lock-ID:0

//执行上面缓存的save方法()
Hibernate: update demo_entity set age=?, name=?, udate=? where id=?

//更新二级缓存
//2019-03-04 20:22:57.780 [http-nio-8080-exec-1] INFO  c.h.m.c.c.c.CustomerCacheEventListener - cache updated. key = com.hlj.entity.db.demo.DemoEntity#1, value = org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Item@2d8a81



全部日志

Hibernate: select demoentity0_.id as id1_0_0_, demoentity0_.age as age2_0_0_, demoentity0_.cdate as cdate3_0_0_, demoentity0_.name as name4_0_0_, demoentity0_.udate as udate5_0_0_ from demo_entity demoentity0_ where demoentity0_.id=?
2019-03-04 20:21:34.330 [http-nio-8080-exec-1] INFO  c.h.m.c.c.c.CustomerCacheEventListener - cache put. key = com.hlj.entity.db.demo.DemoEntity#1, value = org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Item@74f6e49d
DemoEntity(id=1, name=更新Mon Mar 04 20:21:51 CST 2019, age=25, cdate=2019-03-04 16:22:58.0, udate=2019-03-04 19:53:08.0)
---------
2019-03-04 20:22:50.932 [http-nio-8080-exec-1] INFO  c.h.m.c.c.c.CustomerCacheEventListener - cache updated. key = com.hlj.entity.db.demo.DemoEntity#1, value = Lock Source-UUID:5d3c1948-9eeb-46e9-8f66-93eeb72901de Lock-ID:0
Hibernate: update demo_entity set age=?, name=?, udate=? where id=?
2019-03-04 20:22:57.780 [http-nio-8080-exec-1] INFO  c.h.m.c.c.c.CustomerCacheEventListener - cache updated. key = com.hlj.entity.db.demo.DemoEntity#1, value = org.hibernate.cache.ehcache.internal.strategy.AbstractReadWriteEhcacheAccessStrategy$Item@2d8a81


```



![ContactAuthor](https://raw.githubusercontent.com/HealerJean/HealerJean.github.io/master/assets/img/artical_bottom.jpg)



<!-- Gitalk 评论 start  -->

<link rel="stylesheet" href="https://unpkg.com/gitalk/dist/gitalk.css">
<script src="https://unpkg.com/gitalk@latest/dist/gitalk.min.js"></script> 
<div id="gitalk-container"></div>    
 <script type="text/javascript">
    var gitalk = new Gitalk({
		clientID: `1d164cd85549874d0e3a`,
		clientSecret: `527c3d223d1e6608953e835b547061037d140355`,
		repo: `HealerJean.github.io`,
		owner: 'HealerJean',
		admin: ['HealerJean'],
		id: 'KFpafCYNehG2nAsl',
    });
    gitalk.render('gitalk-container');
</script> 

<!-- Gitalk end -->

