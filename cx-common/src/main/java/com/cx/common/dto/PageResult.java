package com.cx.common.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<T> records;

    // 兼容 MyBatis-Plus Page 的转换
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(), page.getRecords());
    }
}
