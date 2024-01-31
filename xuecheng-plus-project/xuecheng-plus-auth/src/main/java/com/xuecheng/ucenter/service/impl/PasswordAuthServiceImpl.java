package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("password_authService")
public class PasswordAuthServiceImpl implements AuthService {
    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        String username = authParamsDto.getUsername();
        //远程调用校验码接口，校验验证码
        String checkcode = authParamsDto.getCheckcode();
        String checkcodekey = authParamsDto.getCheckcodekey();
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if(StringUtils.isEmpty(checkcodekey)||StringUtils.isEmpty(checkcode)){
            throw new RuntimeException("请输入验证码");
        }

        if(verify==null||!verify){
            throw new RuntimeException("验证码输入有误");
        }

        //校验账号是否存在
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        //查询到用户不存在，返回null，spring security框架抛出异常用户不存在
        if(xcUser==null){
            throw new RuntimeException("账号不存在！");
        }
        //如果查到了用户，可以拿到正确的密码，包装成userDetails对象给spring security框架返回，由框架进行密码比对
        String passwordDB = xcUser.getPassword();
        //验证密码
        String passwordForm = authParamsDto.getPassword();
        boolean matches = passwordEncoder.matches(passwordForm, passwordDB);
        if(!matches){
            throw new RuntimeException("账号或密码错误");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        return xcUserExt;
    }
}
