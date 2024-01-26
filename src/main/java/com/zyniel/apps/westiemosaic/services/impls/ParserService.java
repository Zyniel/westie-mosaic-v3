package com.zyniel.apps.westiemosaic.services.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyniel.apps.westiemosaic.models.helpers.CoreHelper;
import com.zyniel.apps.westiemosaic.repositories.EventRepository;
import com.zyniel.apps.westiemosaic.models.EventProcessor;
import com.zyniel.apps.westiemosaic.models.WestieCombinedExtractor;
import com.zyniel.apps.westiemosaic.models.WestieParser;
import com.zyniel.apps.westiemosaic.models.helpers.ConfigurationHelper;
import com.zyniel.apps.westiemosaic.services.IParserService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Component
public class ParserService implements IParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserService.class.getName());

    private final EventProcessor processor;
    private final WestieParser parser;

    @Autowired
    EventRepository eventRepository;

    public ParserService() {
        // 0) =============== Prerequisites ===============
        // Load Configuration
        String url = ConfigurationHelper.getWestieAppUrl();

        // Prepare parser
        // IDataProcessor processor = new WestieDataExtractor();
        this.parser = new WestieParser(url);
        this.processor = new WestieCombinedExtractor(this.parser);
        parser.addProcessor(processor);
    }

    @Override
    public void parseWebsite() throws Exception {
        parser.parse();
    }

    @Override
    public void saveData() throws Exception {
        ((WestieCombinedExtractor) processor).getEventInfos().entrySet().stream().forEach(e -> eventRepository.save(e.getValue()));
    }

    @Override
    public void exportImages() throws Exception {
        // Remove previous images
        LOGGER.info("Removing previous banners...");
        String extension = ".png";
        String dir = ConfigurationHelper.getLocalImageRepo();
        Path path = Path.of(dir);
        CoreHelper.removeFilesByExtension(path, extension);

        // Add new images from base64 hashmap
        LOGGER.info("Adding new banners...");
        ((WestieCombinedExtractor) processor).getEventB64Images().entrySet().stream().forEach(event -> {
                Path pngFile = Path.of(dir, event.getKey() + extension);
                // decode the string and write to file
                byte[] decodedBytes = Base64
                        .getDecoder()
                        .decode(event.getValue());
                try {
                    FileUtils.writeByteArrayToFile(pngFile.toFile(), decodedBytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    @Override
    public void exportData() throws Exception {
        // Save Data as JSON
        ObjectMapper objectMapper = new ObjectMapper();
        Path jsonFile = Paths.get(ConfigurationHelper.getLocalImageRepo(), "events.json");
        objectMapper.writeValue(jsonFile.toFile(), ((WestieCombinedExtractor) processor).getEventInfos().values());
    }
}
