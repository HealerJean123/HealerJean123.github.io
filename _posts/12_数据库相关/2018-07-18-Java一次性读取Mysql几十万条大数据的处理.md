---
title: Jave一次性读取Mysql几十万条大数据的处理
date: 2018-07-18 03:33:00
tags: 
- Database
category: 
- Database
description: Jave一次性读取Mysql几十万条大数据的处理
---
**前言**     

 Github：[https://github.com/HealerJean](https://github.com/HealerJean)         

 博客：[http://blog.healerjean.com](http://HealerJean.github.io)           



不用说也知道，一次性读取出那么大的数据是疯了吗，虚拟机能承受的聊那么大的对象吗？，所以我们需要分批进行读取。


下面是使用fenduan 每1万条进行一次读取执行
              

## 1、传入总数和每多少进行分段 制作为map

```java
package com.duodian.youhui.admin.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.TreeMap;


@Service
@Slf4j
public class JavaHeapSpaceUtilsForCouponGood {

    /**
     * @Desc: 传入总数和每多少进行分段
     * @Date:  2018/8/28 下午1:36.
     */
    
    public static TreeMap<Long,Long> getStartIdAndEndId(Long count, Long fenduan){
        TreeMap<Long,Long> map = new TreeMap();
        Long num = count / fenduan;
        Long yushu = count % fenduan;

        for (int i = 1; i <= num; i++) {
            map.put((i - 1) * fenduan + 1, i * fenduan);
        }

        Long yushufinal = num * fenduan + yushu;
        map.put(num * fenduan + 1, yushufinal);

        return map ;
    }

}


```


## 2、开始执行


```java
package com.duodian.youhui.admin.moudle.xiaodang.impl;

import com.duodian.youhui.admin.moudle.coupon.service.CouponItemGoodService;
import com.duodian.youhui.admin.moudle.xiaodang.XiaoDangService;
import com.duodian.youhui.admin.moudle.xiaodang.XiaoDuoDataInsertService;
import com.duodian.youhui.admin.utils.JavaHeapSpaceUtilsForCouponGood;
import com.duodian.youhui.data.xiaodang.XiaodangInsertData;
import com.duodian.youhui.entity.db.coupon.CouponItemGood;
import com.duodian.youhui.entity.db.coupon.CouponItemGoodAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Description
 * @Author HealerJean
 * @Date 2018/4/19  下午1:58.
 */
@Service
@Slf4j
public class XiaoDangServiceImpl implements XiaoDangService {

    //多点数据源
    @Resource
    private CouponItemGoodService  couponItemGoodService ;

    //小当数据源
    @Resource
   private XiaoDuoDataInsertService xiaoDuoDataInsertService;

public void keyTransferToXiaoDang(Long taobaoUserInfoId) {

    //获取所有的相关的 id，默认是有顺序的，正常我们根据需要进行根据我们需求进行id排序
    List<Long> ids = couponItemGoodService.countAllCouponItemGood();
    TreeMap<Long,Long> map = JavaHeapSpaceUtilsForCouponGood.getStartIdAndEndId(Long.valueOf(ids.size()),10000L );


    for(Long key:map.keySet()){
        //在ids 进行划分。然后获取
        List<Long> idParams = ids.subList(key.intValue()-1, map.get(key).intValue());

        XiaodangInsertData xiaodangInsertData = couponItemGoodService.keyTransferToXiaoDuo(taobaoUserInfoId ,idParams);
        for(CouponItemGood couponItemGood : xiaodangInsertData.getCouponItemGoods()){
            try {
                List<CouponItemGoodAttachment> couponItemGoodAttachments = couponItemGoodService.couponItemGoodAttachments(couponItemGood.getId());
                xiaoDuoDataInsertService.insertCouponItemGood(couponItemGood,couponItemGoodAttachments, xiaodangInsertData.getTaobaoUserInfo());
            }catch (Exception e){
                log.error("迁移过程中的异常"+couponItemGood.getId()+":"+e.getMessage());
                continue;
            }
        }
        log.info(key + "到" + map.get(key) + "迁移完成");
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
		id: 'eip3IOA0Vo5gClkz',
    });
    gitalk.render('gitalk-container');
</script> 

<!-- Gitalk end -->

