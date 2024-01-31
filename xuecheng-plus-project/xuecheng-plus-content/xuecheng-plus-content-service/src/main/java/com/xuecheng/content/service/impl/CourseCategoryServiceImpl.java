package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id){
        //调用mapper查询
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //找到每个节点的子结点，最终将其封装成List<CourseCategoryTreeDto>
        List<CourseCategoryTreeDto> courseCategoryTreeList = new ArrayList<>();
        //先将list转为map，key为节点的id，value为CourseCategoryTreeDto对象，目的是为了方便从map中获取节点
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        //从头遍历List<CourseCategoryTreeDto>，一边遍历一边找子结点放在父节点的childrenTreeNodes
        courseCategoryTreeDtos.stream().filter(item->!id.equals(item.getId())).forEach(item->{
            //添加2级节点
            if(item.getParentid().equals(id)){
                courseCategoryTreeList.add(item);
            }
            //获取当前节点的父节点，并判断是否在是2级节点
            CourseCategoryTreeDto courseCategoryParentDto = mapTemp.get(item.getParentid());
            if(courseCategoryParentDto!=null){
                if(courseCategoryParentDto.getChildrenTreeNodes()==null){
                    courseCategoryParentDto.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                courseCategoryParentDto.getChildrenTreeNodes().add(item);
            }
        });
        return courseCategoryTreeList;
    }
}

