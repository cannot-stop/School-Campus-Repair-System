package com.campus.repair.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果封装。设计书 1.1.2.2 要求"列表分页展示"。
 *
 * @param <T> 行数据类型
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> rows = new ArrayList<T>();
    /** 总记录数 */
    private long total;
    /** 当前页码，从 1 开始 */
    private int pageNum = 1;
    /** 每页条数 */
    private int pageSize = 10;

    public PageResult() {
    }

    public PageResult(List<T> rows, long total, int pageNum, int pageSize) {
        this.rows = rows == null ? new ArrayList<T>() : rows;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /** 总页数 */
    public int getPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
