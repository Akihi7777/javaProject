package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {
    public List<TeachplanDto> findTeachplanTree(Long courseId);
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);
    public void deleteTeachplan(Long teachpanId);
    public void moveTeachplan(String moveType, Long teachplanId);

    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
