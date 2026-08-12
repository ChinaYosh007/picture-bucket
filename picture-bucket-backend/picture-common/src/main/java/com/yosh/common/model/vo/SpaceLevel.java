package com.yosh.common.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class SpaceLevel implements Serializable {

    private int value;

    private String text;

    private long maxCount;

    private long maxSize;

    private static final long serialVersionUID = 1L;
}
