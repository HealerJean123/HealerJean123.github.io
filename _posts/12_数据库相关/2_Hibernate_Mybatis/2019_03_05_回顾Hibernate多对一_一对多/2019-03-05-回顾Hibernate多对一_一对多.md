---
title: 回顾Hibernate多对一_一对多
date: 2019-02-20 03:33:00
tags: 
- Database
category: 
- Database
description: 回顾Hibernate多对一_一对多
---
**前言**     

 Github：[https://github.com/HealerJean](https://github.com/HealerJean)         

 博客：[http://blog.healerjean.com](http://HealerJean.github.io)            




主要针对两个表  ，让hibernate自动生成表  

`employee多对-department`    

`department一对多 employee`

## 1、JoinColumn 外键确，和多对一，一对多注解

### 1.1、构建实体`Department`


```java
/**
 * 作者 ：HealerJean
 * 日期 ：2019/3/4  上午11:18.
 * 类描述：
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "department")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Department {

    @Id
    @GeneratedValue(generator="_native")
    @GenericGenerator(name="_native", strategy="native")
    private Integer id; //ID

    private String dname;

    @OneToMany //department 一对多
    private List<Employee> employees = new ArrayList<>();;


}

```

### 1.2、构建实体


```java
@Data
@Accessors(chain = true)
@Entity
@Table(name = "employee")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Employee {

    @Id
    @GeneratedValue(generator="_native")
    @GenericGenerator(name="_native", strategy="native")
    private Integer id ;

    @Column(length=20)
    private String ename; //员工姓名

    @Column(length=20)
    private String phone; //电话

    @ManyToOne  //多对一
    private Department department; //所属部门

}

```

### 1.3  启动项目，自动生成数据表 

<font  clalss="healerColor" color="red" size="5" >     
employee表会自动添加一个外键列department_id，虽然关系映射上是正确了，但是有一个问题，数据库里多了一张表出来，这不是想要的结果。
</font>


![15516759211129](https://raw.githubusercontent.com/HealerJean/HealerJean.github.io/master/blogImages/15516759211129.png)



```java

show tables  ;
show create table  employee ;

CREATE TABLE `department` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dname` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) 


CREATE TABLE `department_employee` (
  `department_id` int(11) NOT NULL,
  `employeeList_id` int(11) NOT NULL,
  UNIQUE KEY `UK_gh6a42m015y9ynyn9y0pfkiyi` (`employeeList_id`),
  KEY `FKs2ai09fn82cogwkj70fmeo34f` (`department_id`),
  CONSTRAINT `FK22lvvnoyhwffto4x817swe761` FOREIGN KEY (`employeeList_id`) REFERENCES `employee` (`id`),
  CONSTRAINT `FKs2ai09fn82cogwkj70fmeo34f` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`)
) 



CREATE TABLE `employee` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ename` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL,
  `department_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbejtwvg9bxus2mffsm3swj3u9` (`department_id`),
  CONSTRAINT `FKbejtwvg9bxus2mffsm3swj3u9` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`)
) 

```

### 1.4、避免生成一张表，解决方法：在employeeList和department字段上加上@JoinColumn注解

@JoinColumn来指定外键列，避免生成一张中间表。


#### 1.4.1、在`Department`和`Employee`都加上`@JoinColumn(name="departmentId")`(建议使用（其实是强制的，否则会出现其他不该出现的问题）)

##### 1.4.1.1、Department


```java

    @OneToMany //department 一对多
    @JoinColumn(name="departmentId")
    private List<Employee> employeeList = new ArrayList<>();;


```
##### 1.4.1.2、Employee


```java

    @ManyToOne  //多对一
    @JoinColumn(name="departmentId")
    private Department department; //所属部门
    
```
##### 1.4.1.3、最终生成的数据表(正常)

`department`

<table>
<tr><th>Field</th><th>Type</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>
<tr><td>id</td><td>int(11)</td><td>NO</td><td>PRI</td><td>NULL</td><td>auto_increment</td></tr>
<tr><td>dname</td><td>varchar(255)</td><td>YES</td><td></td><td>NULL</td><td></td></tr></table>


```sql
CREATE TABLE `department` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dname` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) 
```

`employee`

<table>
<tr><th>Field</th><th>Type</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>
<tr><td>id</td><td>int(11)</td><td>NO</td><td>PRI</td><td>NULL</td><td>auto_increment</td></tr>
<tr><td>ename</td><td>varchar(20)</td><td>YES</td><td></td><td>NULL</td><td></td></tr>
<tr><td>phone</td><td>varchar(20)</td><td>YES</td><td></td><td>NULL</td><td></td></tr>
<tr><td>departmentId</td><td>int(11)</td><td>YES</td><td>MUL</td><td>NULL</td><td></td></tr></table>

```sql


CREATE TABLE `employee` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ename` varchar(20) DEFAULT NULL,
  `phone` varchar(20)  DEFAULT NULL,
  `departmentId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmf6ngbqyackl0thdr3pmf4ypv` (`departmentId`),
  CONSTRAINT `FKmf6ngbqyackl0thdr3pmf4ypv` FOREIGN KEY (`departmentId`) REFERENCES `department` (`id`)
) 
```


#### 4.1.2、如果deparment中不加` @JoinColumn(name="departmentId")`，但是Employee中加` @JoinColumn(name="departmentId")`

结果还是最开始的生成3张表，这不是想要的结果

![WX20190304-132318@2x](https://raw.githubusercontent.com/HealerJean/HealerJean.github.io/master/blogImages/WX20190304-132318@2x.png)

#### 4.1.3、如果Employee中不加` @JoinColumn(name="departmentId")`，但是deparment中加` @JoinColumn(name="departmentId")`

结果是两张表，如果JoinColumn指定了外键名字`departmentId`，hibernate帮我们创建的`department_id`和我自己的`departmentId`      



<table>
<tr><th>Field</th><th>Type</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>
<tr><td>id</td><td>int(11)</td><td>NO</td><td>PRI</td><td>NULL</td><td>auto_increment</td></tr>
<tr><td>dname</td><td>varchar(255)</td><td>YES</td><td></td><td>NULL</td><td></td></tr></table>


```sql
CREATE TABLE `department` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `dname` varchar(255)  DEFAULT NULL,
  PRIMARY KEY (`id`)
) 


```


<table>
<tr><th>Field</th><th>Type</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>
<tr><td>id</td><td>int(11)</td><td>NO</td><td>PRI</td><td>NULL</td><td>auto_increment</td></tr>
<tr><td>ename</td><td>varchar(20)</td><td>YES</td><td></td><td>NULL</td><td></td></tr>
<tr><td>phone</td><td>varchar(20)</td><td>YES</td><td></td><td>NULL</td><td></td></tr>
<tr><td>department_id</td><td>int(11)</td><td>YES</td><td>MUL</td><td>NULL</td><td></td></tr>
<tr><td>departmentId</td><td>int(11)</td><td>YES</td><td>MUL</td><td>NULL</td><td></td></tr></table>



```sql

CREATE TABLE `employee` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ename` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL,
  `department_id` int(11) DEFAULT NULL,
  `departmentId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbejtwvg9bxus2mffsm3swj3u9` (`department_id`),
  KEY `FKmf6ngbqyackl0thdr3pmf4ypv` (`departmentId`),
  CONSTRAINT `FKbejtwvg9bxus2mffsm3swj3u9` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`),
  CONSTRAINT `FKmf6ngbqyackl0thdr3pmf4ypv` FOREIGN KEY (`departmentId`) REFERENCES `department` (`id`)
) 
```

##### 4.3.1.1、所以如果使用这种方式的，那么名字就要和hibernate的`department_id`统一才可以



```
Department

  @OneToMany //department 一对多
    @JoinColumn(name="department_id")
    private List<Employee> employeeList = new ArrayList<>();;
```

## 1.5、使用2中的mapperBy避免生成三张表


### 1.5.1、Department

```java
@Data
@Accessors(chain = true)
@Entity
@Table(name = "department")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Department {

    @Id
    @GeneratedValue(generator="_native")
    @GenericGenerator(name="_native", strategy="native")
    private Integer id; //ID

    private String dname;


    @OneToMany(mappedBy = "department") //department 一对多
//  @JoinColumn(name="departmentId") //有了上面的这个就不能使用了，会冲突
    private List<Employee> employeeList = new ArrayList<>();;


}

```

### 1.5.2、Employee


```java
@Data
@Accessors(chain = true)
@Entity
@Table(name = "employee")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Employee {

    @Id
    @GeneratedValue(generator="_native")
    @GenericGenerator(name="_native", strategy="native")
    private Integer id ;

    @Column(length=20)
    private String ename; //员工姓名

    @Column(length=20)
    private String phone; //电话

    @ManyToOne  //多对一
    @JoinColumn(name="departmentId")
    private Department department; //所属部门


}
```


## 2、mappedBy

本类放弃控制关联关系 ，交给其他方  


<font  clalss="healerColor" color="red" size="5" >  

mappedBy表示声明当前类不是一对多的关系维护端，由对方来维护，是在一的一方进行声明的。mappedBy的值应该为一的一方的表名。



1、只有OneToOne,OneToMany,ManyToMany上才有mappedBy属性，ManyToOne不存在该属性；

2、mappedBy表示，本类防止控制和另一方的关联关系，所填name为本类在另一方的字段名。

3、mappedBy和@JoinTable/JoinColumn是互斥的，也就是说，@"关联关系注解"里面写了mappedBy属性，下面就不能再写@JoinTable。否则，Hibernate报异常。


</font>





### 使用：

一的一方配置上mappedBy属性，将维护权交给多的一方来维护，就不会有update语句了。    

至于为何要将维护权交给多的一方，可以这样考虑：要想一个国家的领导人记住所有人民的名字是不可能的，但可以让所有人民记住领导人的名字！



#### 2.1、Department

```java
package com.hlj.entity.db.demo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 作者 ：HealerJean
 * 日期 ：2019/3/4  上午11:18.
 * 类描述：
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "department")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Department {

    @Id
    @GeneratedValue(generator="_native")
    @GenericGenerator(name="_native", strategy="native")
    private Integer id; //ID

    private String dname;


    @OneToMany(mappedBy = "department") //department 一对多
//    @JoinColumn(name="departmentId") //有了上面的这个就不能使用了，会冲突
    private List<Employee> employeeList = new ArrayList<>();;

}

```

#### 2.2、Employee

```java
package com.hlj.entity.db.demo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * 作者 ：HealerJean
 * 日期 ：2019/3/4  上午11:21.
 * 类描述：
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "employee")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Employee {

    @Id
    @GeneratedValue(generator="_native")
    @GenericGenerator(name="_native", strategy="native")
    private Integer id ;

    @Column(length=20)
    private String ename; //员工姓名

    @Column(length=20)
    private String phone; //电话

    @ManyToOne  //多对一
    @JoinColumn(name="departmentId")
    private Department department; //所属部门


}

```

## 2.3、使用


```java
package com.hlj.moudle.mappedBy.Service.impl;

import com.hlj.dao.db.DepartmentRepository;
import com.hlj.dao.db.EmployeeRepository;
import com.hlj.entity.db.demo.Department;
import com.hlj.entity.db.demo.Employee;
import com.hlj.moudle.mappedBy.Service.MappedByService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 作者 ：HealerJean
 * 日期 ：2019/3/4  下午2:07.
 * 类描述：
 */
@Service
public class MappedByServiceImpl implements MappedByService {


    @Resource
    private DepartmentRepository departmentRepository ;

    @Resource
    private EmployeeRepository employeeRepository ;

    @Override
    public void noMappedBy() {


        //创建两个员工对象
        Employee employee1 = new Employee();
        employee1.setEname("张三");
        employee1.setPhone("13111111111");
        Employee employee2 = new Employee();
        employee2.setEname("李四");
        employee2.setPhone("18523222222");


        //创建一个部门对象
        Department department = new Department();
        department.setDname("研发部");

        //设置对象关联
        department.getEmployeeList().add(employee1);
        department.getEmployeeList().add(employee2);

        employee1.setDepartment(department);
        employee2.setDepartment(department);

        //这个必须放在下面 两个employee1 employee2 保存前面，网络上说的使用的session控制的（也可以保存成功，但是会有多出一些update更新语句问题,而且先保存多的一方，再保存），
        // 这里直接是jpa控制的，不会出现上面的问题，必须先保存一的一方，再保存多的一方。如果先保存多的一方会报错，很明显外键不存在怎么保存
        departmentRepository.save(department);

        employeeRepository.save(employee1);
        employeeRepository.save(employee2);


    }

}


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
		id: 'kAzTJoIMvujldm13',
    });
    gitalk.render('gitalk-container');
</script> 

<!-- Gitalk end -->

