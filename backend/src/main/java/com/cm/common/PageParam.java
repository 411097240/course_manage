package com.cm.common;

import lombok.Data;

@Data
public class PageParam {
    private long current = 1;
    private long size = 10;
}
