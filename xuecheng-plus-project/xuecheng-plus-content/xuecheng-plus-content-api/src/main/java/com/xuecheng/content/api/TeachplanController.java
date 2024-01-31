package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TeachplanController {
    @Autowired
    TeachplanService teachplanService;

    //查询课程计划
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
        List<TeachplanDto> teachPlanTree = teachplanService.findTeachplanTree(courseId);
        return teachPlanTree;
    }

    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto saveTeachplanDto){
        teachplanService.saveTeachplan(saveTeachplanDto);
    }

    @ApiOperation("删除课程计划")
    @DeleteMapping("/teachplan/{teachplanId}")
    public void deleteTeachplan(@PathVariable Long teachplanId){
        teachplanService.deleteTeachplan(teachplanId);
    }

    @ApiOperation("向下/上移动")
    @PostMapping("/teachplan/{moveType}/{teachplanId}")
    public void moveTeachplan(@PathVariable("moveType") String moveType,@PathVariable("teachplanId") Long teachplanId){
        teachplanService.moveTeachplan(moveType,teachplanId);
    }

    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto){
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }

}
