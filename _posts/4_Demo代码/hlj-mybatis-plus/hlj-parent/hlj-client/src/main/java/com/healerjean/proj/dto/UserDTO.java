package com.healerjean.proj.dto;

import lombok.Data;

import java.util.List;

/**
 * @author HealerJean
 * @ClassName UserDTO
 * @date 2020/3/5  16:10.
 * @Description
 */
@Data
public class UserDTO {

    private Long id;
    private Long userId;
    private List<Long> ids ;
    private String name;
    private Integer age;
    private String email;
}
