package com.part2.monew.config;

import com.part2.monew.entity.DataSourceType;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DataSourceRoutingAspect {

    @Before("@annotation(com.part2.monew.annotation.ReadOnly)")
    public void setReadOnly() {
        DataSourceContextHolder.set(DataSourceType.STANDBY);
    }

    @Before("@annotation(com.part2.monew.annotation.Master)")
    public void setMaster() {
        DataSourceContextHolder.set(DataSourceType.MAIN);
    }

    @After("@annotation(com.part2.monew.annotation.ReadOnly) || @annotation(com.part2.monew.annotation.Master)")
    public void clearDataSourceType() {
        DataSourceContextHolder.clear();
    }
}