package com.xuecheng.system.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;


@Configuration
public class GlobalCorsConfig {

    @Bean
//    public CorsFilter corsFilter() {
//
//        CorsConfiguration config = new CorsConfiguration();
//        //允许白名单域名进行跨域调用
//        config.addAllowedOrigin( "*");
//        //允许跨越发送cookie
//        config.setAllowCredentials(true);
//        //放行全部原始头信息
//        config.addAllowedHeader("*");
//        //允许所有请求方法跨域调用
//        config.addAllowedMethod("*");
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return new CorsFilter(source);
//
//    }
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        // 1. 添加cors配置信息
        CorsConfiguration config = new CorsConfiguration();
        // Response Headers里面的Access-Control-Allow-Origin: http://localhost:8080
        // 其实不建议使用*，允许所有跨域
        config.addAllowedOrigin("*");

        // Response Headers里面的Access-Control-Allow-Credentials: true
        config.setAllowCredentials(true);

        // 设置允许请求的方式，比如get、post、put、delete，*表示全部
        // Response Headers里面的Access-Control-Allow-Methods属性
        config.addAllowedMethod("*");

        // 设置允许的header
        // Response Headers里面的Access-Control-Allow-Headers属性，这里是Access-Control-Allow-Headers: content-type, headeruserid, headerusertoken
        config.addAllowedHeader("*");

        // Response Headers里面的Access-Control-Max-Age:3600
        // 表示下回同一个接口post请求，在3600s之内不会发送options请求，不管post请求成功还是失败，3600s之内不会再发送options请求
        // 如果不设置这个，那么每次post请求之前必定有options请求
        config.setMaxAge(3600L);

        // 2. 为url添加映射路径
        UrlBasedCorsConfigurationSource corsSource = new UrlBasedCorsConfigurationSource();
        // /**表示该config适用于所有路由
        corsSource.registerCorsConfiguration("/**", config);

        // 当存在多个Filter时，需要通过如下方式返回一个新的FilterRegistrationBean出去，并设置order。否则会导致跨域配置失效
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(corsSource));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        // 3. 返回重新定义好的corsSource
        return bean;
    }
}
