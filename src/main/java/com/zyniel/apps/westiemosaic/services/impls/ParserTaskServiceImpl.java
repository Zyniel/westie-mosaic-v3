package com.zyniel.apps.westiemosaic.services.impls;

import com.zyniel.apps.westiemosaic.services.IParserService;
import com.zyniel.apps.westiemosaic.services.IParserTaskService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("${scheduler.parser.startup.enabled:true} or ${scheduler.parser.periodic.enabled:true}")
public class ParserTaskServiceImpl implements IParserTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserTaskServiceImpl.class);

    @Autowired
    IParserService parserService;

    ParserTaskServiceImpl() {}

    @Override
    @PostConstruct
    @ConditionalOnExpression("${scheduler.parser.startup.enabled:true}")
    public void initialRefresh() throws Exception {
        LOGGER.info("========================== STARTING INITIAL DATA EXTRACTION ==========================");
        processWestieAppWebsite();
        LOGGER.info("========================= FINISHED INITIAL DATA EXTRACTION ! ==========================");
    }

    @Override
    public void periodicRefresh() throws Exception {
        LOGGER.info("========================== STARTING PERIODIC DATA EXTRACTION =========================");
        processWestieAppWebsite();
        LOGGER.info("========================= FINISHED PERIODIC DATA EXTRACTION ! =========================");
    }

    /**
     * Proceeds to service calls to extract and exploit data from Westie.app for this program.
     * @throws Exception Any exception that may occur during the process
     */
    private void processWestieAppWebsite() throws Exception {
        LOGGER.info("STEP 01: Extracting Westie.app data");
        parserService.parseWebsite();
        LOGGER.info("STEP 02: Saving files to disk ");
        parserService.exportImages();
        LOGGER.info("STEP 03: Persisting events ");
        parserService.saveData();
        LOGGER.info("STEP 04: Saving data as JSON ");
        parserService.exportData();
    }
}
