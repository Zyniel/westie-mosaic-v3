package com.zyniel.apps.westiemosaic.services;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("${scheduler.parser.startup.enabled:true} or ${scheduler.parser.periodic.enabled:true}")
public interface IParserTaskService {

    @PostConstruct
    @ConditionalOnExpression("${scheduler.parser.startup.enabled:true}")
    void initialRefresh() throws Exception;

    @Scheduled(cron = "${scheduler.parser.periodic.cron}")
    @ConditionalOnExpression("${scheduler.parser.periodic.enabled:true}")
    void periodicRefresh() throws Exception;

}
