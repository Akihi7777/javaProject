package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class EditCourse extends AddCourseDto{
    @ApiModelProperty(value = "课程id",required = true)
    private Long id;
}
