package com.dongxm.controller;

import com.dongxm.annotation.ProxyGetMapping;
import com.dongxm.annotation.ProxyRestController;
import com.dongxm.dto.UserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Map;

@ProxyRestController
public class HelloController {
    @GetMapping(value = "get")
    public String get() {
        return "get";
    }

    @ProxyGetMapping("proxy_get")
    public String proxyGet(String name) {
        return "proxy_get_"+name;
    }

    @ProxyGetMapping("proxy_get2")
    public UserDTO proxyGet2(String name) {
        UserDTO userDTO = new UserDTO();
        userDTO.setName(name);
        return userDTO;
    }
}
