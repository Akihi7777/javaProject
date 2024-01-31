package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        //将转入的json转成对象
        AuthParamsDto authParamsDto=null;
        try {
            authParamsDto=JSON.parseObject(s,AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证参数不符合要求");
        }
        String authType = authParamsDto.getAuthType();
        String beanName=authType+"_authService";
        //根据认证类型从spring容器中取出对应的bean
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);
        //统一调用execute方法实现认证
        XcUserExt xcUserExt = authService.execute(authParamsDto);
        //封装XcUserExt用户信息为UerDetails
        UserDetails userDetails = getUserPrincipal(xcUserExt);
        return userDetails;
    }

    public UserDetails getUserPrincipal(XcUserExt user){
        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        String[] authorities = {"p1"};
        String password = user.getPassword();
        //为了安全在令牌中不放密码
        user.setPassword(null);
        //将user对象转json
        String userString = JSON.toJSONString(user);
        //如果查到了用户，可以拿到正确的密码，包装成userDetails对象给spring security框架返回，由框架进行密码比对
        UserDetails userDetails = User.withUsername(userString).password(password).authorities(authorities).build();
        return userDetails;
    }

}
