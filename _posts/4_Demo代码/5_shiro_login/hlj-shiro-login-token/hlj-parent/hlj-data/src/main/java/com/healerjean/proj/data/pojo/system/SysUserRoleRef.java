/*
 * Copyright (C) 2019 xiaomi.com, Inc. All Rights Reserved.
 */
package com.healerjean.proj.data.pojo.system;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author zhangyujin
 * @ClassName: SysUserRoleRef
 * @date 2099/1/1
 * @Description: SysUserRoleRef
 */
@Data
@Entity
@Table(name = "sys_user_role_ref")
public class SysUserRoleRef implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 用户ID
     */
    private Long refUserId;
    /**
     * 角色ID
     */
    private Long refRoleId;
    /**
     * 状态
     */
    private String status;
    /**
     * 创建人
     */
    private Long createUser;
    /**
     * 创建人名称
     */
    private String createName;
    /**
     * 创建时间
     */
    private java.util.Date createTime;
    /**
     * 更新人
     */
    private Long updateUser;
    /**
     * 更新人名称
     */
    private String updateName;
    /**
     * 更新时间
     */
    private java.util.Date updateTime;

}
